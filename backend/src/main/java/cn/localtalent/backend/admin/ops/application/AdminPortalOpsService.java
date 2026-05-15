package cn.localtalent.backend.admin.ops.application;

import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.FeatureResponse;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.HomeSlotPageResponse;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.HomeSlotImageDownload;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.HomeSlotRequest;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.HomeSlotResponse;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.OpsOverviewResponse;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.RecommendationPageResponse;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.RecommendationRequest;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.RecommendationResponse;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.RiskHandleRequest;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.RiskReviewPageResponse;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.RiskReviewResponse;
import cn.localtalent.backend.admin.ops.infrastructure.AdminPortalOpsJdbcRepository;
import cn.localtalent.backend.audit.AuditService;
import cn.localtalent.backend.audit.FieldAccessRecorder;
import cn.localtalent.backend.auth.domain.IdentityType;
import cn.localtalent.backend.authz.AuthzContext;
import cn.localtalent.backend.authz.AuthzPrincipal;
import cn.localtalent.backend.candidate.application.IdempotencyService;
import cn.localtalent.backend.candidate.domain.IdempotentActionResult;
import cn.localtalent.backend.common.exception.ApiException;
import cn.localtalent.backend.common.json.AuditJsonMapper;
import cn.localtalent.backend.company.workbench.infrastructure.CompanyStyleImageStorageService;
import cn.localtalent.backend.phase3.Phase3FeatureProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminPortalOpsService {

    private static final String RECOMMENDATION_CREATE_API = "admin.recommendation.create";
    private static final String RECOMMENDATION_UPDATE_API = "admin.recommendation.update";
    private static final String RECOMMENDATION_OFFLINE_API = "admin.recommendation.offline";
    private static final String HOME_SLOT_CREATE_API = "admin.home-slot.create";
    private static final String HOME_SLOT_UPDATE_API = "admin.home-slot.update";
    private static final String HOME_SLOT_OFFLINE_API = "admin.home-slot.offline";
    private static final String HOME_SLOT_IMAGE_UPLOAD_API = "admin.home-slot.image.upload";
    private static final String HOME_SLOT_IMAGE_DELETE_API = "admin.home-slot.image.delete";
    private static final String RISK_HANDLE_API = "admin.risk-review.handle";
    private static final List<String> TARGET_TYPES = List.of("job", "company", "content", "event", "talent_snapshot");
    private static final List<String> HOME_SLOT_CODES = List.of(
            "home_hero_banner",
            "home_full_width_banner",
            "home_half_left",
            "home_half_right",
            "home_third_1",
            "home_third_2",
            "home_third_3",
            "home_quick_1",
            "home_quick_2",
            "home_quick_3",
            "home_bottom_banner");
    private static final List<String> HOME_SLOT_LINK_TYPES = List.of("none", "internal", "target");
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final int MAX_PAGE_SIZE = 100;

    private final AdminPortalOpsJdbcRepository repository;
    private final Phase3FeatureProperties phase3Features;
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;
    private final FieldAccessRecorder fieldAccessRecorder;
    private final CompanyStyleImageStorageService imageStorageService;
    private final long maxHomeSlotImageBytes;
    private final ObjectMapper objectMapper = AuditJsonMapper.create();

    public AdminPortalOpsService(
            AdminPortalOpsJdbcRepository repository,
            Phase3FeatureProperties phase3Features,
            IdempotencyService idempotencyService,
            AuditService auditService,
            FieldAccessRecorder fieldAccessRecorder,
            CompanyStyleImageStorageService imageStorageService,
            @Value("${localtalent.portal-home-slot-image.max-size-bytes:5242880}") long maxHomeSlotImageBytes
    ) {
        this.repository = repository;
        this.phase3Features = phase3Features;
        this.idempotencyService = idempotencyService;
        this.auditService = auditService;
        this.fieldAccessRecorder = fieldAccessRecorder;
        this.imageStorageService = imageStorageService;
        this.maxHomeSlotImageBytes = maxHomeSlotImageBytes;
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

    public HomeSlotPageResponse homeSlots(String slotCode, Integer status, int page, int size) {
        requireAdminReader();
        requireEnabled();
        String normalizedSlot = normalizeHomeSlotFilter(slotCode);
        Integer normalizedStatus = normalizeStatusFilter(status);
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = (normalizedPage - 1) * normalizedSize;
        return new HomeSlotPageResponse(
                repository.listHomeSlots(normalizedSlot, normalizedStatus, normalizedSize, offset),
                repository.countHomeSlots(normalizedSlot, normalizedStatus),
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
    public HomeSlotResponse createHomeSlot(HomeSlotRequest request, String idempotencyKey) {
        AuthzPrincipal principal = requireOperator();
        requireEnabled();
        NormalizedHomeSlot normalized = normalizeHomeSlot(request);
        return idempotencyService.execute(
                HOME_SLOT_CREATE_API,
                principal,
                idempotencyKey,
                normalized.fingerprint(),
                HomeSlotResponse.class,
                () -> createHomeSlotOnce(principal, normalized));
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
    public HomeSlotResponse updateHomeSlot(long id, HomeSlotRequest request, String idempotencyKey) {
        AuthzPrincipal principal = requireOperator();
        requireEnabled();
        NormalizedHomeSlot normalized = normalizeHomeSlot(request);
        long slotId = requirePositive(id, "slot_id");
        Map<String, Object> fingerprint = new LinkedHashMap<>(normalized.fingerprint());
        fingerprint.put("slot_id", slotId);
        return idempotencyService.execute(
                HOME_SLOT_UPDATE_API,
                principal,
                idempotencyKey,
                fingerprint,
                HomeSlotResponse.class,
                () -> updateHomeSlotOnce(principal, slotId, normalized));
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

    @Transactional
    public HomeSlotResponse offlineHomeSlot(long id, String idempotencyKey) {
        AuthzPrincipal principal = requireOperator();
        requireEnabled();
        long slotId = requirePositive(id, "slot_id");
        return idempotencyService.execute(
                HOME_SLOT_OFFLINE_API,
                principal,
                idempotencyKey,
                Map.of("slot_id", slotId, "action", "offline"),
                HomeSlotResponse.class,
                () -> offlineHomeSlotOnce(principal, slotId));
    }

    @Transactional
    public HomeSlotResponse uploadHomeSlotImage(long id, MultipartFile file, String idempotencyKey) {
        AuthzPrincipal principal = requireOperator();
        requireEnabled();
        long slotId = requirePositive(id, "slot_id");
        NormalizedHomeSlotImage image = normalizeHomeSlotImage(file);
        Map<String, Object> fingerprint = new LinkedHashMap<>();
        fingerprint.put("slot_id", slotId);
        fingerprint.put("file_name", image.fileName());
        fingerprint.put("content_type", image.contentType());
        fingerprint.put("size_bytes", image.content().length);
        fingerprint.put("sha256", image.sha256());
        return idempotencyService.execute(
                HOME_SLOT_IMAGE_UPLOAD_API,
                principal,
                idempotencyKey,
                fingerprint,
                HomeSlotResponse.class,
                () -> uploadHomeSlotImageOnce(principal, slotId, image));
    }

    public HomeSlotImageDownload homeSlotImageContent(long id) {
        AuthzPrincipal principal = requireAdminReader();
        requireEnabled();
        HomeSlotResponse slot = requireHomeSlot(id);
        if (!slot.hasImage() || slot.imageObjectKey() == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "home slot image not found");
        }
        fieldAccessRecorder.record(principal, "portal_home_operation_slot", slot.slotId(), "image_content", "HOME_SLOT_IMAGE_READ");
        return new HomeSlotImageDownload(slot.imageFileName(), slot.imageContentType(), imageStorageService.get(slot.imageObjectKey()));
    }

    @Transactional
    public HomeSlotResponse deleteHomeSlotImage(long id, String idempotencyKey) {
        AuthzPrincipal principal = requireOperator();
        requireEnabled();
        long slotId = requirePositive(id, "slot_id");
        return idempotencyService.execute(
                HOME_SLOT_IMAGE_DELETE_API,
                principal,
                idempotencyKey,
                Map.of("slot_id", slotId, "action", "delete_image"),
                HomeSlotResponse.class,
                () -> deleteHomeSlotImageOnce(principal, slotId));
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

    private IdempotentActionResult<HomeSlotResponse> createHomeSlotOnce(
            AuthzPrincipal principal,
            NormalizedHomeSlot normalized
    ) {
        validateHomeSlotTarget(normalized);
        long slotId = repository.createHomeSlot(
                normalized.slotCode(),
                normalized.title(),
                normalized.subtitle(),
                normalized.imageUrl(),
                normalized.imageAlt(),
                normalized.linkType(),
                normalized.linkUrl(),
                normalized.targetType(),
                normalized.targetId(),
                normalized.displayOrder(),
                normalized.status(),
                normalized.startTime(),
                normalized.endTime(),
                principal.userId());
        HomeSlotResponse after = requireHomeSlot(slotId);
        auditService.record(principal, "portal_home_operation_slot", slotId, "home_slot_create", null, safeHomeSlotAudit(after));
        return new IdempotentActionResult<>(after, "portal_home_operation_slot", slotId);
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

    private IdempotentActionResult<HomeSlotResponse> updateHomeSlotOnce(
            AuthzPrincipal principal,
            long slotId,
            NormalizedHomeSlot normalized
    ) {
        HomeSlotResponse before = requireHomeSlot(slotId);
        validateHomeSlotTarget(normalized);
        repository.updateHomeSlot(
                slotId,
                normalized.slotCode(),
                normalized.title(),
                normalized.subtitle(),
                normalized.imageUrl(),
                normalized.imageAlt(),
                normalized.linkType(),
                normalized.linkUrl(),
                normalized.targetType(),
                normalized.targetId(),
                normalized.displayOrder(),
                normalized.status(),
                normalized.startTime(),
                normalized.endTime(),
                principal.userId());
        HomeSlotResponse after = requireHomeSlot(slotId);
        auditService.record(principal, "portal_home_operation_slot", slotId, "home_slot_update",
                safeHomeSlotAudit(before), safeHomeSlotAudit(after));
        return new IdempotentActionResult<>(after, "portal_home_operation_slot", slotId);
    }

    private IdempotentActionResult<RecommendationResponse> offlineRecommendationOnce(AuthzPrincipal principal, long recommendationId) {
        RecommendationResponse before = requireRecommendation(recommendationId);
        repository.offlineRecommendation(recommendationId, principal.userId());
        RecommendationResponse after = requireRecommendation(recommendationId);
        auditService.record(principal, "portal_recommendation", recommendationId, "recommendation_offline",
                safeRecommendationAudit(before), safeRecommendationAudit(after));
        return new IdempotentActionResult<>(after, "portal_recommendation", recommendationId);
    }

    private IdempotentActionResult<HomeSlotResponse> offlineHomeSlotOnce(AuthzPrincipal principal, long slotId) {
        HomeSlotResponse before = requireHomeSlot(slotId);
        repository.offlineHomeSlot(slotId, principal.userId());
        HomeSlotResponse after = requireHomeSlot(slotId);
        auditService.record(principal, "portal_home_operation_slot", slotId, "home_slot_offline",
                safeHomeSlotAudit(before), safeHomeSlotAudit(after));
        return new IdempotentActionResult<>(after, "portal_home_operation_slot", slotId);
    }

    private IdempotentActionResult<HomeSlotResponse> uploadHomeSlotImageOnce(
            AuthzPrincipal principal,
            long slotId,
            NormalizedHomeSlotImage image
    ) {
        HomeSlotResponse before = requireHomeSlot(slotId);
        String objectKey = "portal-home-slots/%d/%s.%s".formatted(slotId, UUID.randomUUID(), image.extension());
        boolean stored = false;
        try {
            imageStorageService.put(objectKey, image.content(), image.contentType());
            stored = true;
            repository.updateHomeSlotImage(
                    slotId,
                    objectKey,
                    image.fileName(),
                    image.contentType(),
                    image.content().length,
                    image.sha256(),
                    principal.userId());
            if (before.imageObjectKey() != null && !before.imageObjectKey().isBlank()) {
                imageStorageService.deleteQuietly(before.imageObjectKey());
            }
            HomeSlotResponse after = requireHomeSlot(slotId);
            auditService.record(principal, "portal_home_operation_slot", slotId, "home_slot_image_upload",
                    safeHomeSlotAudit(before), safeHomeSlotAudit(after));
            return new IdempotentActionResult<>(after, "portal_home_operation_slot", slotId);
        } catch (RuntimeException exception) {
            if (stored) {
                imageStorageService.deleteQuietly(objectKey);
            }
            throw exception;
        }
    }

    private IdempotentActionResult<HomeSlotResponse> deleteHomeSlotImageOnce(AuthzPrincipal principal, long slotId) {
        HomeSlotResponse before = requireHomeSlot(slotId);
        if (before.hasImage()) {
            repository.clearHomeSlotImage(slotId, principal.userId());
            imageStorageService.deleteQuietly(before.imageObjectKey());
        }
        HomeSlotResponse after = requireHomeSlot(slotId);
        auditService.record(principal, "portal_home_operation_slot", slotId, "home_slot_image_delete",
                safeHomeSlotAudit(before), safeHomeSlotAudit(after));
        return new IdempotentActionResult<>(after, "portal_home_operation_slot", slotId);
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

    private void validateHomeSlotTarget(NormalizedHomeSlot slot) {
        if (!"target".equals(slot.linkType())) {
            return;
        }
        if (slot.targetType() == null || slot.targetId() == null) {
            throw validation("target_type and target_id are required for target link");
        }
        validateTarget(slot.targetType(), slot.targetId());
    }

    private RecommendationResponse requireRecommendation(long recommendationId) {
        return repository.findRecommendation(recommendationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "recommendation not found"));
    }

    private HomeSlotResponse requireHomeSlot(long slotId) {
        return repository.findHomeSlot(requirePositive(slotId, "slot_id"))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "home slot not found"));
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

    private NormalizedHomeSlot normalizeHomeSlot(HomeSlotRequest request) {
        if (request == null) {
            throw validation("request body is required");
        }
        String slotCode = required(request.slotCode(), "slot_code", 64);
        if (!HOME_SLOT_CODES.contains(slotCode)) {
            throw validation("slot_code is not supported");
        }
        String linkType = normalizeNullable(request.linkType(), 32);
        if (linkType == null) {
            linkType = "none";
        }
        if (!HOME_SLOT_LINK_TYPES.contains(linkType)) {
            throw validation("link_type is not supported");
        }
        String linkUrl = normalizeNullable(request.linkUrl(), 500);
        String targetType = normalizeTargetFilter(request.targetType());
        Long targetId = request.targetId() == null ? null : requirePositive(request.targetId(), "target_id");
        if ("none".equals(linkType)) {
            linkUrl = null;
            targetType = null;
            targetId = null;
        } else if ("internal".equals(linkType)) {
            if (linkUrl == null || !linkUrl.startsWith("/") || linkUrl.startsWith("//") || linkUrl.contains("://")) {
                throw validation("link_url must be an internal path");
            }
            targetType = null;
            targetId = null;
        } else if ("target".equals(linkType)) {
            linkUrl = null;
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
        return new NormalizedHomeSlot(
                slotCode,
                required(request.title(), "title", 200),
                normalizeNullable(request.subtitle(), 500),
                normalizeInternalAssetPath(request.imageUrl(), "image_url"),
                normalizeNullable(request.imageAlt(), 200),
                linkType,
                linkUrl,
                targetType,
                targetId,
                request.displayOrder() == null ? 100 : request.displayOrder(),
                status,
                start,
                end);
    }

    private NormalizedHomeSlotImage normalizeHomeSlotImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw validation("file is required");
        }
        if (file.getSize() > maxHomeSlotImageBytes) {
            throw validation("file exceeds max size");
        }
        String fileName = safeFileName(file.getOriginalFilename(), "home-slot-image");
        String extension = extension(fileName);
        String contentType = normalizeNullable(file.getContentType(), 120);
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType) || !extensionAllowed(contentType, extension)) {
            throw validation("file type must be JPEG, PNG or WebP");
        }
        try {
            byte[] content = file.getBytes();
            if (content.length == 0 || content.length > maxHomeSlotImageBytes) {
                throw validation("file exceeds max size");
            }
            return new NormalizedHomeSlotImage(fileName, contentType, extension, content, sha256(content));
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw validation("failed to read file");
        }
    }

    private String normalizeHomeSlotFilter(String slotCode) {
        String normalized = normalizeNullable(slotCode, 64);
        if (normalized != null && !HOME_SLOT_CODES.contains(normalized)) {
            throw validation("slot_code is not supported");
        }
        return normalized;
    }

    private String normalizeInternalAssetPath(String value, String fieldName) {
        String normalized = normalizeNullable(value, 500);
        if (normalized == null) {
            return null;
        }
        if (!normalized.startsWith("/") || normalized.startsWith("//") || normalized.contains("://")) {
            throw validation(fieldName + " must be an internal asset path before image upload is enabled");
        }
        return normalized;
    }

    private String safeFileName(String rawFileName, String fallback) {
        String raw = rawFileName == null || rawFileName.isBlank() ? fallback : rawFileName.trim();
        String normalizedPath = raw.replace('\\', '/');
        String name = normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1);
        String safe = name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (safe.isBlank() || !safe.contains(".")) {
            safe = fallback + ".png";
        }
        return safe.length() > 120 ? safe.substring(safe.length() - 120) : safe;
    }

    private String extension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            throw validation("file extension is required");
        }
        return fileName.substring(index + 1).toLowerCase();
    }

    private boolean extensionAllowed(String contentType, String extension) {
        return switch (contentType) {
            case "image/jpeg" -> extension.equals("jpg") || extension.equals("jpeg");
            case "image/png" -> extension.equals("png");
            case "image/webp" -> extension.equals("webp");
            default -> false;
        };
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception exception) {
            throw new IllegalStateException("failed to calculate image sha256", exception);
        }
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

    private String safeHomeSlotAudit(HomeSlotResponse response) {
        if (response == null) {
            return null;
        }
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("slot_id", response.slotId());
        audit.put("slot_code", response.slotCode());
        audit.put("link_type", response.linkType());
        audit.put("target_type", response.targetType());
        audit.put("target_id", response.targetId());
        audit.put("status", response.status());
        audit.put("has_image_url", response.imageUrl() != null && !response.imageUrl().isBlank());
        audit.put("has_uploaded_image", response.hasImage());
        audit.put("image_content_type", response.imageContentType());
        audit.put("image_size_bytes", response.imageSizeBytes());
        return json(audit);
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

    private record NormalizedHomeSlot(
            String slotCode,
            String title,
            String subtitle,
            String imageUrl,
            String imageAlt,
            String linkType,
            String linkUrl,
            String targetType,
            Long targetId,
            int displayOrder,
            int status,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        Map<String, Object> fingerprint() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("slot_code", slotCode);
            map.put("title", title);
            map.put("subtitle", subtitle == null ? "" : subtitle);
            map.put("image_url", imageUrl == null ? "" : imageUrl);
            map.put("image_alt", imageAlt == null ? "" : imageAlt);
            map.put("link_type", linkType);
            map.put("link_url", linkUrl == null ? "" : linkUrl);
            map.put("target_type", targetType == null ? "" : targetType);
            map.put("target_id", targetId == null ? 0 : targetId);
            map.put("display_order", displayOrder);
            map.put("status", status);
            map.put("start_time", startTime == null ? "" : startTime);
            map.put("end_time", endTime == null ? "" : endTime);
            return map;
        }
    }

    private record NormalizedHomeSlotImage(
            String fileName,
            String contentType,
            String extension,
            byte[] content,
            String sha256
    ) {
    }
}
