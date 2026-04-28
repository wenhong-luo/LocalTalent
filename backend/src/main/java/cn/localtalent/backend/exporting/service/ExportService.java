package cn.localtalent.backend.exporting.service;

import cn.localtalent.backend.audit.AuditService;
import cn.localtalent.backend.auth.domain.IdentityType;
import cn.localtalent.backend.authz.AuthzContext;
import cn.localtalent.backend.authz.AuthzPrincipal;
import cn.localtalent.backend.authz.DataScopeService;
import cn.localtalent.backend.authz.ResourceOwner;
import cn.localtalent.backend.common.exception.ApiException;
import cn.localtalent.backend.common.json.AuditJsonMapper;
import cn.localtalent.backend.common.trace.TraceIdContext;
import cn.localtalent.backend.exporting.api.ExportApplyPageResponse;
import cn.localtalent.backend.exporting.api.ExportApplyRequest;
import cn.localtalent.backend.exporting.api.ExportApplyResponse;
import cn.localtalent.backend.exporting.api.ExportDownloadUrlResponse;
import cn.localtalent.backend.exporting.api.ExportReviewRequest;
import cn.localtalent.backend.exporting.api.ExportScopeRequest;
import cn.localtalent.backend.exporting.domain.ExportApplyRow;
import cn.localtalent.backend.exporting.domain.ExportScope;
import cn.localtalent.backend.exporting.infrastructure.ExportApplyJdbcRepository;
import cn.localtalent.backend.exporting.infrastructure.ExportStorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ExportService {

    public static final String BIZ_APPLICATION_CANDIDATE_DETAIL = "application_candidate_detail";
    public static final int APPROVE_PENDING = 0;
    public static final int APPROVE_APPROVED = 1;
    public static final int APPROVE_REJECTED = 2;
    public static final int GENERATE_NONE = 0;
    public static final int GENERATE_RUNNING = 1;
    public static final int GENERATE_DONE = 2;
    public static final int GENERATE_FAILED = 3;

    private static final int MAX_PAGE_SIZE = 100;

    private final ExportApplyJdbcRepository exportRepository;
    private final ExportCsvGenerator csvGenerator;
    private final ExportStorageService storageService;
    private final DataScopeService dataScopeService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper = AuditJsonMapper.create();
    private final int downloadTtlSeconds;

    public ExportService(
            ExportApplyJdbcRepository exportRepository,
            ExportCsvGenerator csvGenerator,
            ExportStorageService storageService,
            DataScopeService dataScopeService,
            AuditService auditService,
            @Value("${localtalent.export.download-ttl-seconds}") int downloadTtlSeconds
    ) {
        this.exportRepository = exportRepository;
        this.csvGenerator = csvGenerator;
        this.storageService = storageService;
        this.dataScopeService = dataScopeService;
        this.auditService = auditService;
        this.downloadTtlSeconds = Math.min(Math.max(downloadTtlSeconds, 1), 604800);
    }

    @Transactional
    public ExportApplyResponse apply(ExportApplyRequest request) {
        AuthzPrincipal principal = requireCompanyPrincipal();
        long companyId = principal.companyId();
        dataScopeService.assertWritable(principal, "export_apply", ResourceOwner.company(companyId));
        String bizType = required(request == null ? null : request.bizType(), "biz_type");
        if (!BIZ_APPLICATION_CANDIDATE_DETAIL.equals(bizType)) {
            throw validation("unsupported biz_type");
        }
        String reason = required(request.reason(), "reason");
        ExportScope scope = normalizeScope(request.scope());
        assertScopeBelongsToCompany(companyId, scope);
        String scopeJson = json(scopeMap(scope));

        long exportId = exportRepository.create(
                companyId,
                principal.userId(),
                principal.identityType().value(),
                principal.primaryRole(),
                bizType,
                scopeJson,
                reason);
        ExportApplyRow row = requireExport(exportId);
        auditService.record(principal, "export_apply", exportId, "export_apply", null, json(row));
        return response(row);
    }

    public ExportApplyResponse companyDetail(long exportId) {
        AuthzPrincipal principal = requireCompanyPrincipal();
        ExportApplyRow row = requireExport(exportId);
        assertCompanyOwned(principal, row);
        return response(row);
    }

    @Transactional
    public ExportDownloadUrlResponse issueDownloadUrl(long exportId) {
        AuthzPrincipal principal = requireCompanyPrincipal();
        ExportApplyRow row = requireExport(exportId);
        assertCompanyOwned(principal, row);
        if (row.approveStatus() != APPROVE_APPROVED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EXPORT_403", "export is not approved");
        }
        if (row.generateStatus() != GENERATE_DONE || row.fileObjectKey() == null || row.fileObjectKey().isBlank()) {
            throw new ApiException(HttpStatus.CONFLICT, "EXPORT_409", "export file is not ready");
        }
        if (row.downloadCount() > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "EXPORT_409", "download url already issued");
        }

        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(downloadTtlSeconds);
        String downloadUrl = storageService.presignedGetUrl(row.fileObjectKey(), downloadTtlSeconds);
        if (!exportRepository.markDownloadIssued(exportId, downloadUrl, expireTime)) {
            throw new ApiException(HttpStatus.CONFLICT, "EXPORT_409", "download url already issued");
        }
        auditService.record(principal, "export_apply", exportId, "export_download_url_issue", null, json(Map.of(
                "expire_time", expireTime,
                "download_count", row.downloadCount() + 1)));
        return new ExportDownloadUrlResponse(downloadUrl, expireTime);
    }

    public ExportApplyPageResponse reviewList(Integer approveStatus, int page, int size) {
        requireAdminReader();
        Integer normalizedStatus = normalizeReviewListStatus(approveStatus);
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = (normalizedPage - 1) * normalizedSize;
        List<ExportApplyResponse> items = exportRepository
                .listForReview(normalizedStatus, normalizedSize, offset)
                .stream()
                .map(this::response)
                .toList();
        long total = exportRepository.countForReview(normalizedStatus);
        return new ExportApplyPageResponse(items, total);
    }

    public ExportApplyResponse reviewDetail(long exportId) {
        requireAdminReader();
        return response(requireExport(exportId));
    }

    @Transactional
    public ExportApplyResponse review(ExportReviewRequest request) {
        AuthzPrincipal principal = requireOperator();
        long exportId = requirePositive(request == null ? null : request.exportId(), "export_id");
        int approveStatus = requireReviewStatus(request.approveStatus());
        String memo = normalizeNullable(request.memo());
        if (approveStatus == APPROVE_REJECTED && memo == null) {
            throw validation("memo is required when rejecting export");
        }

        ExportApplyRow before = requireExport(exportId);
        if (before.approveStatus() != APPROVE_PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "EXPORT_409", "export has already been reviewed");
        }
        exportRepository.approve(exportId, approveStatus, principal.userId(), approveStatus == APPROVE_REJECTED ? memo : null);
        ExportApplyRow after = requireExport(exportId);
        String action = approveStatus == APPROVE_APPROVED ? "export_approve" : "export_reject";
        auditService.record(principal, "export_apply", exportId, action, json(before), json(reviewAudit(after, memo)));
        if (approveStatus == APPROVE_APPROVED) {
            scheduleGeneration(exportId, TraceIdContext.getCurrentTraceId());
        }
        return response(after);
    }

    public void generate(long exportId, String traceId) {
        TraceIdContext.setCurrentTraceId(traceId);
        try {
            exportRepository.markGenerating(exportId);
            ExportApplyRow row = requireExport(exportId);
            AuthzPrincipal exportPrincipal = new AuthzPrincipal(
                    IdentityType.COMPANY,
                    row.applyUserId(),
                    row.companyId(),
                    List.of(row.applyRoleCode()),
                    "export-" + exportId);
            ExportScope scope = parseScope(row.scopeJson());
            byte[] content = csvGenerator.generate(
                    exportPrincipal,
                    exportRepository.listApplicationCandidates(row.companyId(), scope));
            String objectKey = "exports/" + row.companyId() + "/" + exportId + "-" + UUID.randomUUID() + ".csv";
            storageService.putCsv(objectKey, content);
            exportRepository.markGenerated(exportId, objectKey, sha256(content), content.length);
            auditService.record(exportPrincipal, "export_apply", exportId, "export_generate", null, json(Map.of(
                    "generate_status", GENERATE_DONE,
                    "file_object_key", objectKey,
                    "file_size_bytes", content.length)));
        } catch (Throwable exception) {
            exportRepository.markFailed(exportId, exception.getMessage());
            ExportApplyRow row = requireExport(exportId);
            AuthzPrincipal exportPrincipal = new AuthzPrincipal(
                    IdentityType.COMPANY,
                    row.applyUserId(),
                    row.companyId(),
                    List.of(row.applyRoleCode()),
                    "export-" + exportId);
            auditService.record(exportPrincipal, "export_apply", exportId, "export_generate", null, json(Map.of(
                    "generate_status", GENERATE_FAILED,
                    "error_msg", exception.getMessage() == null ? "generation failed" : exception.getMessage())));
        } finally {
            TraceIdContext.clear();
        }
    }

    private void scheduleGeneration(long exportId, String traceId) {
        Runnable task = () -> generate(exportId, traceId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    CompletableFuture.runAsync(task);
                }
            });
        } else {
            CompletableFuture.runAsync(task);
        }
    }

    private ExportScope normalizeScope(ExportScopeRequest request) {
        if (request == null) {
            return new ExportScope(null, null);
        }
        Long jobId = optionalPositive(request.jobId(), "job_id");
        Integer status = request.status();
        if (status != null && (status < 0 || status > 5)) {
            throw validation("status must be between 0 and 5");
        }
        return new ExportScope(jobId, status);
    }

    private ExportScope parseScope(String scopeJson) {
        try {
            JsonNode node = objectMapper.readTree(scopeJson);
            Long jobId = node.hasNonNull("job_id") ? node.get("job_id").asLong() : null;
            Integer status = node.hasNonNull("status") ? node.get("status").asInt() : null;
            return new ExportScope(jobId, status);
        } catch (Exception exception) {
            return new ExportScope(null, null);
        }
    }

    private void assertScopeBelongsToCompany(long companyId, ExportScope scope) {
        if (scope.jobId() != null && !exportRepository.jobBelongsToCompany(companyId, scope.jobId())) {
            throw forbidden("job does not belong to current company");
        }
    }

    private void assertCompanyOwned(AuthzPrincipal principal, ExportApplyRow row) {
        if (row.companyId() == null) {
            throw forbidden("export company owner is missing");
        }
        dataScopeService.assertAccessible(principal, "export_apply", ResourceOwner.company(row.companyId()));
    }

    private AuthzPrincipal requireCompanyPrincipal() {
        AuthzPrincipal principal = AuthzContext.requireCurrentPrincipal();
        if (principal.identityType() != IdentityType.COMPANY || principal.companyId() == null) {
            throw forbidden("company identity required");
        }
        return principal;
    }

    private AuthzPrincipal requireAdminReader() {
        AuthzPrincipal principal = AuthzContext.requireCurrentPrincipal();
        if (principal.identityType() != IdentityType.OPERATOR
                || (!principal.hasRole("operator") && !principal.hasRole("auditor"))) {
            throw forbidden("admin identity required");
        }
        return principal;
    }

    private AuthzPrincipal requireOperator() {
        AuthzPrincipal principal = AuthzContext.requireCurrentPrincipal();
        if (principal.identityType() != IdentityType.OPERATOR || !principal.hasRole("operator")) {
            throw forbidden("operator identity required");
        }
        return principal;
    }

    private ExportApplyRow requireExport(long exportId) {
        return exportRepository.findById(exportId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "export apply not found"));
    }

    private int requireReviewStatus(Integer approveStatus) {
        if (approveStatus == null) {
            throw validation("approve_status is required");
        }
        if (approveStatus != APPROVE_APPROVED && approveStatus != APPROVE_REJECTED) {
            throw validation("approve_status must be 1 or 2");
        }
        return approveStatus;
    }

    private Integer normalizeReviewListStatus(Integer approveStatus) {
        if (approveStatus == null) {
            return null;
        }
        if (approveStatus < APPROVE_PENDING || approveStatus > APPROVE_REJECTED) {
            throw validation("approve_status must be between 0 and 2");
        }
        return approveStatus;
    }

    private long requirePositive(Long value, String fieldName) {
        Long normalized = optionalPositive(value, fieldName);
        if (normalized == null) {
            throw validation(fieldName + " is required");
        }
        return normalized;
    }

    private Long optionalPositive(Long value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value <= 0) {
            throw validation(fieldName + " must be positive");
        }
        return value;
    }

    private String required(String value, String fieldName) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw validation(fieldName + " is required");
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Map<String, Object> scopeMap(ExportScope scope) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (scope.jobId() != null) {
            map.put("job_id", scope.jobId());
        }
        if (scope.status() != null) {
            map.put("status", scope.status());
        }
        return map;
    }

    private Map<String, Object> reviewAudit(ExportApplyRow row, String memo) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("approve_status", row.approveStatus());
        map.put("reject_reason", row.rejectReason());
        map.put("memo", memo);
        return map;
    }

    private ExportApplyResponse response(ExportApplyRow row) {
        return new ExportApplyResponse(
                row.exportId(),
                row.bizType(),
                row.approveStatus(),
                row.generateStatus(),
                row.reason(),
                row.rejectReason(),
                row.approveTime(),
                row.generatedAt(),
                row.expireTime(),
                row.fileSizeBytes(),
                row.downloadIssuedAt(),
                row.downloadCount(),
                row.errorMsg(),
                row.createdAt());
    }

    private String sha256(byte[] content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private ApiException validation(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALID_400", message);
    }

    private ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "AUTHZ_403", message);
    }
}
