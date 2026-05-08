package cn.localtalent.backend.candidatecenter.application;

import cn.localtalent.backend.audit.AuditService;
import cn.localtalent.backend.audit.FieldAccessRecorder;
import cn.localtalent.backend.auth.domain.IdentityType;
import cn.localtalent.backend.authz.AuthzContext;
import cn.localtalent.backend.authz.AuthzPrincipal;
import cn.localtalent.backend.authz.DataScopeService;
import cn.localtalent.backend.authz.ResourceOwner;
import cn.localtalent.backend.candidate.application.IdempotencyService;
import cn.localtalent.backend.candidate.domain.IdempotentActionResult;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.ApplicationPageResponse;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.FavoriteCreateRequest;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.FavoritePageResponse;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.NotificationPageResponse;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.ResumeResponse;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.ResumeSaveRequest;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.SubscriptionCreateRequest;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.SubscriptionPageResponse;
import cn.localtalent.backend.candidatecenter.domain.CandidateResumeSummary;
import cn.localtalent.backend.candidatecenter.infrastructure.CandidateClosureJdbcRepository;
import cn.localtalent.backend.common.exception.ApiException;
import cn.localtalent.backend.common.json.AuditJsonMapper;
import cn.localtalent.backend.phase3.Phase3FeatureProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CandidateClosureService {

    private static final String RESUME_SAVE_API = "candidate.center.resume.write";
    private static final String FAVORITE_CREATE_API = "candidate.center.favorite.write";
    private static final String FAVORITE_CANCEL_API = "candidate.center.favorite.cancel";
    private static final String SUBSCRIPTION_CREATE_API = "candidate.center.subscription.write";
    private static final String SUBSCRIPTION_CANCEL_API = "candidate.center.subscription.cancel";
    private static final String NOTIFICATION_READ_API = "candidate.center.notification.readmark";

    private final CandidateClosureJdbcRepository repository;
    private final Phase3FeatureProperties phase3Features;
    private final IdempotencyService idempotencyService;
    private final DataScopeService dataScopeService;
    private final AuditService auditService;
    private final FieldAccessRecorder fieldAccessRecorder;
    private final ObjectMapper objectMapper = AuditJsonMapper.create();

    public CandidateClosureService(
            CandidateClosureJdbcRepository repository,
            Phase3FeatureProperties phase3Features,
            IdempotencyService idempotencyService,
            DataScopeService dataScopeService,
            AuditService auditService,
            FieldAccessRecorder fieldAccessRecorder
    ) {
        this.repository = repository;
        this.phase3Features = phase3Features;
        this.idempotencyService = idempotencyService;
        this.dataScopeService = dataScopeService;
        this.auditService = auditService;
        this.fieldAccessRecorder = fieldAccessRecorder;
    }

    public CandidateCenterDtos.PrivateStatsResponse stats(long candidateId) {
        if (!phase3Features.isCandidateClosure()) {
            return new CandidateCenterDtos.PrivateStatsResponse(0, 0, 0);
        }
        return new CandidateCenterDtos.PrivateStatsResponse(
                repository.countActiveFavorites(candidateId),
                repository.countActiveSubscriptions(candidateId),
                repository.countUnreadNotifications(candidateId));
    }

    public CandidateCenterDtos.FeatureResponse features() {
        return new CandidateCenterDtos.FeatureResponse(phase3Features.isCandidateClosure());
    }

    public CandidateCenterDtos.OnboardingResponse onboarding(
            long candidateId,
            CandidateResumeSummary resumeSummary,
            String publishStatus
    ) {
        if (!phase3Features.isCandidateClosure()) {
            return new CandidateCenterDtos.OnboardingResponse(false, "center", publishStatus);
        }
        return repository.findOnboardingState(candidateId)
                .map(state -> onboardingFromState(state, publishStatus))
                .orElseGet(() -> onboardingFromLegacyResume(resumeSummary, publishStatus));
    }

    public ResumeResponse resume() {
        AuthzPrincipal principal = requireCandidate();
        requireEnabled();
        dataScopeService.assertAccessible(principal, "candidate_resume", ResourceOwner.candidate(principal.userId()));
        recordResumeFieldRead(principal, "SELF_READ");
        return repository.latestResume(principal.userId());
    }

    public ResumeResponse preview() {
        AuthzPrincipal principal = requireCandidate();
        requireEnabled();
        dataScopeService.assertAccessible(principal, "candidate_resume", ResourceOwner.candidate(principal.userId()));
        recordResumeFieldRead(principal, "SELF_READ");
        return repository.latestResume(principal.userId());
    }

    @Transactional
    public ResumeResponse saveResume(ResumeSaveRequest request, String idempotencyKey) {
        AuthzPrincipal principal = requireCandidate();
        requireEnabled();
        NormalizedResume normalized = normalizeResume(request);
        return idempotencyService.execute(
                RESUME_SAVE_API,
                principal,
                idempotencyKey,
                normalized.fingerprint(),
                ResumeResponse.class,
                () -> saveResumeOnce(principal, normalized));
    }

    public ApplicationPageResponse applications(int page, int size) {
        AuthzPrincipal principal = requireCandidate();
        requireEnabled();
        dataScopeService.assertAccessible(principal, "job_application", ResourceOwner.candidate(principal.userId()));
        return new ApplicationPageResponse(
                repository.listApplications(principal.userId(), page, size),
                repository.countApplications(principal.userId()));
    }

    public FavoritePageResponse favorites(int page, int size) {
        AuthzPrincipal principal = requireCandidate();
        requireEnabled();
        dataScopeService.assertAccessible(principal, "candidate_job_favorite", ResourceOwner.candidate(principal.userId()));
        return new FavoritePageResponse(
                repository.listFavorites(principal.userId(), page, size),
                repository.countActiveFavorites(principal.userId()));
    }

    @Transactional
    public FavoritePageResponse createFavorite(FavoriteCreateRequest request, String idempotencyKey) {
        AuthzPrincipal principal = requireCandidate();
        requireEnabled();
        long jobId = requirePositive(request == null ? null : request.jobId(), "job_id");
        return idempotencyService.execute(
                FAVORITE_CREATE_API,
                principal,
                idempotencyKey,
                Map.of("job_id", jobId),
                FavoritePageResponse.class,
                () -> createFavoriteOnce(principal, jobId));
    }

    @Transactional
    public FavoritePageResponse cancelFavorite(long favoriteId, String idempotencyKey) {
        AuthzPrincipal principal = requireCandidate();
        requireEnabled();
        long normalizedFavoriteId = requirePositive(favoriteId, "favorite_id");
        return idempotencyService.execute(
                FAVORITE_CANCEL_API,
                principal,
                idempotencyKey,
                Map.of("favorite_id", normalizedFavoriteId),
                FavoritePageResponse.class,
                () -> cancelFavoriteOnce(principal, normalizedFavoriteId));
    }

    public SubscriptionPageResponse subscriptions(int page, int size) {
        AuthzPrincipal principal = requireCandidate();
        requireEnabled();
        dataScopeService.assertAccessible(
                principal,
                "candidate_search_subscription",
                ResourceOwner.candidate(principal.userId()));
        return new SubscriptionPageResponse(
                repository.listSubscriptions(principal.userId(), page, size),
                repository.countActiveSubscriptions(principal.userId()));
    }

    @Transactional
    public SubscriptionPageResponse createSubscription(SubscriptionCreateRequest request, String idempotencyKey) {
        AuthzPrincipal principal = requireCandidate();
        requireEnabled();
        NormalizedSubscription normalized = normalizeSubscription(request);
        return idempotencyService.execute(
                SUBSCRIPTION_CREATE_API,
                principal,
                idempotencyKey,
                normalized.fingerprint(),
                SubscriptionPageResponse.class,
                () -> createSubscriptionOnce(principal, normalized));
    }

    @Transactional
    public SubscriptionPageResponse cancelSubscription(long subscriptionId, String idempotencyKey) {
        AuthzPrincipal principal = requireCandidate();
        requireEnabled();
        long normalizedSubscriptionId = requirePositive(subscriptionId, "subscription_id");
        return idempotencyService.execute(
                SUBSCRIPTION_CANCEL_API,
                principal,
                idempotencyKey,
                Map.of("subscription_id", normalizedSubscriptionId),
                SubscriptionPageResponse.class,
                () -> cancelSubscriptionOnce(principal, normalizedSubscriptionId));
    }

    public NotificationPageResponse notifications(int page, int size) {
        AuthzPrincipal principal = requireCandidate();
        requireEnabled();
        dataScopeService.assertAccessible(principal, "candidate_notification", ResourceOwner.candidate(principal.userId()));
        return new NotificationPageResponse(
                repository.listNotifications(principal.userId(), page, size),
                repository.countUnreadNotifications(principal.userId()));
    }

    @Transactional
    public NotificationPageResponse markNotificationRead(long notificationId, String idempotencyKey) {
        AuthzPrincipal principal = requireCandidate();
        requireEnabled();
        long normalizedNotificationId = requirePositive(notificationId, "notification_id");
        return idempotencyService.execute(
                NOTIFICATION_READ_API,
                principal,
                idempotencyKey,
                Map.of("notification_id", normalizedNotificationId),
                NotificationPageResponse.class,
                () -> markNotificationReadOnce(principal, normalizedNotificationId));
    }

    private IdempotentActionResult<ResumeResponse> saveResumeOnce(AuthzPrincipal principal, NormalizedResume resume) {
        dataScopeService.assertWritable(principal, "candidate_resume", ResourceOwner.candidate(principal.userId()));
        long resumeId = repository.saveResume(
                principal.userId(),
                resume.resumeName(),
                toJson(resume.baseProfile()),
                toJson(resume.educationExperience().isEmpty() ? resume.education() : resume.educationExperience()),
                toJson(resume.workExperience().isEmpty() ? resume.experience() : resume.workExperience()),
                toJson(resume.skills()));
        recordResumeFieldRead(principal, "SELF_WRITE");
        ResumeResponse response = repository.latestResume(principal.userId());
        OnboardingProgress progress = onboardingProgress(resume, response.completionPercent());
        long onboardingId = repository.upsertOnboardingState(
                principal.userId(),
                resumeId,
                progress.status(),
                progress.currentStep(),
                response.completionPercent());
        auditService.record(
                principal,
                "candidate_resume",
                resumeId,
                "resume_save",
                null,
                "{\"resume_status\":\"saved\"}");
        auditService.record(
                principal,
                "candidate_resume_onboarding",
                onboardingId,
                "resume_onboarding_update",
                null,
                "{\"onboarding_status\":\"" + progress.status() + "\",\"current_step\":\""
                        + progress.currentStep() + "\",\"resume_id\":" + resumeId + ",\"completion_score\":"
                        + response.completionPercent() + "}");
        return new IdempotentActionResult<>(response, "candidate_resume", resumeId);
    }

    private IdempotentActionResult<FavoritePageResponse> createFavoriteOnce(AuthzPrincipal principal, long jobId) {
        dataScopeService.assertWritable(
                principal,
                "candidate_job_favorite",
                ResourceOwner.candidate(principal.userId()));
        if (!repository.isVisibleJob(jobId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "job not available");
        }
        long favoriteId = repository.upsertFavorite(principal.userId(), jobId);
        auditService.record(
                principal,
                "candidate_job_favorite",
                favoriteId,
                "favorite_create",
                null,
                "{\"favorite_status\":\"active\"}");
        return new IdempotentActionResult<>(
                favorites(1, 20),
                "candidate_job_favorite",
                favoriteId);
    }

    private IdempotentActionResult<FavoritePageResponse> cancelFavoriteOnce(AuthzPrincipal principal, long favoriteId) {
        dataScopeService.assertWritable(
                principal,
                "candidate_job_favorite",
                ResourceOwner.candidate(principal.userId()));
        if (!repository.cancelFavorite(principal.userId(), favoriteId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "favorite not found");
        }
        auditService.record(
                principal,
                "candidate_job_favorite",
                favoriteId,
                "favorite_cancel",
                "{\"favorite_status\":\"active\"}",
                "{\"favorite_status\":\"cancelled\"}");
        return new IdempotentActionResult<>(
                favorites(1, 20),
                "candidate_job_favorite",
                favoriteId);
    }

    private IdempotentActionResult<SubscriptionPageResponse> createSubscriptionOnce(
            AuthzPrincipal principal,
            NormalizedSubscription request
    ) {
        dataScopeService.assertWritable(
                principal,
                "candidate_search_subscription",
                ResourceOwner.candidate(principal.userId()));
        long subscriptionId = repository.createSubscription(
                principal.userId(),
                request.subscriptionName(),
                request.keyword(),
                request.cityCode(),
                request.categoryCode(),
                request.salaryMin(),
                request.salaryMax());
        repository.createNotification(
                principal.userId(),
                "subscription_created",
                "搜索订阅已创建",
                "站内订阅已保存，后续只通过站内通知提醒。",
                "candidate_search_subscription",
                subscriptionId);
        auditService.record(
                principal,
                "candidate_search_subscription",
                subscriptionId,
                "subscription_create",
                null,
                "{\"subscription_status\":\"active\"}");
        return new IdempotentActionResult<>(
                subscriptions(1, 20),
                "candidate_search_subscription",
                subscriptionId);
    }

    private IdempotentActionResult<SubscriptionPageResponse> cancelSubscriptionOnce(
            AuthzPrincipal principal,
            long subscriptionId
    ) {
        dataScopeService.assertWritable(
                principal,
                "candidate_search_subscription",
                ResourceOwner.candidate(principal.userId()));
        if (!repository.cancelSubscription(principal.userId(), subscriptionId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "subscription not found");
        }
        auditService.record(
                principal,
                "candidate_search_subscription",
                subscriptionId,
                "subscription_cancel",
                "{\"subscription_status\":\"active\"}",
                "{\"subscription_status\":\"cancelled\"}");
        return new IdempotentActionResult<>(
                subscriptions(1, 20),
                "candidate_search_subscription",
                subscriptionId);
    }

    private IdempotentActionResult<NotificationPageResponse> markNotificationReadOnce(
            AuthzPrincipal principal,
            long notificationId
    ) {
        dataScopeService.assertWritable(principal, "candidate_notification", ResourceOwner.candidate(principal.userId()));
        if (!repository.markNotificationRead(principal.userId(), notificationId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "notification not found");
        }
        auditService.record(
                principal,
                "candidate_notification",
                notificationId,
                "notification_read",
                "{\"read_status\":\"unread\"}",
                "{\"read_status\":\"read\"}");
        return new IdempotentActionResult<>(
                notifications(1, 20),
                "candidate_notification",
                notificationId);
    }

    private AuthzPrincipal requireCandidate() {
        AuthzPrincipal principal = AuthzContext.requireCurrentPrincipal();
        if (principal.identityType() != IdentityType.CANDIDATE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTHZ_403", "candidate identity required");
        }
        return principal;
    }

    private void requireEnabled() {
        if (!phase3Features.isCandidateClosure()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FEATURE_DISABLED_403", "candidate closure disabled");
        }
    }

    private CandidateCenterDtos.OnboardingResponse onboardingFromState(
            CandidateClosureJdbcRepository.OnboardingState state,
            String publishStatus
    ) {
        if ("completed".equals(state.onboardingStatus())) {
            return new CandidateCenterDtos.OnboardingResponse(false, "center", publishStatus);
        }
        String step = "detail".equals(state.currentStep()) ? "detail" : "basic";
        return new CandidateCenterDtos.OnboardingResponse(true, step, publishStatus);
    }

    private CandidateCenterDtos.OnboardingResponse onboardingFromLegacyResume(
            CandidateResumeSummary resumeSummary,
            String publishStatus
    ) {
        int completion = resumeSummary.completionPercent();
        if (completion >= 80) {
            return new CandidateCenterDtos.OnboardingResponse(false, "center", publishStatus);
        }
        return new CandidateCenterDtos.OnboardingResponse(true, completion < 40 ? "basic" : "detail", publishStatus);
    }

    private OnboardingProgress onboardingProgress(NormalizedResume resume, int completionPercent) {
        if (completionPercent >= 80) {
            return new OnboardingProgress("completed", "done");
        }
        if (hasDetailContent(resume)) {
            return new OnboardingProgress("detail_saved", "detail");
        }
        if (completionPercent > 0) {
            return new OnboardingProgress("basic_saved", "detail");
        }
        return new OnboardingProgress("not_started", "basic");
    }

    private boolean hasDetailContent(NormalizedResume resume) {
        return !resume.workExperience().isEmpty()
                || !resume.educationExperience().isEmpty()
                || !resume.education().isEmpty()
                || !resume.experience().isEmpty()
                || !resume.skills().isEmpty()
                || resume.baseProfile().get("self_description") instanceof String text && isPresent(text);
    }

    private NormalizedResume normalizeResume(ResumeSaveRequest request) {
        if (request == null) {
            throw validation("request body is required");
        }
        CandidateCenterDtos.BaseProfileRequest profile = request.baseProfile();
        Map<String, Object> baseProfile = new LinkedHashMap<>();
        baseProfile.put("display_name", normalize(profile == null ? null : profile.displayName(), 64));
        baseProfile.put("city_code", normalize(profile == null ? null : profile.cityCode(), 32));
        baseProfile.put("category_code", normalize(profile == null ? null : profile.categoryCode(), 64));
        Integer experienceYears = profile == null ? null : profile.experienceYears();
        if (experienceYears != null && (experienceYears < 0 || experienceYears > 80)) {
            throw validation("experience_years must be between 0 and 80");
        }
        baseProfile.put("experience_years", experienceYears);
        String selfDescription = normalize(request.selfDescription(), 1000);
        String summary = normalize(profile == null ? null : profile.summary(), 500);
        baseProfile.put("summary", summary.isBlank() ? selfDescription : summary);
        baseProfile.put("gender", normalize(profile == null ? null : profile.gender(), 16));
        baseProfile.put("birth_date", normalize(profile == null ? null : profile.birthDate(), 32));
        baseProfile.put("highest_education", normalize(profile == null ? null : profile.highestEducation(), 32));
        baseProfile.put("start_work_date", normalize(profile == null ? null : profile.startWorkDate(), 32));
        baseProfile.put("no_experience", Boolean.TRUE.equals(profile == null ? null : profile.noExperience()));
        baseProfile.put("contact_phone", normalize(profile == null ? null : profile.contactPhone(), 32));
        baseProfile.put("contact_wechat", normalize(profile == null ? null : profile.contactWechat(), 64));
        baseProfile.put("wechat_same_as_phone", Boolean.TRUE.equals(profile == null ? null : profile.wechatSameAsPhone()));
        baseProfile.put("expected_positions", normalizeList(profile == null ? null : profile.expectedPositions(), 5, 80));
        baseProfile.put("expected_salary", normalize(profile == null ? null : profile.expectedSalary(), 64));
        baseProfile.put("expected_cities", normalizeList(profile == null ? null : profile.expectedCities(), 5, 80));
        baseProfile.put("job_status", normalize(profile == null ? null : profile.jobStatus(), 80));
        baseProfile.put("self_description", selfDescription);
        List<Map<String, Object>> workExperience = normalizeWorkExperience(request.workExperience());
        List<Map<String, Object>> educationExperience = normalizeEducationExperience(request.educationExperience());
        return new NormalizedResume(
                defaultText(request.resumeName(), "我的简历", 120),
                baseProfile,
                educationExperience.isEmpty() ? normalizeList(request.education(), 20, 200) : summarizeEducation(educationExperience),
                workExperience.isEmpty() ? normalizeList(request.experience(), 20, 200) : summarizeWork(workExperience),
                normalizeList(request.skills(), 20, 80),
                workExperience,
                educationExperience);
    }

    private List<Map<String, Object>> normalizeWorkExperience(List<CandidateCenterDtos.WorkExperienceRequest> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null)
                .map(value -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("company_name", normalize(value.companyName(), 120));
                    item.put("position_name", normalize(value.positionName(), 120));
                    item.put("start_date", normalize(value.startDate(), 32));
                    item.put("end_date", normalize(value.endDate(), 32));
                    item.put("ongoing", Boolean.TRUE.equals(value.ongoing()));
                    item.put("responsibility", normalize(value.responsibility(), 1000));
                    return item;
                })
                .filter(item -> item.values().stream().anyMatch(value -> value instanceof String text && !text.isBlank()))
                .limit(10)
                .toList();
    }

    private List<Map<String, Object>> normalizeEducationExperience(List<CandidateCenterDtos.EducationExperienceRequest> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null)
                .map(value -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("school_name", normalize(value.schoolName(), 120));
                    item.put("major_name", normalize(value.majorName(), 120));
                    item.put("start_date", normalize(value.startDate(), 32));
                    item.put("end_date", normalize(value.endDate(), 32));
                    item.put("ongoing", Boolean.TRUE.equals(value.ongoing()));
                    item.put("degree", normalize(value.degree(), 32));
                    return item;
                })
                .filter(item -> item.values().stream().anyMatch(value -> value instanceof String text && !text.isBlank()))
                .limit(10)
                .toList();
    }

    private List<String> summarizeWork(List<Map<String, Object>> values) {
        return values.stream()
                .map(value -> String.join(" / ",
                        normalize(String.valueOf(value.get("company_name")), 120),
                        normalize(String.valueOf(value.get("position_name")), 120)))
                .map(value -> value.replaceAll("(^ / | / $)", "").trim())
                .filter(value -> !value.isBlank())
                .limit(20)
                .toList();
    }

    private List<String> summarizeEducation(List<Map<String, Object>> values) {
        return values.stream()
                .map(value -> String.join(" / ",
                        normalize(String.valueOf(value.get("school_name")), 120),
                        normalize(String.valueOf(value.get("major_name")), 120),
                        normalize(String.valueOf(value.get("degree")), 32)))
                .map(value -> value.replaceAll("(^ / | / $)", "").trim())
                .filter(value -> !value.isBlank())
                .limit(20)
                .toList();
    }

    private NormalizedSubscription normalizeSubscription(SubscriptionCreateRequest request) {
        if (request == null) {
            throw validation("request body is required");
        }
        Integer salaryMin = request.salaryMin();
        Integer salaryMax = request.salaryMax();
        if (salaryMin != null && salaryMin < 0 || salaryMax != null && salaryMax < 0) {
            throw validation("salary must not be negative");
        }
        if (salaryMin != null && salaryMax != null && salaryMin > salaryMax) {
            throw validation("salary_min must not be greater than salary_max");
        }
        return new NormalizedSubscription(
                defaultText(request.subscriptionName(), "我的职位订阅", 120),
                normalize(request.keyword(), 120),
                normalize(request.cityCode(), 32),
                normalize(request.categoryCode(), 64),
                salaryMin,
                salaryMax);
    }

    private List<String> normalizeList(List<String> values, int maxItems, int maxLength) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(value -> normalize(value, maxLength))
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .limit(maxItems)
                .toList();
    }

    private long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw validation(fieldName + " must be positive");
        }
        return value;
    }

    private long requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw validation(fieldName + " must be positive");
        }
        return value;
    }

    private void recordResumeFieldRead(AuthzPrincipal principal, String accessType) {
        fieldAccessRecorder.record(principal, "candidate_resume", principal.userId(), "base_profile_json", accessType);
        fieldAccessRecorder.record(principal, "candidate_resume", principal.userId(), "education_json", accessType);
        fieldAccessRecorder.record(principal, "candidate_resume", principal.userId(), "experience_json", accessType);
        fieldAccessRecorder.record(principal, "candidate_resume", principal.userId(), "skills_json", accessType);
    }

    private String normalize(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            return normalized.substring(0, maxLength);
        }
        return normalized;
    }

    private String defaultText(String value, String defaultValue, int maxLength) {
        String normalized = normalize(value, maxLength);
        return normalized.isBlank() ? defaultValue : normalized;
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to write json", exception);
        }
    }

    private ApiException validation(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALID_400", message);
    }

    private record NormalizedResume(
            String resumeName,
            Map<String, Object> baseProfile,
            List<String> education,
            List<String> experience,
            List<String> skills,
            List<Map<String, Object>> workExperience,
            List<Map<String, Object>> educationExperience
    ) {

        Map<String, Object> fingerprint() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("resume_name", resumeName);
            payload.put("base_profile", baseProfile);
            payload.put("education", education);
            payload.put("experience", experience);
            payload.put("skills", skills);
            payload.put("work_experience", workExperience);
            payload.put("education_experience", educationExperience);
            return payload;
        }
    }

    private record NormalizedSubscription(
            String subscriptionName,
            String keyword,
            String cityCode,
            String categoryCode,
            Integer salaryMin,
            Integer salaryMax
    ) {

        Map<String, Object> fingerprint() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("subscription_name", subscriptionName);
            payload.put("keyword", keyword);
            payload.put("city_code", cityCode);
            payload.put("category_code", categoryCode);
            payload.put("salary_min", salaryMin);
            payload.put("salary_max", salaryMax);
            return payload;
        }
    }

    private record OnboardingProgress(
            String status,
            String currentStep
    ) {
    }
}
