package cn.localtalent.backend.company.workbench.application;

import cn.localtalent.backend.application.service.ApplicationService;
import cn.localtalent.backend.audit.AuditService;
import cn.localtalent.backend.audit.FieldAccessRecorder;
import cn.localtalent.backend.auth.domain.IdentityType;
import cn.localtalent.backend.authz.AuthzContext;
import cn.localtalent.backend.authz.AuthzPrincipal;
import cn.localtalent.backend.authz.DataScopeService;
import cn.localtalent.backend.authz.ResourceOwner;
import cn.localtalent.backend.candidate.application.IdempotencyService;
import cn.localtalent.backend.candidate.domain.IdempotentActionResult;
import cn.localtalent.backend.common.exception.ApiException;
import cn.localtalent.backend.common.json.AuditJsonMapper;
import cn.localtalent.backend.company.application.CompanyService;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.ApplicationItemResponse;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.ApplicationPageResponse;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.ApplicationStageRequest;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.CertificationSubmitRequest;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.CompanyLogoDownload;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.CompanyLogoResponse;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.CompanyStyleImageDownload;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.CompanyStyleImageOrderRequest;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.CompanyStyleImagePageResponse;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.CompanyStyleImageResponse;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.CompanyProfileResponse;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.CompanyProfileSaveRequest;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.FeatureResponse;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.InterviewSessionPageResponse;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.OverviewResponse;
import cn.localtalent.backend.company.workbench.infrastructure.CompanyWorkbenchJdbcRepository;
import cn.localtalent.backend.company.workbench.infrastructure.CompanyWorkbenchJdbcRepository.LogoRow;
import cn.localtalent.backend.company.workbench.infrastructure.CompanyWorkbenchJdbcRepository.StyleImageRow;
import cn.localtalent.backend.company.workbench.infrastructure.CompanyStyleImageStorageService;
import cn.localtalent.backend.exporting.api.ExportApplyRequest;
import cn.localtalent.backend.exporting.api.ExportApplyResponse;
import cn.localtalent.backend.exporting.api.ExportScopeRequest;
import cn.localtalent.backend.exporting.service.ExportService;
import cn.localtalent.backend.interview.api.InterviewSessionCreateRequest;
import cn.localtalent.backend.interview.api.InterviewSessionResponse;
import cn.localtalent.backend.interview.service.InterviewService;
import cn.localtalent.backend.job.api.JobCreateRequest;
import cn.localtalent.backend.job.api.JobPageResponse;
import cn.localtalent.backend.job.api.JobResponse;
import cn.localtalent.backend.job.api.JobStatusRequest;
import cn.localtalent.backend.job.api.JobUpdateRequest;
import cn.localtalent.backend.job.application.JobService;
import cn.localtalent.backend.phase3.Phase3FeatureProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
public class CompanyWorkbenchService {

    private static final String PROFILE_SAVE_API = "company.workbench.profile.write";
    private static final String CERTIFICATION_SUBMIT_API = "company.workbench.certification.submit";
    private static final String JOB_CREATE_API = "company.workbench.job.create";
    private static final String JOB_UPDATE_API = "company.workbench.job.update";
    private static final String JOB_SUBMIT_REVIEW_API = "company.workbench.job.submit-review";
    private static final String JOB_OFFLINE_API = "company.workbench.job.offline";
    private static final String APPLICATION_STAGE_API = "company.workbench.application.stage";
    private static final String INTERVIEW_CREATE_API = "company.workbench.interview.create";
    private static final String EXPORT_APPLY_API = "company.workbench.export.apply";
    private static final String STYLE_IMAGE_UPLOAD_API = "company.workbench.style-image.upload";
    private static final String STYLE_IMAGE_DELETE_API = "company.workbench.style-image.delete";
    private static final String STYLE_IMAGE_ORDER_API = "company.workbench.style-image.order";
    private static final String LOGO_UPLOAD_API = "company.workbench.logo.upload";
    private static final String LOGO_DELETE_API = "company.workbench.logo.delete";

    private static final int MAX_PAGE_SIZE = 100;
    private static final int STYLE_IMAGE_LIMIT = 6;
    private static final Set<String> ALLOWED_STYLE_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final CompanyWorkbenchJdbcRepository repository;
    private final Phase3FeatureProperties phase3Features;
    private final IdempotencyService idempotencyService;
    private final DataScopeService dataScopeService;
    private final AuditService auditService;
    private final FieldAccessRecorder fieldAccessRecorder;
    private final JobService jobService;
    private final InterviewService interviewService;
    private final ExportService exportService;
    private final CompanyStyleImageStorageService styleImageStorageService;
    private final long maxStyleImageBytes;
    private final long maxLogoBytes;
    private final ObjectMapper objectMapper = AuditJsonMapper.create();

