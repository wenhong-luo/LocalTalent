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
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.CompanyProfileResponse;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.CompanyProfileSaveRequest;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.FeatureResponse;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.InterviewSessionPageResponse;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.OverviewResponse;
import cn.localtalent.backend.company.workbench.infrastructure.CompanyWorkbenchJdbcRepository;
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
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final int MAX_PAGE_SIZE = 100;

    private final CompanyWorkbenchJdbcRepository repository;
    private final Phase3FeatureProperties phase3Features;
    private final IdempotencyService idempotencyService;
    private final DataScopeService dataScopeService;
    private final AuditService auditService;
    private final FieldAccessRecorder fieldAccessRecorder;
    private final JobService jobService;
    private final InterviewService interviewService;
    private final ExportService exportService;
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
            ExportService exportService
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
    }

    public OverviewResponse overview() {
        AuthzPrincipal principal = requireCompany();
        requireEnabled();
        return new OverviewResponse(
                profileWithoutFeatureGuard(principal),
                repository.stats(principal.companyId()),
                new FeatureResponse(true));
    }

    public CompanyProfileResponse profile() {
        AuthzPrincipal principal = requireCompany();
        requireEnabled();
        return profileWithoutFeatureGuard(principal);
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
                profile.companyProfile());
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
                normalizeNullable(request.companyProfile(), 2000));
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
            String companyProfile
    ) {
        Map<String, Object> fingerprint() {
            return Map.of(
                    "company_name", companyName,
                    "industry_code", industryCode == null ? "" : industryCode,
                    "nature_code", natureCode == null ? "" : natureCode,
                    "scale_code", scaleCode == null ? "" : scaleCode,
                    "city_code", cityCode == null ? "" : cityCode,
                    "address", address == null ? "" : address,
                    "company_profile", companyProfile == null ? "" : companyProfile);
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
            payload.put("certification_material_summary", materialSummary);
            return payload;
        }
    }

    private record NormalizedStage(String stage, String note) {
    }
}
