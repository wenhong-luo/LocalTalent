package cn.localtalent.backend.admin.ops.application;

import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.FeatureResponse;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.OpsOverviewResponse;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.RecommendationPageResponse;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.RecommendationRequest;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.RecommendationResponse;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.RiskHandleRequest;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.RiskReviewPageResponse;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.RiskReviewResponse;
import cn.localtalent.backend.admin.ops.infrastructure.AdminPortalOpsJdbcRepository;
import cn.localtalent.backend.audit.AuditService;
import cn.localtalent.backend.auth.domain.IdentityType;
import cn.localtalent.backend.authz.AuthzContext;
import cn.localtalent.backend.authz.AuthzPrincipal;
import cn.localtalent.backend.candidate.application.IdempotencyService;
import cn.localtalent.backend.candidate.domain.IdempotentActionResult;
import cn.localtalent.backend.common.exception.ApiException;
import cn.localtalent.backend.common.json.AuditJsonMapper;
import cn.localtalent.backend.phase3.Phase3FeatureProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPortalOpsService {

    private static final String RECOMMENDATION_CREATE_API = "admin.recommendation.create";
    private static final String RECOMMENDATION_UPDATE_API = "admin.recommendation.update";
    private static final String RECOMMENDATION_OFFLINE_API = "admin.recommendation.offline";
    private static final String RISK_HANDLE_API = "admin.risk-review.handle";
    private static final List<String> TARGET_TYPES = List.of("job", "company", "content", "event", "talent_snapshot");
    private static final int MAX_PAGE_SIZE = 100;

    private final AdminPortalOpsJdbcRepository repository;
    private final Phase3FeatureProperties phase3Features;
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper = AuditJsonMapper.create();

    public AdminPortalOpsService(
            AdminPortalOpsJdbcRepository repository,
            Phase3FeatureProperties phase3Features,
            IdempotencyService idempotencyService,
            AuditService auditService
    ) {
        this.repository = repository;
        this.phase3Features = phase3Features;
        this.idempotencyService = idempotencyService;
        this.auditService = auditService;
    }

    public OpsOverviewResponse overview() {
        requireAdminReader();
        requireEnabled();
        return new OpsOverviewResponse(
                new FeatureResponse(true),
                repository.pendingCompanyCount(),
                repository.pendingJobCount(),
                repository.pendingExportCount(),
                repository.publishedContentCount(),
                repository.publishedEventCount(),
                repository.activeRecommendationCount(),
                repository.pendingRiskCount(),
                repository.recentAuditCount());
    }

    public RecommendationPageResponse recommendations(String slotCode, String targetType, Integer status, int page, int size) {
        requireAdminReader();
        requireEnabled();
        String normalizedSlot = normalizeNullable(slotCode, 64);
        String normalizedTarget = normalizeTargetFilter(targetType);
        Integer normalizedStatus = normalizeStatusFilter(status);
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = (normalizedPage - 1) * normalizedSize;
        return new RecommendationPageResponse(
                repository.listRecommendations(normalizedSlot, normalizedTarget, normalizedStatus, normalizedSize, offset),
                repository.countRecommendations(normalizedSlot, normalizedTarget, normalizedStatus),
                normalizedPage,
                normalizedSize);
    }

    @Transactional
    public RecommendationResponse createRecommendation(RecommendationRequest request, String idempotencyKey) {
        AuthzPrincipal principal = requireOperator();
        requireEnabled();
        NormalizedRecommendation normalized = normalizeRecommendation(request);
        return idempotencyService.execute(
                RECOMMENDATION_CREATE_API,
                principal,
                idempotencyKey,
                normalized.fingerprint(),
                RecommendationResponse.class,
                () -> createRecommendationOnce(principal, normalized));
    }

    @Transactional
    public RecommendationResponse updateRecommendation(long id, RecommendationRequest request, String idempotencyKey) {
        AuthzPrincipal principal = requireOperator();
        requireEnabled();
        NormalizedRecommendation normalized = normalizeRecommendation(request);
        long recommendationId = requirePositive(id, "recommendation_id");
        Map<String, Object> fingerprint = new LinkedHashMap<>(normalized.fingerprint());
        fingerprint.put("recommendation_id", recommendationId);
        return idempotencyService.execute(
                RECOMMENDATION_UPDATE_API,
                principal,
                idempotencyKey,
                fingerprint,
                RecommendationResponse.class,
                () -> updateRecommendationOnce(principal, recommendationId, normalized));
    }

    @Transactional
    public RecommendationResponse offlineRecommendation(long id, String idempotencyKey) {
        AuthzPrincipal principal = requireOperator();
        requireEnabled();
        long recommendationId = requirePositive(id, "recommendation_id");
        return idempotencyService.execute(
                RECOMMENDATION_OFFLINE_API,
                principal,
                idempotencyKey,
                Map.of("recommendation_id", recommendationId, "action", "offline"),
                RecommendationResponse.class,
                () -> offlineRecommendationOnce(principal, recommendationId));
    }

    public RiskReviewPageResponse riskReviews(String riskType, Integer status, String severity, int page, int size) {
        requireAdminReader();
        requireEnabled();
        String normalizedRiskType = normalizeNullable(riskType, 64);
        Integer normalizedStatus = normalizeRiskStatusFilter(status);
        String normalizedSeverity = normalizeNullable(severity, 32);
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = (normalizedPage - 1) * normalizedSize;
        return new RiskReviewPageResponse(
                repository.listRiskReviews(normalizedRiskType, normalizedStatus, normalizedSeverity, normalizedSize, offset),
                repository.countRiskReviews(normalizedRiskType, normalizedStatus, normalizedSeverity),
                normalizedPage,
                normalizedSize);
    }

    public RiskReviewResponse riskReview(long id) {
        requireAdminReader();
        requireEnabled();
        return requireRisk(id);
    }

    @Transactional
    public RiskReviewResponse handleRiskReview(long id, RiskHandleRequest request, String idempotencyKey) {
        AuthzPrincipal principal = requireOperator();
        requireEnabled();
        long riskId = requirePositive(id, "risk_id");
        NormalizedRiskHandle normalized = normalizeRiskHandle(request);
        return idempotencyService.execute(
                RISK_HANDLE_API,
                principal,
                idempotencyKey,
                Map.of("risk_id", riskId, "status", normalized.status(), "decision", normalized.decision()),
                RiskReviewResponse.class,
                () -> handleRiskReviewOnce(principal, riskId, normalized));
    }

    private IdempotentActionResult<RecommendationResponse> createRecommendationOnce(
            AuthzPrincipal principal,
            NormalizedRecommendation normalized
    ) {
        validateTarget(normalized.targetType(), normalized.targetId());
        long recommendationId = repository.createRecommendation(
                normalized.slotCode(),
                normalized.targetType(),
                normalized.targetId(),
                normalized.titleOverride(),
                normalized.summaryOverride(),
                normalized.displayOrder(),
                normalized.status(),
                normalized.startTime(),
                normalized.endTime(),
                principal.userId());
        RecommendationResponse after = requireRecommendation(recommendationId);
        auditService.record(principal, "portal_recommendation", recommendationId, "recommendation_create", null, safeRecommendationAudit(after));
        return new IdempotentActionResult<>(after, "portal_recommendation", recommendationId);
    }

    private IdempotentActionResult<RecommendationResponse> updateRecommendationOnce(
            AuthzPrincipal principal,
            long recommendationId,
            NormalizedRecommendation normalized
    ) {
        RecommendationResponse before = requireRecommendation(recommendationId);
        validateTarget(normalized.targetType(), normalized.targetId());
        repository.updateRecommendation(
                recommendationId,
                normalized.slotCode(),
                normalized.targetType(),
                normalized.targetId(),
                normalized.titleOverride(),
                normalized.summaryOverride(),
                normalized.displayOrder(),
                normalized.status(),
                normalized.startTime(),
                normalized.endTime(),
                principal.userId());
        RecommendationResponse after = requireRecommendation(recommendationId);
        auditService.record(principal, "portal_recommendation", recommendationId, "recommendation_update",
                safeRecommendationAudit(before), safeRecommendationAudit(after));
        return new IdempotentActionResult<>(after, "portal_recommendation", recommendationId);
    }

    private IdempotentActionResult<RecommendationResponse> offlineRecommendationOnce(AuthzPrincipal principal, long recommendationId) {
        RecommendationResponse before = requireRecommendation(recommendationId);
        repository.offlineRecommendation(recommendationId, principal.userId());
        RecommendationResponse after = requireRecommendation(recommendationId);
        auditService.record(principal, "portal_recommendation", recommendationId, "recommendation_offline",
                safeRecommendationAudit(before), safeRecommendationAudit(after));
        return new IdempotentActionResult<>(after, "portal_recommendation", recommendationId);
    }

    private IdempotentActionResult<RiskReviewResponse> handleRiskReviewOnce(
            AuthzPrincipal principal,
            long riskId,
            NormalizedRiskHandle normalized
    ) {
        RiskReviewResponse before = requireRisk(riskId);
        repository.handleRiskReview(riskId, normalized.status(), normalized.decision(), principal.userId());
        RiskReviewResponse after = requireRisk(riskId);
        auditService.record(principal, "risk_review", riskId, "risk_review_handle",
                safeRiskAudit(before), safeRiskAudit(after));
        return new IdempotentActionResult<>(after, "risk_review", riskId);
    }

    private void validateTarget(String targetType, long targetId) {
        if (repository.resolveTargetCard(new RecommendationResponse(
                0,
                "validation",
                targetType,
                targetId,
                null,
                null,
                0,
                1,
                null,
                null,
                true,
                null,
                null)).isEmpty()) {
            throw validation("recommendation target is not public or not eligible");
        }
    }

    private RecommendationResponse requireRecommendation(long recommendationId) {
        return repository.findRecommendation(recommendationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "recommendation not found"));
    }

    private RiskReviewResponse requireRisk(long riskId) {
        return repository.findRiskReview(requirePositive(riskId, "risk_id"))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "risk review not found"));
    }

    private AuthzPrincipal requireAdminReader() {
        AuthzPrincipal principal = AuthzContext.requireCurrentPrincipal();
        if (principal.identityType() != IdentityType.OPERATOR || (!principal.hasRole("operator") && !principal.hasRole("auditor"))) {
            throw forbidden("admin reader role required");
        }
        return principal;
    }

    private AuthzPrincipal requireOperator() {
        AuthzPrincipal principal = AuthzContext.requireCurrentPrincipal();
        if (principal.identityType() != IdentityType.OPERATOR || !principal.hasRole("operator")) {
            throw forbidden("operator role required");
        }
        return principal;
    }

    private void requireEnabled() {
        if (!phase3Features.isOperatorPortalOps()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FEATURE_DISABLED_403", "operator portal ops disabled");
        }
    }

    private NormalizedRecommendation normalizeRecommendation(RecommendationRequest request) {
        if (request == null) {
            throw validation("request body is required");
        }
        String targetType = required(request.targetType(), "target_type", 32);
        if (!TARGET_TYPES.contains(targetType)) {
            throw validation("target_type is not supported");
        }
        int status = request.status() == null ? 1 : request.status();
        if (status != 0 && status != 1) {
            throw validation("status must be 0 or 1");
        }
        LocalDateTime start = request.startTime();
        LocalDateTime end = request.endTime();
        if (start != null && end != null && end.isBefore(start)) {
            throw validation("end_time must be after start_time");
        }
        return new NormalizedRecommendation(
                required(request.slotCode(), "slot_code", 64),
                targetType,
                requirePositive(request.targetId() == null ? 0 : request.targetId(), "target_id"),
                normalizeNullable(request.titleOverride(), 200),
                normalizeNullable(request.summaryOverride(), 500),
                request.displayOrder() == null ? 0 : request.displayOrder(),
                status,
                start,
                end);
    }

    private NormalizedRiskHandle normalizeRiskHandle(RiskHandleRequest request) {
        if (request == null) {
            throw validation("request body is required");
        }
        if (request.status() < 1 || request.status() > 4) {
            throw validation("status must be between 1 and 4");
        }
        return new NormalizedRiskHandle(request.status(), required(request.decision(), "decision", 1000));
    }

    private String normalizeTargetFilter(String targetType) {
        String normalized = normalizeNullable(targetType, 32);
        if (normalized != null && !TARGET_TYPES.contains(normalized)) {
            throw validation("target_type is not supported");
        }
        return normalized;
    }

    private Integer normalizeStatusFilter(Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw validation("status must be 0 or 1");
        }
        return status;
    }

    private Integer normalizeRiskStatusFilter(Integer status) {
        if (status != null && (status < 0 || status > 4)) {
            throw validation("status must be between 0 and 4");
        }
        return status;
    }

    private long requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw validation(fieldName + " must be positive");
        }
        return value;
    }

    private String required(String value, String fieldName, int maxLength) {
        String normalized = normalizeNullable(value, maxLength);
        if (normalized == null) {
            throw validation(fieldName + " is required");
        }
        return normalized;
    }

    private String normalizeNullable(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private String safeRecommendationAudit(RecommendationResponse response) {
        if (response == null) {
            return null;
        }
        return json(Map.of(
                "recommendation_id", response.recommendationId(),
                "slot_code", response.slotCode(),
                "target_type", response.targetType(),
                "target_id", response.targetId(),
                "status", response.status()));
    }

    private String safeRiskAudit(RiskReviewResponse response) {
        if (response == null) {
            return null;
        }
        return json(Map.of(
                "risk_id", response.riskId(),
                "risk_type", response.riskType(),
                "target_type", response.targetType(),
                "target_id", response.targetId(),
                "status", response.status()));
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

    private record NormalizedRecommendation(
            String slotCode,
            String targetType,
            long targetId,
            String titleOverride,
            String summaryOverride,
            int displayOrder,
            int status,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        Map<String, Object> fingerprint() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("slot_code", slotCode);
            map.put("target_type", targetType);
            map.put("target_id", targetId);
            map.put("title_override", titleOverride == null ? "" : titleOverride);
            map.put("summary_override", summaryOverride == null ? "" : summaryOverride);
            map.put("display_order", displayOrder);
            map.put("status", status);
            map.put("start_time", startTime == null ? "" : startTime);
            map.put("end_time", endTime == null ? "" : endTime);
            return map;
        }
    }

    private record NormalizedRiskHandle(int status, String decision) {
    }
}