    public CompanyWorkbenchService(
            CompanyWorkbenchJdbcRepository repository,
            Phase3FeatureProperties phase3Features,
            IdempotencyService idempotencyService,
            DataScopeService dataScopeService,
            AuditService auditService,
            FieldAccessRecorder fieldAccessRecorder,
            JobService jobService,
            InterviewService interviewService,
            ExportService exportService,
            CompanyStyleImageStorageService styleImageStorageService,
            @Value("${localtalent.company-style-image.max-size-bytes:5242880}") long maxStyleImageBytes,
            @Value("${localtalent.company-logo.max-size-bytes:2097152}") long maxLogoBytes
    ) {
        this.repository = repository;
        this.phase3Features = phase3Features;
        this.idempotencyService = idempotencyService;
        this.dataScopeService = dataScopeService;
        this.auditService = auditService;
        this.fieldAccessRecorder = fieldAccessRecorder;
        this.jobService = jobService;
        this.interviewService = interviewService;
        this.exportService = exportService;
        this.styleImageStorageService = styleImageStorageService;
        this.maxStyleImageBytes = maxStyleImageBytes;
        this.maxLogoBytes = maxLogoBytes;
    }

    public OverviewResponse overview() {
        AuthzPrincipal principal = requireCompany();
        requireEnabled();
        return new OverviewResponse(
                profileWithoutFeatureGuard(principal),
                repository.stats(principal.companyId()),
                new FeatureResponse(true, phase3Features.isCompanyStyleUpload(), phase3Features.isCompanyLogoUpload()));
    }

    public CompanyProfileResponse profile() {
        AuthzPrincipal principal = requireCompany();
        requireEnabled();
        return profileWithoutFeatureGuard(principal);
    }

    public CompanyLogoResponse logo() {
        AuthzPrincipal principal = requireCompany();
        requireLogoUploadEnabled();
        dataScopeService.assertAccessible(principal, "company_logo_asset", ResourceOwner.company(principal.companyId()));
        return logoResponse(repository.findActiveLogo(principal.companyId()).orElse(null));
    }

    @Transactional
    public CompanyLogoResponse uploadLogo(MultipartFile file, String idempotencyKey) {
        AuthzPrincipal principal = requireCompany();
        requireLogoUploadEnabled();
        NormalizedStyleImage image = normalizeLogoImage(file);
        Map<String, Object> fingerprint = Map.of(
                "file_name", image.fileName(),
                "content_type", image.contentType(),
                "size_bytes", image.content().length,
                "sha256", image.sha256());
        return idempotencyService.execute(
                LOGO_UPLOAD_API,
                principal,
                idempotencyKey,
                fingerprint,
                CompanyLogoResponse.class,
                () -> uploadLogoOnce(principal, image));
    }

    public CompanyLogoDownload logoContent() {
        AuthzPrincipal principal = requireCompany();
        requireLogoUploadEnabled();
        LogoRow row = requireLogo(principal);
        fieldAccessRecorder.record(principal, "company_logo_asset", row.logoId(), "image_content", "COMPANY_LOGO_READ");
        return new CompanyLogoDownload(row.fileName(), row.contentType(), styleImageStorageService.get(row.objectKey()));
    }

    @Transactional
    public CompanyLogoResponse deleteLogo(String idempotencyKey) {
        AuthzPrincipal principal = requireCompany();
        requireLogoUploadEnabled();
        LogoRow row = repository.findActiveLogo(principal.companyId()).orElse(null);
        long logoId = row == null ? 0 : row.logoId();
        return idempotencyService.execute(
                LOGO_DELETE_API,
                principal,
                idempotencyKey,
                Map.of("logo_id", logoId, "action", "delete"),
                CompanyLogoResponse.class,
                () -> deleteLogoOnce(principal, row));
    }

    public CompanyStyleImagePageResponse styleImages() {
        AuthzPrincipal principal = requireCompany();
        requireStyleUploadEnabled();
        dataScopeService.assertAccessible(principal, "company_style_image", ResourceOwner.company(principal.companyId()));
        return styleImagePage(repository.listStyleImages(principal.companyId()));
    }

    @Transactional
    public CompanyStyleImageResponse uploadStyleImage(MultipartFile file, String idempotencyKey) {
        AuthzPrincipal principal = requireCompany();
        requireStyleUploadEnabled();
        NormalizedStyleImage image = normalizeStyleImage(file);
        Map<String, Object> fingerprint = Map.of(
                "file_name", image.fileName(),
                "content_type", image.contentType(),
                "size_bytes", image.content().length,
                "sha256", image.sha256());
        return idempotencyService.execute(
                STYLE_IMAGE_UPLOAD_API,
                principal,
                idempotencyKey,
                fingerprint,
                CompanyStyleImageResponse.class,
                () -> uploadStyleImageOnce(principal, image));
    }

    public CompanyStyleImageDownload styleImageContent(long imageId) {
        AuthzPrincipal principal = requireCompany();
        requireStyleUploadEnabled();
        StyleImageRow row = requireStyleImage(principal, imageId);
        fieldAccessRecorder.record(principal, "company_style_image", row.imageId(), "image_content", "COMPANY_STYLE_IMAGE_READ");
        return new CompanyStyleImageDownload(row.fileName(), row.contentType(), styleImageStorageService.get(row.objectKey()));
    }

    @Transactional
    public CompanyStyleImagePageResponse saveStyleImageOrder(CompanyStyleImageOrderRequest request, String idempotencyKey) {
        AuthzPrincipal principal = requireCompany();
        requireStyleUploadEnabled();
        List<Long> ids = normalizeStyleImageOrder(request);
        return idempotencyService.execute(
                STYLE_IMAGE_ORDER_API,
                principal,
                idempotencyKey,
                Map.of("image_ids", ids),
                CompanyStyleImagePageResponse.class,
                () -> saveStyleImageOrderOnce(principal, ids));
    }

