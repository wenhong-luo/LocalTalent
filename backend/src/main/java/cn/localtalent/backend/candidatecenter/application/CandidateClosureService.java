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
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.AiSuggestionItemResponse;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.AiSuggestionTaskResponse;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.AttachmentDownload;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.AttachmentResponse;
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
import cn.localtalent.backend.candidatecenter.infrastructure.CandidateClosureJdbcRepository.AiSuggestionItemRow;
import cn.localtalent.backend.candidatecenter.infrastructure.CandidateClosureJdbcRepository.AiSuggestionTaskRow;
import cn.localtalent.backend.candidatecenter.infrastructure.CandidateClosureJdbcRepository.AttachmentState;
import cn.localtalent.backend.candidatecenter.infrastructure.CandidateResumeAttachmentStorageService;
import cn.localtalent.backend.common.exception.ApiException;
import cn.localtalent.backend.common.json.AuditJsonMapper;
import cn.localtalent.backend.phase3.Phase3FeatureProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CandidateClosureService {

    private static final String RESUME_SAVE_API = "candidate.center.resume.write";
    private static final String FAVORITE_CREATE_API = "candidate.center.favorite.write";
    private static final String FAVORITE_CANCEL_API = "candidate.center.favorite.cancel";
    private static final String SUBSCRIPTION_CREATE_API = "candidate.center.subscription.write";
    private static final String SUBSCRIPTION_CANCEL_API = "candidate.center.subscription.cancel";
    private static final String NOTIFICATION_READ_API = "candidate.center.notification.readmark";
    private static final String ATTACHMENT_UPLOAD_API = "candidate.center.resume.attachment.upload";
    private static final String ATTACHMENT_DELETE_API = "candidate.center.resume.attachment.delete";
    private static final String AI_SUGGESTION_GENERATE_API = "candidate.center.resume.ai.generate";
    private static final String AI_SUGGESTION_APPLY_API = "candidate.center.resume.ai.apply";
    private static final String AI_SUGGESTION_DISMISS_API = "candidate.center.resume.ai.dismiss";
    private static final Set<String> ALLOWED_ATTACHMENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final CandidateClosureJdbcRepository repository;
    private final Phase3FeatureProperties phase3Features;
    private final IdempotencyService idempotencyService;
    private final DataScopeService dataScopeService;
    private final AuditService auditService;
    private final FieldAccessRecorder fieldAccessRecorder;
    private final CandidateResumeAttachmentStorageService attachmentStorageService;
    private final long maxAttachmentSizeBytes;
    private final ObjectMapper objectMapper = AuditJsonMapper.create();

    public CandidateClosureService(
            CandidateClosureJdbcRepository repository,
            Phase3FeatureProperties phase3Features,
            IdempotencyService idempotencyService,
            DataScopeService dataScopeService,
            AuditService auditService,
            FieldAccessRecorder fieldAccessRecorder,
            CandidateResumeAttachmentStorageService attachmentStorageService,
            @Value("${localtalent.resume-attachment.max-size-bytes}") long maxAttachmentSizeBytes
    ) {
        this.repository = repository;
        this.phase3Features = phase3Features;
        this.idempotencyService = idempotencyService;
        this.dataScopeService = dataScopeService;
        this.auditService = auditService;
        this.fieldAccessRecorder = fieldAccessRecorder;
        this.attachmentStorageService = attachmentStorageService;
        this.maxAttachmentSizeBytes = maxAttachmentSizeBytes;
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
        return new CandidateCenterDtos.FeatureResponse(
                phase3Features.isCandidateClosure(),
                phase3Features.isResumeAttachmentUpload(),
                phase3Features.isResumeAiAssist());
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

    public AttachmentResponse attachment() {
        AuthzPrincipal principal = requireCandidate();
        requireAttachmentEnabled();
        dataScopeService.assertAccessible(principal, "candidate_resume", ResourceOwner.candidate(principal.userId()));
        return repository.attachmentState(principal.userId())
                .map(this::toAttachmentResponse)
                .orElseGet(AttachmentResponse::empty);
    }

    @Transactional
    public AttachmentResponse uploadAttachment(MultipartFile file, String idempotencyKey) {
        AuthzPrincipal principal = requireCandidate();
        requireAttachmentEnabled();
        NormalizedAttachment attachment = normalizeAttachment(file);
        return idempotencyService.execute(
                ATTACHMENT_UPLOAD_API,
                principal,
                idempotencyKey,
                attachment.fingerprint(),
                AttachmentResponse.class,
                () -> uploadAttachmentOnce(principal, attachment));
    }

    public AttachmentDownload downloadAttachment() {
        AuthzPrincipal principal = requireCandidate();
        requireAttachmentEnabled();
        dataScopeService.assertAccessible(principal, "candidate_resume", ResourceOwner.candidate(principal.userId()));
        AttachmentState state = repository.attachmentState(principal.userId())
                .filter(value -> isPresent(value.objectKey()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "resume attachment not found"));
        fieldAccessRecorder.record(
                principal,
                "candidate_resume",
                state.resumeId(),
                "resume_attachment",
                "SELF_ATTACHMENT_DOWNLOAD");
        byte[] content = attachmentStorageService.get(state.objectKey());
        return new AttachmentDownload(
                defaultText(state.fileName(), "resume-attachment", 180),
                defaultText(state.contentType(), "application/octet-stream", 120),
                content);
    }

    @Transactional
    public AttachmentResponse deleteAttachment(String idempotencyKey) {
        AuthzPrincipal principal = requireCandidate();
        requireAttachmentEnabled();
        AttachmentState state = repository.attachmentState(principal.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "resume not found"));
        return idempotencyService.execute(
                ATTACHMENT_DELETE_API,
                principal,
                idempotencyKey,
                Map.of("resume_id", state.resumeId(), "had_attachment", isPresent(state.objectKey())),
                AttachmentResponse.class,
                () -> deleteAttachmentOnce(principal, state));
    }

    @Transactional
    public AiSuggestionTaskResponse generateAiSuggestions(String idempotencyKey) {
        AuthzPrincipal principal = requireCandidate();
        requireAiAssistEnabled();
        ResumeResponse resume = repository.latestResume(principal.userId());
        if (resume.resumeId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "RESUME_REQUIRED_400", "resume is required before ai assist");
        }
        dataScopeService.assertAccessible(principal, "candidate_resume", ResourceOwner.candidate(principal.userId()));
        recordResumeFieldRead(principal, "SELF_AI_ASSIST");
        return idempotencyService.execute(
                AI_SUGGESTION_GENERATE_API,
                principal,
                idempotencyKey,
                Map.of("resume_id", resume.resumeId(), "completion_percent", resume.completionPercent()),
                AiSuggestionTaskResponse.class,
                () -> generateAiSuggestionsOnce(principal, resume));
    }

    public AiSuggestionTaskResponse latestAiSuggestions() {
        AuthzPrincipal principal = requireCandidate();
        requireAiAssistEnabled();
        dataScopeService.assertAccessible(principal, "candidate_resume", ResourceOwner.candidate(principal.userId()));
        return latestAiSuggestionResponse(principal.userId());
    }

    @Transactional
    public AiSuggestionTaskResponse applyAiSuggestion(long itemId, String idempotencyKey) {
        AuthzPrincipal principal = requireCandidate();
        requireAiAssistEnabled();
        long normalizedItemId = requirePositive(itemId, "suggestion_id");
        return idempotencyService.execute(
                AI_SUGGESTION_APPLY_API,
                principal,
                idempotencyKey,
                Map.of("suggestion_id", normalizedItemId),
                AiSuggestionTaskResponse.class,
                () -> applyAiSuggestionOnce(principal, normalizedItemId));
    }

    @Transactional
    public AiSuggestionTaskResponse dismissAiSuggestion(long itemId, String idempotencyKey) {
        AuthzPrincipal principal = requireCandidate();
        requireAiAssistEnabled();
        long normalizedItemId = requirePositive(itemId, "suggestion_id");
        return idempotencyService.execute(
                AI_SUGGESTION_DISMISS_API,
                principal,
                idempotencyKey,
                Map.of("suggestion_id", normalizedItemId),
                AiSuggestionTaskResponse.class,
                () -> dismissAiSuggestionOnce(principal, normalizedItemId));
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

    private IdempotentActionResult<AttachmentResponse> uploadAttachmentOnce(
            AuthzPrincipal principal,
            NormalizedAttachment attachment
    ) {
        dataScopeService.assertWritable(principal, "candidate_resume", ResourceOwner.candidate(principal.userId()));
        AttachmentState state = repository.attachmentState(principal.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "RESUME_REQUIRED_400", "resume is required before attachment upload"));
        String objectKey = "candidate-resume-attachments/%d/%d/%s.%s".formatted(
                principal.userId(),
                state.resumeId(),
                UUID.randomUUID(),
                attachment.extension());
        try {
            attachmentStorageService.put(objectKey, attachment.content(), attachment.contentType());
            repository.updateAttachment(
                    principal.userId(),
                    state.resumeId(),
                    objectKey,
                    attachment.fileName(),
                    attachment.contentType(),
                    attachment.sizeBytes(),
                    attachment.sha256());
        } catch (RuntimeException exception) {
            attachmentStorageService.deleteQuietly(objectKey);
            throw exception;
        }
        if (isPresent(state.objectKey())) {
            attachmentStorageService.deleteQuietly(state.objectKey());
        }
        fieldAccessRecorder.record(
                principal,
                "candidate_resume",
                state.resumeId(),
                "resume_attachment",
                "SELF_ATTACHMENT_UPLOAD");
        auditService.record(
                principal,
                "candidate_resume",
                state.resumeId(),
                "resume_attachment_upload",
                attachmentAuditJson(state, false),
                attachmentAuditJson(attachment, true));
        AttachmentResponse response = repository.attachmentState(principal.userId())
                .map(this::toAttachmentResponse)
                .orElseGet(AttachmentResponse::empty);
        return new IdempotentActionResult<>(response, "candidate_resume", state.resumeId());
    }

    private IdempotentActionResult<AttachmentResponse> deleteAttachmentOnce(AuthzPrincipal principal, AttachmentState state) {
        dataScopeService.assertWritable(principal, "candidate_resume", ResourceOwner.candidate(principal.userId()));
        repository.clearAttachment(principal.userId(), state.resumeId());
        attachmentStorageService.deleteQuietly(state.objectKey());
        fieldAccessRecorder.record(
                principal,
                "candidate_resume",
                state.resumeId(),
                "resume_attachment",
                "SELF_ATTACHMENT_DELETE");
        auditService.record(
                principal,
                "candidate_resume",
                state.resumeId(),
                "resume_attachment_delete",
                attachmentAuditJson(state, true),
                "{\"has_attachment\":false}");
        return new IdempotentActionResult<>(AttachmentResponse.empty(), "candidate_resume", state.resumeId());
    }

    private IdempotentActionResult<AiSuggestionTaskResponse> generateAiSuggestionsOnce(
            AuthzPrincipal principal,
            ResumeResponse resume
    ) {
        dataScopeService.assertWritable(principal, "candidate_resume_ai_suggestion_task", ResourceOwner.candidate(principal.userId()));
        List<GeneratedSuggestion> suggestions = buildAiSuggestions(resume);
        long taskId = repository.createAiSuggestionTask(principal.userId(), resume.resumeId(), suggestions.size());
        for (GeneratedSuggestion suggestion : suggestions) {
            repository.createAiSuggestionItem(
                    taskId,
                    principal.userId(),
                    resume.resumeId(),
                    suggestion.suggestionType(),
                    suggestion.targetField(),
                    suggestion.title(),
                    suggestion.reasonSummary(),
                    suggestion.beforePreview(),
                    suggestion.suggestedValue(),
                    suggestion.canApply());
        }
        auditService.record(
                principal,
                "resume_ai_task",
                taskId,
                "resume_ai_suggestions_generate",
                null,
                toJson(Map.of("resume_id", resume.resumeId(), "suggestion_count", suggestions.size())));
        return new IdempotentActionResult<>(
                latestAiSuggestionResponse(principal.userId()),
                "candidate_resume_ai_suggestion_task",
                taskId);
    }

    private IdempotentActionResult<AiSuggestionTaskResponse> applyAiSuggestionOnce(
            AuthzPrincipal principal,
            long itemId
    ) {
        dataScopeService.assertWritable(principal, "candidate_resume_ai_suggestion_item", ResourceOwner.candidate(principal.userId()));
        AiSuggestionItemRow item = repository.findAiSuggestionItem(principal.userId(), itemId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "ai suggestion not found"));
        if (!item.canApply()) {
            throw validation("ai suggestion is guidance only and cannot be applied");
        }
        if (!"pending".equals(item.applyStatus())) {
            return new IdempotentActionResult<>(
                    latestAiSuggestionResponse(principal.userId()),
                    "candidate_resume_ai_suggestion_item",
                    itemId);
        }
        ResumeResponse resume = repository.latestResume(principal.userId());
        if (resume.resumeId() == null || item.resumeId() != null && !item.resumeId().equals(resume.resumeId())) {
            throw new ApiException(HttpStatus.CONFLICT, "RESUME_CHANGED_409", "resume changed after ai suggestion generated");
        }
        recordResumeFieldRead(principal, "SELF_AI_ASSIST");
        ResumeSaveRequest request = applySuggestion(resume, item);
        NormalizedResume normalized = normalizeResume(request);
        long resumeId = repository.saveResume(
                principal.userId(),
                normalized.resumeName(),
                toJson(normalized.baseProfile()),
                toJson(normalized.educationExperience().isEmpty() ? normalized.education() : normalized.educationExperience()),
                toJson(normalized.workExperience().isEmpty() ? normalized.experience() : normalized.workExperience()),
                toJson(normalized.skills()));
        recordResumeFieldRead(principal, "SELF_WRITE");
        repository.markAiSuggestionApplied(principal.userId(), itemId);
        repository.incrementAiSuggestionApplied(item.taskId(), principal.userId());
        auditService.record(
                principal,
                "resume_ai_item",
                itemId,
                "resume_ai_suggestion_apply",
                toJson(Map.of("apply_status", item.applyStatus(), "target_field", item.targetField())),
                toJson(Map.of("apply_status", "applied", "target_field", item.targetField(), "resume_id", resumeId)));
        return new IdempotentActionResult<>(
                latestAiSuggestionResponse(principal.userId()),
                "candidate_resume_ai_suggestion_item",
                itemId);
    }

    private IdempotentActionResult<AiSuggestionTaskResponse> dismissAiSuggestionOnce(
            AuthzPrincipal principal,
            long itemId
    ) {
        dataScopeService.assertWritable(principal, "candidate_resume_ai_suggestion_item", ResourceOwner.candidate(principal.userId()));
        AiSuggestionItemRow item = repository.findAiSuggestionItem(principal.userId(), itemId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "ai suggestion not found"));
        if ("pending".equals(item.applyStatus())) {
            repository.markAiSuggestionDismissed(principal.userId(), itemId);
            repository.incrementAiSuggestionDismissed(item.taskId(), principal.userId());
            auditService.record(
                    principal,
                    "resume_ai_item",
                    itemId,
                    "resume_ai_suggestion_dismiss",
                    toJson(Map.of("apply_status", item.applyStatus(), "target_field", item.targetField())),
                    toJson(Map.of("apply_status", "dismissed", "target_field", item.targetField())));
        }
        return new IdempotentActionResult<>(
                latestAiSuggestionResponse(principal.userId()),
                "candidate_resume_ai_suggestion_item",
                itemId);
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

    private AiSuggestionTaskResponse latestAiSuggestionResponse(long candidateId) {
        return repository.latestAiSuggestionTask(candidateId)
                .map(task -> new AiSuggestionTaskResponse(
                        task.taskId(),
                        blankToDefault(task.taskStatus(), "generated"),
                        task.suggestionCount(),
                        task.appliedCount(),
                        task.dismissedCount(),
                        blankToDefault(task.generatedAt(), ""),
                        repository.listAiSuggestionItems(task.taskId(), candidateId).stream()
                                .map(this::toAiSuggestionItemResponse)
                                .toList()))
                .orElseGet(AiSuggestionTaskResponse::empty);
    }

    private AiSuggestionItemResponse toAiSuggestionItemResponse(AiSuggestionItemRow item) {
        return new AiSuggestionItemResponse(
                item.itemId(),
                blankToDefault(item.suggestionType(), "guidance"),
                blankToDefault(item.targetField(), ""),
                blankToDefault(item.title(), "简历优化建议"),
                blankToDefault(item.reasonSummary(), ""),
                blankToDefault(item.beforePreview(), ""),
                blankToDefault(item.suggestedValue(), ""),
                item.canApply(),
                blankToDefault(item.applyStatus(), "pending"));
    }

    private List<GeneratedSuggestion> buildAiSuggestions(ResumeResponse resume) {
        List<GeneratedSuggestion> suggestions = new java.util.ArrayList<>();
        CandidateCenterDtos.BaseProfileResponse profile = resume.baseProfile();
        String displayName = profile == null ? "" : profile.displayName();
        String category = profile == null ? "" : profile.categoryCode();
        String city = profile == null ? "" : profile.cityCode();
        Integer experienceYears = profile == null ? null : profile.experienceYears();
        List<String> expectedPositions = profile == null ? List.of() : safeList(profile.expectedPositions());
        List<String> expectedCities = profile == null ? List.of() : safeList(profile.expectedCities());
        List<String> skills = safeList(resume.skills());

        if (resume.selfDescription() == null || resume.selfDescription().trim().length() < 40) {
            String targetRole = firstPresent(expectedPositions.stream().findFirst().orElse(""), category, "目标岗位");
            String skillText = String.join("、", skills.stream().limit(3).toList());
            if (skillText.isBlank()) {
                skillText = "岗位相关技能";
            }
            String suggestion = "我具备" + (experienceYears == null ? "一定" : experienceYears + "年")
                    + targetRole + "相关经验，熟悉" + skillText
                    + "，希望在" + defaultText(city, "目标城市", 32)
                    + "持续深耕，重视稳定交付、协作沟通与持续学习。";
            suggestions.add(new GeneratedSuggestion(
                    "self_description_polish",
                    "self_description",
                    "补充结构化自我描述",
                    "当前自我描述为空或偏短，建议补充经验、技能、城市和职业态度。",
                    preview(resume.selfDescription()),
                    normalize(suggestion, 1000),
                    true));
        }

        if (skills.size() < 3) {
            List<String> derivedSkills = deriveSkills(expectedPositions, resume.workExperience(), resume.educationExperience(), category);
            if (!derivedSkills.isEmpty()) {
                suggestions.add(new GeneratedSuggestion(
                        "skill_append",
                        "skills",
                        "补充技能标签",
                        "当前技能标签较少，建议从求职意向、经历和专业中提取低风险技能标签。",
                        String.join("、", skills),
                        String.join(", ", derivedSkills),
                        true));
            }
        }

        if (expectedPositions.isEmpty()) {
            String position = derivePosition(resume.workExperience(), resume.educationExperience(), category);
            if (isPresent(position)) {
                suggestions.add(new GeneratedSuggestion(
                        "expected_position_append",
                        "base_profile.expected_positions",
                        "补充求职意向岗位",
                        "求职意向岗位为空，建议根据现有经历补一个候选方向。",
                        "",
                        position,
                        true));
            }
        }

        if (expectedCities.isEmpty() && isPresent(city)) {
            suggestions.add(new GeneratedSuggestion(
                    "expected_city_append",
                    "base_profile.expected_cities",
                    "补充期望城市",
                    "当前期望城市为空，建议先使用基本信息中的城市作为候选。",
                    "",
                    city,
                    true));
        }

        if (profile == null || !isPresent(profile.jobStatus())) {
            suggestions.add(new GeneratedSuggestion(
                    "job_status_fill",
                    "base_profile.job_status",
                    "补充求职状态",
                    "求职状态为空会影响后续求职者中心引导，建议先选择一个明确状态。",
                    "",
                    "正在看机会",
                    true));
        }

        if (resume.workExperience().stream().anyMatch(item -> !isPresent(item.responsibility())
                || item.responsibility().trim().length() < 20)) {
            suggestions.add(new GeneratedSuggestion(
                    "work_responsibility_guidance",
                    "work_experience.responsibility",
                    "完善工作职责描述",
                    "部分工作经历职责偏短，建议补充负责事项、使用工具、协作对象和结果。",
                    "职责描述偏短",
                    "建议按“负责事项 + 工具方法 + 业务结果”的结构补充，不自动改写。",
                    false));
        }

        if (resume.educationExperience().isEmpty() && resume.education().isEmpty()) {
            suggestions.add(new GeneratedSuggestion(
                    "education_guidance",
                    "education_experience",
                    "补充教育经历",
                    "教育经历缺失，建议补充学校、专业、学历和时间范围。",
                    "",
                    "请补充学校、专业、学历和起止时间。本条为指导建议，不自动写入。",
                    false));
        }

        if (suggestions.isEmpty()) {
            suggestions.add(new GeneratedSuggestion(
                    "resume_quality_guidance",
                    "resume_quality",
                    "简历结构已较完整",
                    "当前简历已具备基础结构，可继续按目标岗位补充项目结果和量化描述。",
                    defaultText(displayName, "当前简历", 64),
                    "建议继续补充可量化成果。本条为指导建议，不自动写入。",
                    false));
        }
        return suggestions.stream().limit(8).toList();
    }

    private ResumeSaveRequest applySuggestion(ResumeResponse resume, AiSuggestionItemRow item) {
        CandidateCenterDtos.BaseProfileResponse profile = resume.baseProfile();
        List<String> skills = new java.util.ArrayList<>(safeList(resume.skills()));
        List<String> expectedPositions = new java.util.ArrayList<>(profile == null ? List.of() : safeList(profile.expectedPositions()));
        List<String> expectedCities = new java.util.ArrayList<>(profile == null ? List.of() : safeList(profile.expectedCities()));
        String selfDescription = blankToDefault(resume.selfDescription(), "");
        String summary = profile == null ? "" : blankToDefault(profile.summary(), "");
        String jobStatus = profile == null ? "" : blankToDefault(profile.jobStatus(), "");

        switch (item.targetField()) {
            case "self_description" -> {
                selfDescription = normalize(item.suggestedValue(), 1000);
                summary = selfDescription;
            }
            case "base_profile.summary" -> summary = normalize(item.suggestedValue(), 500);
            case "skills" -> mergeValues(skills, item.suggestedValue(), 20, 80);
            case "base_profile.expected_positions" -> mergeValues(expectedPositions, item.suggestedValue(), 5, 80);
            case "base_profile.expected_cities" -> mergeValues(expectedCities, item.suggestedValue(), 5, 80);
            case "base_profile.job_status" -> jobStatus = normalize(item.suggestedValue(), 80);
            default -> throw validation("ai suggestion target field is not allowed");
        }

        CandidateCenterDtos.BaseProfileRequest baseProfile = new CandidateCenterDtos.BaseProfileRequest(
                profile == null ? "" : profile.displayName(),
                profile == null ? "" : profile.cityCode(),
                profile == null ? "" : profile.categoryCode(),
                profile == null ? null : profile.experienceYears(),
                summary,
                profile == null ? "" : profile.gender(),
                profile == null ? "" : profile.birthDate(),
                profile == null ? "" : profile.highestEducation(),
                profile == null ? "" : profile.startWorkDate(),
                profile != null && profile.noExperience(),
                profile == null ? "" : profile.contactPhone(),
                profile == null ? "" : profile.contactWechat(),
                profile != null && profile.wechatSameAsPhone(),
                expectedPositions,
                profile == null ? "" : profile.expectedSalary(),
                expectedCities,
                jobStatus);
        return new ResumeSaveRequest(
                resume.resumeName(),
                baseProfile,
                resume.education(),
                resume.experience(),
                skills,
                resume.workExperience().stream()
                        .map(itemValue -> new CandidateCenterDtos.WorkExperienceRequest(
                                itemValue.companyName(),
                                itemValue.positionName(),
                                itemValue.startDate(),
                                itemValue.endDate(),
                                itemValue.ongoing(),
                                itemValue.responsibility()))
                        .toList(),
                resume.educationExperience().stream()
                        .map(itemValue -> new CandidateCenterDtos.EducationExperienceRequest(
                                itemValue.schoolName(),
                                itemValue.majorName(),
                                itemValue.startDate(),
                                itemValue.endDate(),
                                itemValue.ongoing(),
                                itemValue.degree()))
                        .toList(),
                selfDescription);
    }

    private void mergeValues(List<String> target, String suggestedValue, int maxItems, int maxLength) {
        for (String value : suggestedValue.split("[,，、/\\n]")) {
            String normalized = normalize(value, maxLength);
            if (!normalized.isBlank() && !target.contains(normalized)) {
                target.add(normalized);
            }
            if (target.size() >= maxItems) {
                break;
            }
        }
    }

    private List<String> deriveSkills(
            List<String> expectedPositions,
            List<CandidateCenterDtos.WorkExperienceResponse> workExperience,
            List<CandidateCenterDtos.EducationExperienceResponse> educationExperience,
            String category
    ) {
        List<String> values = new java.util.ArrayList<>();
        values.addAll(expectedPositions);
        values.add(category);
        workExperience.forEach(item -> {
            values.add(item.positionName());
            values.add(item.responsibility());
        });
        educationExperience.forEach(item -> {
            values.add(item.majorName());
            values.add(item.degree());
        });
        return values.stream()
                .flatMap(value -> List.of(defaultText(value, "", 120).split("[,，、/\\s]+")).stream())
                .map(value -> normalize(value, 40))
                .filter(value -> value.length() >= 2)
                .filter(value -> !Set.of("负责", "工作", "岗位", "相关", "经验", "专业", "本科", "大专").contains(value))
                .distinct()
                .limit(5)
                .toList();
    }

    private String derivePosition(
            List<CandidateCenterDtos.WorkExperienceResponse> workExperience,
            List<CandidateCenterDtos.EducationExperienceResponse> educationExperience,
            String category
    ) {
        return firstPresent(
                workExperience.stream().map(CandidateCenterDtos.WorkExperienceResponse::positionName)
                        .filter(this::isPresent)
                        .findFirst()
                        .orElse(""),
                category,
                educationExperience.stream().map(CandidateCenterDtos.EducationExperienceResponse::majorName)
                        .filter(this::isPresent)
                        .findFirst()
                        .orElse(""));
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String preview(String value) {
        return isPresent(value) ? normalize(value, 180) : "当前为空";
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (isPresent(value)) {
                return normalize(value, 120);
            }
        }
        return "";
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

    private void requireAttachmentEnabled() {
        requireEnabled();
        if (!phase3Features.isResumeAttachmentUpload()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FEATURE_DISABLED_403", "resume attachment upload disabled");
        }
    }

    private void requireAiAssistEnabled() {
        requireEnabled();
        if (!phase3Features.isResumeAiAssist()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FEATURE_DISABLED_403", "resume ai assist disabled");
        }
    }

    private NormalizedAttachment normalizeAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw validation("resume attachment file is required");
        }
        if (file.getSize() > maxAttachmentSizeBytes) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "ATTACHMENT_SIZE_413", "resume attachment is too large");
        }
        String originalName = defaultText(file.getOriginalFilename(), "resume-attachment", 180);
        String extension = extension(originalName);
        String contentType = normalize(file.getContentType(), 120);
        if (!ALLOWED_ATTACHMENT_TYPES.contains(contentType) || !extensionAllowed(extension, contentType)) {
            throw validation("resume attachment must be PDF, DOC or DOCX");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (Exception exception) {
            throw validation("resume attachment file cannot be read");
        }
        if (content.length == 0) {
            throw validation("resume attachment file is empty");
        }
        return new NormalizedAttachment(
                safeFileName(originalName, extension),
                contentType,
                extension,
                content.length,
                sha256(content),
                content);
    }

    private AttachmentResponse toAttachmentResponse(AttachmentState state) {
        if (!isPresent(state.objectKey())) {
            return AttachmentResponse.empty();
        }
        return new AttachmentResponse(
                true,
                "uploaded",
                blankToDefault(state.fileName(), "resume-attachment"),
                blankToDefault(state.contentType(), "application/octet-stream"),
                state.sizeBytes(),
                blankToDefault(state.uploadedAt(), ""));
    }

    private String extension(String fileName) {
        int dotIndex = fileName == null ? -1 : fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw validation("resume attachment file extension is required");
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    private boolean extensionAllowed(String extension, String contentType) {
        return ("pdf".equals(extension) && "application/pdf".equals(contentType))
                || ("doc".equals(extension) && "application/msword".equals(contentType))
                || ("docx".equals(extension)
                && "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType));
    }

    private String safeFileName(String fileName, String extension) {
        String baseName = fileName == null ? "resume-attachment" : fileName.replace('\\', '/');
        int slashIndex = baseName.lastIndexOf('/');
        if (slashIndex >= 0) {
            baseName = baseName.substring(slashIndex + 1);
        }
        baseName = baseName.replaceAll("[^A-Za-z0-9._\\-\\u4e00-\\u9fa5]", "_");
        if (!baseName.toLowerCase().endsWith("." + extension)) {
            baseName = "resume-attachment." + extension;
        }
        if (baseName.length() > 120) {
            baseName = baseName.substring(0, 100) + "." + extension;
        }
        return baseName.isBlank() ? "resume-attachment." + extension : baseName;
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception exception) {
            throw new IllegalStateException("failed to hash resume attachment", exception);
        }
    }

    private String attachmentAuditJson(AttachmentState state, boolean hadAttachment) {
        return toJson(Map.of(
                "has_attachment", hadAttachment && isPresent(state.objectKey()),
                "resume_id", state.resumeId(),
                "content_type", blankToDefault(state.contentType(), ""),
                "size_bytes", state.sizeBytes() == null ? 0L : state.sizeBytes()));
    }

    private String attachmentAuditJson(NormalizedAttachment attachment, boolean hasAttachment) {
        return toJson(Map.of(
                "has_attachment", hasAttachment,
                "content_type", attachment.contentType(),
                "size_bytes", attachment.sizeBytes()));
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

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
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

    private record NormalizedAttachment(
            String fileName,
            String contentType,
            String extension,
            long sizeBytes,
            String sha256,
            byte[] content
    ) {

        Map<String, Object> fingerprint() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("file_name", fileName);
            payload.put("content_type", contentType);
            payload.put("extension", extension);
            payload.put("size_bytes", sizeBytes);
            payload.put("sha256", sha256);
            return payload;
        }
    }

    private record GeneratedSuggestion(
            String suggestionType,
            String targetField,
            String title,
            String reasonSummary,
            String beforePreview,
            String suggestedValue,
            boolean canApply
    ) {
    }

    private record OnboardingProgress(
            String status,
            String currentStep
    ) {
    }
}
