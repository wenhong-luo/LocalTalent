package cn.localtalent.backend.company.workbench.application;

import cn.localtalent.backend.audit.AuditService;
import cn.localtalent.backend.audit.FieldAccessRecorder;
import cn.localtalent.backend.auth.domain.IdentityType;
import cn.localtalent.backend.authz.AuthzContext;
import cn.localtalent.backend.authz.AuthzPrincipal;
import cn.localtalent.backend.candidate.application.IdempotencyService;
import cn.localtalent.backend.candidate.domain.IdempotentActionResult;
import cn.localtalent.backend.common.exception.ApiException;
import cn.localtalent.backend.company.application.CompanyService;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.ResumeSearchDetailResponse;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.ResumeSearchItemResponse;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.ResumeSearchPageResponse;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.ResumeSnapshotReportRequest;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.ResumeSnapshotReportResponse;
import cn.localtalent.backend.company.workbench.infrastructure.CompanyResumeSearchJdbcRepository;
import cn.localtalent.backend.company.workbench.infrastructure.CompanyResumeSearchJdbcRepository.ResumeSearchQuery;
import cn.localtalent.backend.company.workbench.infrastructure.CompanyResumeSearchJdbcRepository.ResumeSearchRow;
import cn.localtalent.backend.company.workbench.infrastructure.CompanyWorkbenchJdbcRepository;
import cn.localtalent.backend.phase3.Phase3FeatureProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyResumeSearchService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final String REPORT_API = "company.resume-search.report";
    private static final String CONTACT_ACCESS_HINT = "联系方式查看需通过合规申请；当前不开放联系解锁。";
    private static final Set<String> REPORT_REASON_CODES = Set.of(
            "false_information",
            "inappropriate_content",
            "duplicate_snapshot",
            "wrong_category",
            "privacy_concern",
            "other");

    private final Phase3FeatureProperties phase3Features;
    private final CompanyWorkbenchJdbcRepository companyRepository;
    private final CompanyResumeSearchJdbcRepository searchRepository;
    private final FieldAccessRecorder fieldAccessRecorder;
    private final AuditService auditService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CompanyResumeSearchService(
            Phase3FeatureProperties phase3Features,
            CompanyWorkbenchJdbcRepository companyRepository,
            CompanyResumeSearchJdbcRepository searchRepository,
            FieldAccessRecorder fieldAccessRecorder,
            AuditService auditService,
            IdempotencyService idempotencyService
    ) {
        this.phase3Features = phase3Features;
        this.companyRepository = companyRepository;
        this.searchRepository = searchRepository;
        this.fieldAccessRecorder = fieldAccessRecorder;
        this.auditService = auditService;
        this.idempotencyService = idempotencyService;
    }

    public ResumeSearchPageResponse search(ResumeSearchRequest request) {
        AuthzPrincipal principal = requireCompany();
        requireEnabled();
        requireApprovedCompany(principal);
        ResumeSearchQuery query = normalize(request);
        var rows = searchRepository.search(query);
        List<ResumeSearchItemResponse> items = rows.rows().stream()
                .map(row -> toResponse(principal, row))
                .toList();
        return new ResumeSearchPageResponse(items, rows.total(), query.page(), query.size());
    }

    public ResumeSearchDetailResponse detail(long snapshotId) {
        AuthzPrincipal principal = requireCompany();
        requireEnabled();
        requireApprovedCompany(principal);
        ResumeSearchRow row = requireVisibleSnapshot(snapshotId);
        fieldAccessRecorder.record(
                principal,
                "candidate_publish_snapshot",
                row.snapshotId(),
                "snapshot_detail",
                "COMPANY_RESUME_SEARCH_DETAIL");
        return toDetailResponse(row);
    }

    @Transactional
    public ResumeSnapshotReportResponse report(
            long snapshotId,
            ResumeSnapshotReportRequest request,
            String idempotencyKey
    ) {
        AuthzPrincipal principal = requireCompany();
        requireEnabled();
        requireApprovedCompany(principal);
        requireVisibleSnapshot(snapshotId);
        NormalizedReport normalized = normalizeReport(request);
        Map<String, Object> fingerprint = Map.of(
                "snapshot_id", snapshotId,
                "reason_code", normalized.reasonCode(),
                "remark_summary", normalized.remarkSummary() == null ? "" : normalized.remarkSummary());
        return idempotencyService.execute(
                REPORT_API,
                principal,
                idempotencyKey,
                fingerprint,
                ResumeSnapshotReportResponse.class,
                () -> reportOnce(principal, snapshotId, normalized));
    }

    private IdempotentActionResult<ResumeSnapshotReportResponse> reportOnce(
            AuthzPrincipal principal,
            long snapshotId,
            NormalizedReport normalized
    ) {
        long reportId = searchRepository.createReport(
                principal.companyId(),
                principal.userId(),
                snapshotId,
                normalized.reasonCode(),
                normalized.remarkSummary());
        searchRepository.createRiskReviewForReport(reportId, snapshotId, normalized.reasonCode(), normalized.remarkSummary());
        auditService.record(
                principal,
                "company_resume_snapshot_report",
                reportId,
                "resume_snapshot_report",
                null,
                "{\"snapshot_id\":" + snapshotId + ",\"reason_code\":\"" + normalized.reasonCode()
                        + "\",\"status\":\"submitted\"}");
        return new IdempotentActionResult<>(
                new ResumeSnapshotReportResponse(
                        reportId,
                        snapshotId,
                        "submitted",
                        "举报已提交，运营将按风险审核流程处理。"),
                "company_resume_snapshot_report",
                reportId);
    }

    private AuthzPrincipal requireCompany() {
        AuthzPrincipal principal = AuthzContext.requireCurrentPrincipal();
        if (principal.identityType() != IdentityType.COMPANY || principal.companyId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTHZ_403", "company identity required");
        }
        return principal;
    }

    private void requireEnabled() {
        if (!phase3Features.isCompanyWorkbench() || !phase3Features.isCompanyResumeSearch()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FEATURE_DISABLED_403", "company resume search disabled");
        }
    }

    private void requireApprovedCompany(AuthzPrincipal principal) {
        if (companyRepository.authStatus(principal.companyId()) != CompanyService.AUTH_APPROVED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTHZ_403", "企业认证通过后才能搜索发布快照");
        }
    }

    private ResumeSearchQuery normalize(ResumeSearchRequest request) {
        int page = request.page() == null ? DEFAULT_PAGE : Math.max(request.page(), DEFAULT_PAGE);
        int size = request.size() == null ? DEFAULT_SIZE : Math.min(Math.max(request.size(), 1), MAX_SIZE);
        Integer experienceMin = normalizeInteger(request.experienceMin(), "experience_min", 0, 80);
        Integer experienceMax = normalizeInteger(request.experienceMax(), "experience_max", 0, 80);
        if (experienceMin != null && experienceMax != null && experienceMin > experienceMax) {
            throw validation("experience_min must be less than or equal to experience_max");
        }
        return new ResumeSearchQuery(
                normalizeText(request.keyword(), 80),
                normalizeCode(request.cityCode(), 64),
                normalizeCode(request.categoryCode(), 64),
                normalizeCode(request.educationCode(), 64),
                experienceMin,
                experienceMax,
                normalizeCode(request.gender(), 32),
                normalizeText(request.resumeTag(), 64),
                normalizeCode(request.industryCode(), 64),
                normalizeText(request.major(), 80),
                normalizeCode(request.workNature(), 64),
                normalizeCode(request.expectedSalaryCode(), 64),
                normalizeInteger(request.updatedWithinDays(), "updated_within", 1, 365),
                page,
                size,
                normalizeSort(request.sort()));
    }

    private ResumeSearchItemResponse toResponse(AuthzPrincipal principal, ResumeSearchRow row) {
        JsonNode node = readSnapshot(row.snapshotJson());
        fieldAccessRecorder.record(principal, "candidate_publish_snapshot", row.snapshotId(), "snapshot_summary", "COMPANY_RESUME_SEARCH");
        return new ResumeSearchItemResponse(
                row.snapshotId(),
                text(node, "display_name_masked"),
                text(node, "age_band"),
                text(node, "gender"),
                text(node, "education_code"),
                text(node, "highest_education"),
                integer(node, "experience_years"),
                textArray(node.path("expected_positions"), 6),
                textArray(node.path("expected_cities"), 6),
                text(node, "expected_salary"),
                text(node, "city_code"),
                text(node, "category_code"),
                text(node, "industry_code"),
                text(node, "major_name"),
                text(node, "work_nature"),
                textArray(node.path("resume_tags"), 8),
                text(node, "skills_summary"),
                row.updatedAtText());
    }

    private ResumeSearchDetailResponse toDetailResponse(ResumeSearchRow row) {
        JsonNode node = readSnapshot(row.snapshotJson());
        return new ResumeSearchDetailResponse(
                row.snapshotId(),
                text(node, "display_name_masked"),
                text(node, "age_band"),
                text(node, "gender"),
                text(node, "education_code"),
                text(node, "highest_education"),
                integer(node, "experience_years"),
                textArray(node.path("expected_positions"), 6),
                textArray(node.path("expected_cities"), 6),
                text(node, "expected_salary"),
                text(node, "city_code"),
                text(node, "category_code"),
                text(node, "industry_code"),
                text(node, "major_name"),
                text(node, "work_nature"),
                textArray(node.path("resume_tags"), 8),
                text(node, "skills_summary"),
                limit(text(node, "education_summary"), 300),
                limit(text(node, "experience_summary"), 300),
                limit(text(node, "self_description_summary"), 300),
                CONTACT_ACCESS_HINT,
                row.updatedAtText());
    }

    private ResumeSearchRow requireVisibleSnapshot(long snapshotId) {
        if (snapshotId <= 0) {
            throw validation("snapshot_id must be positive");
        }
        ResumeSearchRow row = searchRepository.findVisibleSnapshot(snapshotId);
        if (row == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "发布快照不存在或不可见");
        }
        return row;
    }

    private NormalizedReport normalizeReport(ResumeSnapshotReportRequest request) {
        if (request == null) {
            throw validation("report request is required");
        }
        String reasonCode = normalizeCode(request.reasonCode(), 64);
        if (reasonCode == null || !REPORT_REASON_CODES.contains(reasonCode)) {
            throw validation("unsupported report reason");
        }
        return new NormalizedReport(reasonCode, sanitizeRemark(request.remark()));
    }

    private String sanitizeRemark(String value) {
        String text = normalizeText(value, 300);
        if (text == null) {
            return null;
        }
        return text
                .replaceAll("(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}", "[redacted-email]")
                .replaceAll("1[3-9]\\d{9}", "[redacted-mobile]")
                .replaceAll("\\d{7,}", "[redacted-number]");
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private JsonNode readSnapshot(String snapshotJson) {
        if (snapshotJson == null || snapshotJson.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(snapshotJson);
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text.isBlank() ? null : text;
    }

    private Integer integer(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isInt() || value.isLong()) {
            return value.asInt();
        }
        if (value.isTextual()) {
            try {
                return Integer.parseInt(value.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private List<String> textArray(JsonNode node, int limit) {
        List<String> values = new ArrayList<>();
        if (node == null || node.isMissingNode() || node.isNull()) {
            return values;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                addText(values, item.asText(), limit);
            }
        } else if (node.isTextual()) {
            for (String part : node.asText().split("[,，/、]")) {
                addText(values, part, limit);
            }
        }
        return values;
    }

    private void addText(List<String> values, String raw, int limit) {
        if (values.size() >= limit || raw == null) {
            return;
        }
        String value = raw.trim();
        if (!value.isBlank() && !values.contains(value)) {
            values.add(value);
        }
    }

    private String normalizeCode(String value, int maxLength) {
        return normalizeText(value, maxLength);
    }

    private String normalizeText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw validation("filter value is too long");
        }
        if (normalized.chars().anyMatch(Character::isISOControl)) {
            throw validation("filter value contains control character");
        }
        return normalized;
    }

    private Integer normalizeInteger(Integer value, String fieldName, int min, int max) {
        if (value == null) {
            return null;
        }
        if (value < min || value > max) {
            throw validation(fieldName + " must be between " + min + " and " + max);
        }
        return value;
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "updated_desc";
        }
        return switch (sort.trim()) {
            case "updated_desc", "experience_desc", "experience_asc" -> sort.trim();
            default -> throw validation("unsupported sort");
        };
    }

    private ApiException validation(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALID_400", message);
    }

    public record ResumeSearchRequest(
            String keyword,
            String cityCode,
            String categoryCode,
            String educationCode,
            Integer experienceMin,
            Integer experienceMax,
            String gender,
            String resumeTag,
            String industryCode,
            String major,
            String workNature,
            String expectedSalaryCode,
            Integer updatedWithinDays,
            Integer page,
            Integer size,
            String sort
    ) {
    }

    private record NormalizedReport(String reasonCode, String remarkSummary) {
    }
}