    @Transactional
    public CompanyStyleImagePageResponse deleteStyleImage(long imageId, String idempotencyKey) {
        AuthzPrincipal principal = requireCompany();
        requireStyleUploadEnabled();
        long normalizedId = requirePositive(imageId, "image_id");
        return idempotencyService.execute(
                STYLE_IMAGE_DELETE_API,
                principal,
                idempotencyKey,
                Map.of("image_id", normalizedId, "action", "delete"),
                CompanyStyleImagePageResponse.class,
                () -> deleteStyleImageOnce(principal, normalizedId));
    }

    @Transactional
    public CompanyProfileResponse saveProfile(CompanyProfileSaveRequest request, String idempotencyKey) {
        AuthzPrincipal principal = requireCompany();
        requireEnabled();
        NormalizedProfile normalized = normalizeProfile(request);
        return idempotencyService.execute(
                PROFILE_SAVE_API,
                principal,
                idempotencyKey,
                normalized.fingerprint(),
                CompanyProfileResponse.class,
                () -> saveProfileOnce(principal, normalized));
    }

    @Transactional
    public CompanyProfileResponse submitCertification(CertificationSubmitRequest request, String idempotencyKey) {
        AuthzPrincipal principal = requireCompany();
        requireEnabled();
        NormalizedCertification normalized = normalizeCertification(request);
        return idempotencyService.execute(
                CERTIFICATION_SUBMIT_API,
                principal,
                idempotencyKey,
                normalized.fingerprint(),
                CompanyProfileResponse.class,
                () -> submitCertificationOnce(principal, normalized));
    }

    public JobPageResponse jobs(int page, int size) {
        requireEnabled();
        return jobService.listCompanyJobs(page, size);
    }

    @Transactional
    public JobResponse createJob(JobCreateRequest request, String idempotencyKey) {
        AuthzPrincipal principal = requireCompany();
        requireEnabled();
        return idempotencyService.execute(
                JOB_CREATE_API,
                principal,
                idempotencyKey,
                request,
                JobResponse.class,
                () -> {
                    JobResponse response = jobService.create(request);
                    return new IdempotentActionResult<>(response, "job_post", response.jobId());
                });
    }

    public JobResponse getJob(long jobId) {
        requireEnabled();
        return jobService.getCompanyJob(jobId);
    }

    @Transactional
    public JobResponse updateJob(long jobId, JobUpdateRequest request, String idempotencyKey) {
        AuthzPrincipal principal = requireCompany();
        requireEnabled();
        Map<String, Object> fingerprint = new LinkedHashMap<>();
        fingerprint.put("job_id", jobId);
        fingerprint.put("request", request);
        return idempotencyService.execute(
                JOB_UPDATE_API,
                principal,
                idempotencyKey,
                fingerprint,
                JobResponse.class,
                () -> {
                    JobResponse response = jobService.update(jobId, request);
                    return new IdempotentActionResult<>(response, "job_post", response.jobId());
                });
    }

    @Transactional
    public Object submitReview(long jobId, String idempotencyKey) {
        AuthzPrincipal principal = requireCompany();
        requireEnabled();
        if (repository.authStatus(principal.companyId()) != CompanyService.AUTH_APPROVED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTHZ_403", "company is not certified");
        }
        return idempotencyService.execute(
                JOB_SUBMIT_REVIEW_API,
                principal,
                idempotencyKey,
                Map.of("job_id", jobId, "action", "submit_review"),
                Object.class,
                () -> new IdempotentActionResult<>(
                        jobService.changeCompanyStatus(jobId, new JobStatusRequest("submit_review", null)),
                        "job_post",
                        jobId));
    }

    @Transactional
    public Object offlineJob(long jobId, JobStatusRequest request, String idempotencyKey) {
        AuthzPrincipal principal = requireCompany();
        requireEnabled();
        String reason = normalizeNullable(request == null ? null : request.reason(), 500);
        return idempotencyService.execute(
                JOB_OFFLINE_API,
                principal,
                idempotencyKey,
                Map.of("job_id", jobId, "action", "offline", "reason", reason == null ? "" : reason),
                Object.class,
                () -> new IdempotentActionResult<>(
                        jobService.changeCompanyStatus(jobId, new JobStatusRequest("offline", reason)),
                        "job_post",
                        jobId));
    }

    public ApplicationPageResponse applications(Long jobId, Integer status, int page, int size) {
        AuthzPrincipal principal = requireCompany();
        requireEnabled();
        dataScopeService.assertAccessible(principal, "job_application", ResourceOwner.company(principal.companyId()));
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = (normalizedPage - 1) * normalizedSize;
        return new ApplicationPageResponse(
                repository.listApplications(principal.companyId(), optionalPositive(jobId, "job_id"), normalizeStatus(status), normalizedSize, offset),
                repository.countApplications(principal.companyId(), optionalPositive(jobId, "job_id"), normalizeStatus(status)));
    }

    public ApplicationItemResponse applicationDetail(long applicationId) {
        AuthzPrincipal principal = requireCompany();
        requireEnabled();
        ApplicationItemResponse response = requireCompanyApplication(principal, applicationId);
        fieldAccessRecorder.record(principal, "job_application", applicationId, "candidate_summary", "COMPANY_APPLICATION_DETAIL");
        return response;
    }

    @Transactional
    public ApplicationItemResponse changeStage(long applicationId, ApplicationStageRequest request, String idempotencyKey) {
        AuthzPrincipal principal = requireCompany();
        requireEnabled();
        NormalizedStage normalized = normalizeStage(request);
        return idempotencyService.execute(
                APPLICATION_STAGE_API,
                principal,
                idempotencyKey,
                Map.of("application_id", applicationId, "stage", normalized.stage(), "note", normalized.note()),
                ApplicationItemResponse.class,
                () -> changeStageOnce(principal, applicationId, normalized));
    }

    public InterviewSessionPageResponse interviewSessions(int page, int size) {
        AuthzPrincipal principal = requireCompany();
        requireEnabled();
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = (normalizedPage - 1) * normalizedSize;
        dataScopeService.assertAccessible(principal, "interview_session", ResourceOwner.company(principal.companyId()));
        return new InterviewSessionPageResponse(
                repository.listInterviewSessions(principal.companyId(), normalizedSize, offset),
                repository.countInterviewSessions(principal.companyId()));
    }

    @Transactional
    public InterviewSessionResponse createInterview(long applicationId, InterviewSessionCreateRequest request, String idempotencyKey) {
        AuthzPrincipal principal = requireCompany();
        requireEnabled();
        requireCompanyApplication(principal, applicationId);
        return idempotencyService.execute(
                INTERVIEW_CREATE_API,
                principal,
                idempotencyKey,
                Map.of("application_id", applicationId, "request", request),
                InterviewSessionResponse.class,
                () -> {
                    InterviewSessionResponse response = interviewService.createSession(applicationId, request);
                    auditService.record(principal, "interview_session", response.sessionId(), "workbench_interview_invite", null,
                            "{\"channel\":\"site_message\"}");
                    return new IdempotentActionResult<>(response, "interview_session", response.sessionId());
                });
    }

    @Transactional
    public ExportApplyResponse applyExport(ExportApplyRequest request, String idempotencyKey) {
        AuthzPrincipal principal = requireCompany();
        requireEnabled();
        ExportApplyRequest normalized = new ExportApplyRequest(
                ExportService.BIZ_APPLICATION_CANDIDATE_DETAIL,
                request == null || request.scope() == null ? new ExportScopeRequest(null, null) : request.scope(),
                required(request == null ? null : request.reason(), "reason", 500));
        return idempotencyService.execute(
                EXPORT_APPLY_API,
                principal,
                idempotencyKey,
                normalized,
                ExportApplyResponse.class,
                () -> {
                    ExportApplyResponse response = exportService.apply(normalized);
                    return new IdempotentActionResult<>(response, "export_apply", response.exportId());
                });
    }

    public ExportApplyResponse exportDetail(long exportId) {
        requireCompany();
        requireEnabled();
        return exportService.companyDetail(exportId);
    }

    private IdempotentActionResult<CompanyStyleImageResponse> uploadStyleImageOnce(
            AuthzPrincipal principal,
            NormalizedStyleImage image
    ) {
        dataScopeService.assertWritable(principal, "company_style_image", ResourceOwner.company(principal.companyId()));
        if (repository.countActiveStyleImages(principal.companyId()) >= STYLE_IMAGE_LIMIT) {
            throw new ApiException(HttpStatus.CONFLICT, "STYLE_IMAGE_LIMIT_409", "company style image limit reached");
        }
        int displayOrder = repository.nextStyleImageOrder(principal.companyId());
        String objectKey = "company-style-images/%d/%s.%s".formatted(
                principal.companyId(),
                UUID.randomUUID(),
                image.extension());
        boolean stored = false;
        try {
            styleImageStorageService.put(objectKey, image.content(), image.contentType());
            stored = true;
            long imageId = repository.insertStyleImage(
                    principal.companyId(),
                    image.fileName(),
                    image.contentType(),
                    image.content().length,
                    image.sha256(),
                    objectKey,
                    displayOrder);
            CompanyStyleImageResponse response = requireStyleImage(principal, imageId)
                    .toResponse(styleImageContentUrl(imageId));
            auditService.record(principal, "company_style_image", imageId, "company_style_image_upload", null,
                    json(Map.of(
                            "file_name", image.fileName(),
                            "content_type", image.contentType(),
                            "size_bytes", image.content().length,
                            "display_order", displayOrder)));
            return new IdempotentActionResult<>(response, "company_style_image", imageId);
        } catch (RuntimeException exception) {
            if (stored) {
                styleImageStorageService.deleteQuietly(objectKey);
            }
            throw exception;
        }
    }

    private IdempotentActionResult<CompanyStyleImagePageResponse> saveStyleImageOrderOnce(
            AuthzPrincipal principal,
            List<Long> imageIds
    ) {
        dataScopeService.assertWritable(principal, "company_style_image", ResourceOwner.company(principal.companyId()));
        List<Long> activeIds = repository.listStyleImages(principal.companyId()).stream()
                .map(StyleImageRow::imageId)
                .toList();
        if (!activeIds.containsAll(imageIds) || imageIds.size() != activeIds.size()) {
            throw validation("image_ids must contain all active company style images");
        }
        for (int index = 0; index < imageIds.size(); index++) {
            repository.updateStyleImageOrder(principal.companyId(), imageIds.get(index), (index + 1) * 10);
        }
        auditService.record(principal, "company_style_image", principal.companyId(), "company_style_image_order",
                null,
                json(Map.of("image_ids", imageIds)));
        CompanyStyleImagePageResponse response = styleImagePage(repository.listStyleImages(principal.companyId()));
        return new IdempotentActionResult<>(response, "company", principal.companyId());
    }

    private IdempotentActionResult<CompanyStyleImagePageResponse> deleteStyleImageOnce(
            AuthzPrincipal principal,
            long imageId
    ) {
        dataScopeService.assertWritable(principal, "company_style_image", ResourceOwner.company(principal.companyId()));
        StyleImageRow row = repository.findStyleImage(principal.companyId(), imageId).orElse(null);
        if (row != null && repository.softDeleteStyleImage(principal.companyId(), imageId)) {
            styleImageStorageService.deleteQuietly(row.objectKey());
            auditService.record(principal, "company_style_image", imageId, "company_style_image_delete",
                    json(Map.of("status", row.status(), "content_type", row.contentType(), "size_bytes", row.sizeBytes())),
                    json(Map.of("status", 0)));
        }
        CompanyStyleImagePageResponse response = styleImagePage(repository.listStyleImages(principal.companyId()));
        return new IdempotentActionResult<>(response, "company_style_image", imageId);
    }

    private IdempotentActionResult<CompanyLogoResponse> uploadLogoOnce(
            AuthzPrincipal principal,
            NormalizedStyleImage image
    ) {
        dataScopeService.assertWritable(principal, "company_logo_asset", ResourceOwner.company(principal.companyId()));
        LogoRow previous = repository.findActiveLogo(principal.companyId()).orElse(null);
        String objectKey = "company-logo-assets/%d/%s.%s".formatted(
                principal.companyId(),
                UUID.randomUUID(),
                image.extension());
        boolean stored = false;
        try {
            styleImageStorageService.put(objectKey, image.content(), image.contentType());
            stored = true;
            long logoId = repository.insertLogo(
                    principal.companyId(),
                    image.fileName(),
                    image.contentType(),
                    image.content().length,
                    image.sha256(),
                    objectKey);
            if (previous != null && repository.softDeleteLogo(principal.companyId(), previous.logoId())) {
                styleImageStorageService.deleteQuietly(previous.objectKey());
            }
            CompanyLogoResponse response = logoResponse(requireLogo(principal));
            auditService.record(principal, "company_logo_asset", logoId, "company_logo_upload", null,
                    json(Map.of(
                            "file_name", image.fileName(),
                            "content_type", image.contentType(),
                            "size_bytes", image.content().length)));
            return new IdempotentActionResult<>(response, "company_logo_asset", logoId);
        } catch (RuntimeException exception) {
            if (stored) {
                styleImageStorageService.deleteQuietly(objectKey);
            }
            throw exception;
        }
    }

    private IdempotentActionResult<CompanyLogoResponse> deleteLogoOnce(AuthzPrincipal principal, LogoRow row) {
        dataScopeService.assertWritable(principal, "company_logo_asset", ResourceOwner.company(principal.companyId()));
        if (row != null && repository.softDeleteLogo(principal.companyId(), row.logoId())) {
            styleImageStorageService.deleteQuietly(row.objectKey());
            auditService.record(principal, "company_logo_asset", row.logoId(), "company_logo_delete",
                    json(Map.of("status", row.status(), "content_type", row.contentType(), "size_bytes", row.sizeBytes())),
                    json(Map.of("status", 0)));
            return new IdempotentActionResult<>(emptyLogoResponse(), "company_logo_asset", row.logoId());
        }
        return new IdempotentActionResult<>(emptyLogoResponse(), "company_logo_asset", 0);
    }

    private IdempotentActionResult<CompanyProfileResponse> saveProfileOnce(AuthzPrincipal principal, NormalizedProfile profile) {
        dataScopeService.assertWritable(principal, "company", ResourceOwner.company(principal.companyId()));
        CompanyProfileResponse before = profileWithoutFeatureGuard(principal);
        repository.updateProfile(
                principal.companyId(),
                profile.companyName(),
                profile.industryCode(),
                profile.natureCode(),
                profile.scaleCode(),
                profile.cityCode(),
                profile.address(),
                profile.companyProfile(),
                profile.registeredCapitalAmount(),
                profile.registeredCapitalUnit(),
                profile.websiteUrl(),
                json(profile.benefitCodes()),
                profile.contactName(),
                profile.contactMobile(),
                profile.contactMobileHidden(),
                profile.contactWechat(),
                profile.contactWechatSameMobile(),
                profile.contactPhone(),
                profile.contactEmail(),
                profile.contactQq());
        CompanyProfileResponse after = profileWithoutFeatureGuard(principal);
        auditService.record(principal, "company", principal.companyId(), "company_profile_save", safeProfileAudit(before), safeProfileAudit(after));
        return new IdempotentActionResult<>(after, "company", principal.companyId());
    }

    private IdempotentActionResult<CompanyProfileResponse> submitCertificationOnce(
            AuthzPrincipal principal,
            NormalizedCertification certification
    ) {
        dataScopeService.assertWritable(principal, "company", ResourceOwner.company(principal.companyId()));
        CompanyProfileResponse before = profileWithoutFeatureGuard(principal);
        repository.submitCertification(
                principal.companyId(),
                certification.companyName(),
                certification.licenseNo(),
                certification.industryCode(),
                certification.natureCode(),
                certification.scaleCode(),
                certification.cityCode(),
                certification.address(),
                certification.companyProfile(),
                certification.registeredCapitalAmount(),
                certification.registeredCapitalUnit(),
                certification.websiteUrl(),
                json(certification.benefitCodes()),
                certification.contactName(),
                certification.contactMobile(),
                certification.contactMobileHidden(),
                certification.contactWechat(),
                certification.contactWechatSameMobile(),
                certification.contactPhone(),
                certification.contactEmail(),
                certification.contactQq(),
                json(certification.materialSummary()));
        CompanyProfileResponse after = profileWithoutFeatureGuard(principal);
        auditService.record(principal, "company", principal.companyId(), "company_certification_submit",
                safeProfileAudit(before),
                json(Map.of("auth_status", after.authStatus(), "material_summary_keys", certification.materialSummary().keySet())));
        return new IdempotentActionResult<>(after, "company", principal.companyId());
    }

    private IdempotentActionResult<ApplicationItemResponse> changeStageOnce(
            AuthzPrincipal principal,
            long applicationId,
            NormalizedStage normalized
    ) {
        ApplicationItemResponse before = requireCompanyApplication(principal, applicationId);
        int status = stageStatus(normalized.stage());
        if (!repository.updateStage(principal.companyId(), applicationId, status, normalized.note(), principal.userId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "application not found");
        }
        ApplicationItemResponse after = requireCompanyApplication(principal, applicationId);
        auditService.record(principal, "job_application", applicationId, "application_stage_change",
                json(Map.of("application_status", before.applicationStatus())),
                json(Map.of("application_status", after.applicationStatus(), "stage", normalized.stage())));
        return new IdempotentActionResult<>(after, "job_application", applicationId);
    }

    private CompanyProfileResponse profileWithoutFeatureGuard(AuthzPrincipal principal) {
        dataScopeService.assertAccessible(principal, "company", ResourceOwner.company(principal.companyId()));
        return repository.findProfile(principal.companyId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "company not found"));
    }

    private ApplicationItemResponse requireCompanyApplication(AuthzPrincipal principal, long applicationId) {
        return repository.findApplication(principal.companyId(), requirePositive(applicationId, "application_id"))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "application not found"));
    }

    private AuthzPrincipal requireCompany() {
        AuthzPrincipal principal = AuthzContext.requireCurrentPrincipal();
        if (principal.identityType() != IdentityType.COMPANY || principal.companyId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTHZ_403", "company identity required");
        }
        return principal;
    }

    private void requireEnabled() {
        if (!phase3Features.isCompanyWorkbench()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FEATURE_DISABLED_403", "company workbench disabled");
        }
    }

    private void requireStyleUploadEnabled() {
        requireEnabled();
        if (!phase3Features.isCompanyStyleUpload()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FEATURE_DISABLED_403", "company style upload disabled");
        }
    }

    private void requireLogoUploadEnabled() {
        requireEnabled();
        if (!phase3Features.isCompanyLogoUpload()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FEATURE_DISABLED_403", "company logo upload disabled");
        }
    }

    private StyleImageRow requireStyleImage(AuthzPrincipal principal, long imageId) {
        return repository.findStyleImage(principal.companyId(), requirePositive(imageId, "image_id"))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "company style image not found"));
    }

    private LogoRow requireLogo(AuthzPrincipal principal) {
        return repository.findActiveLogo(principal.companyId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "company logo not found"));
    }

    private CompanyStyleImagePageResponse styleImagePage(List<StyleImageRow> rows) {
        List<CompanyStyleImageResponse> responses = rows.stream()
                .map(row -> row.toResponse(styleImageContentUrl(row.imageId())))
                .toList();
        return new CompanyStyleImagePageResponse(responses, responses.size());
    }

    private String styleImageContentUrl(long imageId) {
        return "/api/company/workbench/style-images/%d/content".formatted(imageId);
    }

    private CompanyLogoResponse logoResponse(LogoRow row) {
        return row == null ? emptyLogoResponse() : row.toResponse("/api/company/workbench/logo/content");
    }

    private CompanyLogoResponse emptyLogoResponse() {
        return new CompanyLogoResponse(false, "empty", null, null, null, null, null);
    }

    private NormalizedProfile normalizeProfile(CompanyProfileSaveRequest request) {
        if (request == null) {
            throw validation("request body is required");
        }
        return new NormalizedProfile(
                required(request.companyName(), "company_name", 200),
                normalizeNullable(request.industryCode(), 64),
                normalizeNullable(request.natureCode(), 64),
                normalizeNullable(request.scaleCode(), 64),
                normalizeNullable(request.cityCode(), 32),
                normalizeNullable(request.address(), 255),
                normalizeNullable(request.companyProfile(), 2000),
                normalizeNullable(request.registeredCapitalAmount(), 32),
                normalizeNullable(request.registeredCapitalUnit(), 16),
                normalizeNullable(request.websiteUrl(), 255),
                normalizeBenefitCodes(request.benefitCodes()),
                normalizeNullable(request.contactName(), 100),
                normalizeNullable(request.contactMobile(), 32),
                request.contactMobileHidden() == null || request.contactMobileHidden(),
                normalizeNullable(request.contactWechat(), 64),
                request.contactWechatSameMobile() != null && request.contactWechatSameMobile(),
                normalizeNullable(request.contactPhone(), 64),
                normalizeNullable(request.contactEmail(), 128),
                normalizeNullable(request.contactQq(), 32));
    }

    private NormalizedCertification normalizeCertification(CertificationSubmitRequest request) {
        if (request == null) {
            throw validation("request body is required");
        }
        return new NormalizedCertification(
                required(request.companyName(), "company_name", 200),
                required(request.licenseNo(), "license_no", 64),
                normalizeNullable(request.industryCode(), 64),
                normalizeNullable(request.natureCode(), 64),
                normalizeNullable(request.scaleCode(), 64),
                normalizeNullable(request.cityCode(), 32),
                normalizeNullable(request.address(), 255),
                normalizeNullable(request.companyProfile(), 2000),
                normalizeNullable(request.registeredCapitalAmount(), 32),
                normalizeNullable(request.registeredCapitalUnit(), 16),
                normalizeNullable(request.websiteUrl(), 255),
                normalizeBenefitCodes(request.benefitCodes()),
                normalizeNullable(request.contactName(), 100),
                normalizeNullable(request.contactMobile(), 32),
                request.contactMobileHidden() == null || request.contactMobileHidden(),
                normalizeNullable(request.contactWechat(), 64),
                request.contactWechatSameMobile() != null && request.contactWechatSameMobile(),
                normalizeNullable(request.contactPhone(), 64),
                normalizeNullable(request.contactEmail(), 128),
                normalizeNullable(request.contactQq(), 32),
                materialSummary(request.certificationMaterialSummary()));
    }

    private NormalizedStage normalizeStage(ApplicationStageRequest request) {
        if (request == null) {
            throw validation("request body is required");
        }
        String stage = required(request.stage(), "stage", 32);
        stageStatus(stage);
        return new NormalizedStage(stage, normalizeNullable(request.note(), 500));
    }

    private NormalizedStyleImage normalizeStyleImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw validation("file is required");
        }
        if (file.getSize() > maxStyleImageBytes) {
            throw validation("file exceeds max size");
        }
        String originalName = safeFileName(file.getOriginalFilename(), "company-style-image");
        String extension = extension(originalName);
        String contentType = normalizeNullable(file.getContentType(), 120);
        if (contentType == null || !ALLOWED_STYLE_IMAGE_TYPES.contains(contentType) || !extensionAllowed(contentType, extension)) {
            throw validation("file type must be JPEG, PNG or WebP");
        }
        try {
            byte[] content = file.getBytes();
            if (content.length == 0 || content.length > maxStyleImageBytes) {
                throw validation("file exceeds max size");
            }
            return new NormalizedStyleImage(originalName, contentType, extension, content, sha256(content));
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw validation("failed to read file");
        }
    }

    private NormalizedStyleImage normalizeLogoImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw validation("file is required");
        }
        if (file.getSize() > maxLogoBytes) {
            throw validation("file exceeds max size");
        }
        String originalName = safeFileName(file.getOriginalFilename(), "company-logo");
        String extension = extension(originalName);
        String contentType = normalizeNullable(file.getContentType(), 120);
        if (contentType == null || !ALLOWED_STYLE_IMAGE_TYPES.contains(contentType) || !extensionAllowed(contentType, extension)) {
            throw validation("file type must be JPEG, PNG or WebP");
        }
        try {
            byte[] content = file.getBytes();
            if (content.length == 0 || content.length > maxLogoBytes) {
                throw validation("file exceeds max size");
            }
            return new NormalizedStyleImage(originalName, contentType, extension, content, sha256(content));
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw validation("failed to read file");
        }
    }

    private List<Long> normalizeStyleImageOrder(CompanyStyleImageOrderRequest request) {
        if (request == null || request.imageIds() == null || request.imageIds().isEmpty()) {
            throw validation("image_ids is required");
        }
        List<Long> imageIds = request.imageIds().stream()
                .map(id -> requirePositive(id == null ? 0 : id, "image_id"))
                .distinct()
                .toList();
        if (imageIds.size() > STYLE_IMAGE_LIMIT) {
            throw validation("image_ids exceeds max size");
        }
        return imageIds;
    }

    private boolean extensionAllowed(String contentType, String extension) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg".equals(extension) || "jpeg".equals(extension);
            case "image/png" -> "png".equals(extension);
            case "image/webp" -> "webp".equals(extension);
            default -> false;
        };
    }

    private String extension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            throw validation("file extension is required");
        }
        return fileName.substring(index + 1).toLowerCase(java.util.Locale.ROOT);
    }

    private String safeFileName(String raw, String fallback) {
        String fileName = normalizeNullable(raw, 180);
        if (fileName == null) {
            return fallback;
        }
        String normalized = fileName.replace("\\", "/");
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0) {
            normalized = normalized.substring(slash + 1);
        }
        normalized = normalized.replaceAll("[\\r\\n\\t]", "_");
        if (normalized.isBlank() || normalized.contains("..")) {
            return fallback;
        }
        return normalized;
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception exception) {
            throw new IllegalStateException("failed to calculate image sha256", exception);
        }
    }

    private int stageStatus(String stage) {
        return switch (stage) {
            case "viewed" -> 1;
            case "interview_invited" -> 2;
            case "archived" -> 4;
            case "rejected" -> 5;
            default -> throw validation("stage must be viewed, interview_invited, rejected or archived");
        };
    }

    private Map<String, Object> materialSummary(Map<String, Object> raw) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (raw == null) {
            return summary;
        }
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            String key = normalizeNullable(entry.getKey(), 64);
            if (key == null || key.contains("object_key") || key.contains("attachment")) {
                continue;
            }
            Object value = entry.getValue();
            summary.put(key, value == null ? "" : normalizeNullable(String.valueOf(value), 200));
            if (summary.size() >= 12) {
                break;
            }
        }
        return summary;
    }

    private Integer normalizeStatus(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < 0 || value > 5) {
            throw validation("status must be between 0 and 5");
        }
        return value;
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

    private List<String> normalizeBenefitCodes(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String item = normalizeNullable(value, 64);
            if (item != null) {
                normalized.add(item);
            }
            if (normalized.size() >= 20) {
                break;
            }
        }
        return List.copyOf(normalized);
    }

    private String safeProfileAudit(CompanyProfileResponse response) {
        if (response == null) {
            return null;
        }
        return json(Map.of(
                "company_id", response.companyId(),
                "company_name", response.companyName(),
                "auth_status", response.authStatus()));
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

    private record NormalizedProfile(
            String companyName,
            String industryCode,
            String natureCode,
            String scaleCode,
            String cityCode,
            String address,
            String companyProfile,
            String registeredCapitalAmount,
            String registeredCapitalUnit,
            String websiteUrl,
            List<String> benefitCodes,
            String contactName,
            String contactMobile,
            boolean contactMobileHidden,
            String contactWechat,
            boolean contactWechatSameMobile,
            String contactPhone,
            String contactEmail,
            String contactQq
    ) {
        Map<String, Object> fingerprint() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("company_name", companyName);
            payload.put("industry_code", industryCode);
            payload.put("nature_code", natureCode);
            payload.put("scale_code", scaleCode);
            payload.put("city_code", cityCode);
            payload.put("address", address);
            payload.put("company_profile", companyProfile);
            payload.put("registered_capital_amount", registeredCapitalAmount);
            payload.put("registered_capital_unit", registeredCapitalUnit);
            payload.put("website_url", websiteUrl);
            payload.put("benefit_codes", benefitCodes);
            payload.put("contact_name", contactName);
            payload.put("contact_mobile_present", contactMobile != null);
            payload.put("contact_mobile_hidden", contactMobileHidden);
            payload.put("contact_wechat_present", contactWechat != null);
            payload.put("contact_wechat_same_mobile", contactWechatSameMobile);
            payload.put("contact_phone_present", contactPhone != null);
            payload.put("contact_email_present", contactEmail != null);
            payload.put("contact_qq_present", contactQq != null);
            return payload;
        }
    }

    private record NormalizedCertification(
            String companyName,
            String licenseNo,
            String industryCode,
            String natureCode,
            String scaleCode,
            String cityCode,
            String address,
            String companyProfile,
            String registeredCapitalAmount,
            String registeredCapitalUnit,
            String websiteUrl,
            List<String> benefitCodes,
            String contactName,
            String contactMobile,
            boolean contactMobileHidden,
            String contactWechat,
            boolean contactWechatSameMobile,
            String contactPhone,
            String contactEmail,
            String contactQq,
            Map<String, Object> materialSummary
    ) {
        Map<String, Object> fingerprint() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("company_name", companyName);
            payload.put("license_no", licenseNo);
            payload.put("industry_code", industryCode);
            payload.put("nature_code", natureCode);
            payload.put("scale_code", scaleCode);
            payload.put("city_code", cityCode);
            payload.put("address", address);
            payload.put("company_profile", companyProfile);
            payload.put("registered_capital_amount", registeredCapitalAmount);
            payload.put("registered_capital_unit", registeredCapitalUnit);
            payload.put("website_url", websiteUrl);
            payload.put("benefit_codes", benefitCodes);
            payload.put("contact_name", contactName);
            payload.put("contact_mobile_present", contactMobile != null);
            payload.put("contact_mobile_hidden", contactMobileHidden);
            payload.put("contact_wechat_present", contactWechat != null);
            payload.put("contact_wechat_same_mobile", contactWechatSameMobile);
            payload.put("contact_phone_present", contactPhone != null);
            payload.put("contact_email_present", contactEmail != null);
            payload.put("contact_qq_present", contactQq != null);
            payload.put("certification_material_summary", materialSummary);
            return payload;
        }
    }

    private record NormalizedStage(String stage, String note) {
    }

    private record NormalizedStyleImage(
            String fileName,
            String contentType,
            String extension,
            byte[] content,
            String sha256
    ) {
    }
}
