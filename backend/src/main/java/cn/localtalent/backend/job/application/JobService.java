package cn.localtalent.backend.job.application;

import cn.localtalent.backend.audit.AuditService;
import cn.localtalent.backend.auth.domain.IdentityType;
import cn.localtalent.backend.authz.AuthzContext;
import cn.localtalent.backend.authz.AuthzPrincipal;
import cn.localtalent.backend.authz.DataScopeService;
import cn.localtalent.backend.authz.ResourceOwner;
import cn.localtalent.backend.common.exception.ApiException;
import cn.localtalent.backend.common.json.AuditJsonMapper;
import cn.localtalent.backend.company.application.CompanyService;
import cn.localtalent.backend.job.api.JobCreateRequest;
import cn.localtalent.backend.job.api.JobPageResponse;
import cn.localtalent.backend.job.api.JobResponse;
import cn.localtalent.backend.job.api.JobReviewRequest;
import cn.localtalent.backend.job.api.JobReviewResultResponse;
import cn.localtalent.backend.job.api.JobStatusRequest;
import cn.localtalent.backend.job.api.JobUpdateRequest;
import cn.localtalent.backend.job.domain.JobPostRow;
import cn.localtalent.backend.job.infrastructure.JobJdbcRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobService {

    public static final int JOB_PENDING = 1;
    public static final int JOB_ONLINE = 2;
    public static final int JOB_OFFLINE = 3;

    public static final int AUDIT_PENDING = 1;
    public static final int AUDIT_APPROVED = 2;
    public static final int AUDIT_REJECTED = 3;

    private static final int MAX_PAGE_SIZE = 100;

    private final JobJdbcRepository jobRepository;
    private final DataScopeService dataScopeService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper = AuditJsonMapper.create();

    public JobService(
            JobJdbcRepository jobRepository,
            DataScopeService dataScopeService,
            AuditService auditService
    ) {
        this.jobRepository = jobRepository;
        this.dataScopeService = dataScopeService;
        this.auditService = auditService;
    }

    public JobPageResponse listCompanyJobs(int page, int size) {
        AuthzPrincipal principal = requireCompanyPrincipal();
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = (normalizedPage - 1) * normalizedSize;
        List<JobResponse> items = jobRepository
                .listByCompany(principal.companyId(), normalizedSize, offset)
                .stream()
                .map(this::response)
                .toList();
        return new JobPageResponse(items, jobRepository.countByCompany(principal.companyId()));
    }

    public JobPageResponse listDeletedCompanyJobs(int page, int size) {
        AuthzPrincipal principal = requireCompanyPrincipal();
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = (normalizedPage - 1) * normalizedSize;
        List<JobResponse> items = jobRepository
                .listDeletedByCompany(principal.companyId(), normalizedSize, offset)
                .stream()
                .map(this::response)
                .toList();
        return new JobPageResponse(items, jobRepository.countDeletedByCompany(principal.companyId()));
    }

    @Transactional
    public JobResponse create(JobCreateRequest request) {
        AuthzPrincipal principal = requireCompanyPrincipal();
        JobCreateRequest normalized = normalizeCreate(request);
        long jobId = jobRepository.create(principal.companyId(), normalized);
        JobPostRow after = requireJob(jobId);
        auditService.record(principal, "job_post", jobId, "job_create", null, json(jobAuditSnapshot(after)));
        return response(after);
    }

    public JobResponse getCompanyJob(long jobId) {
        AuthzPrincipal principal = requireCompanyPrincipal();
        JobPostRow row = requireActiveJob(jobId);
        dataScopeService.assertAccessible(principal, "job_post", ResourceOwner.company(row.companyId()));
        return response(row);
    }

    @Transactional
    public JobResponse update(long jobId, JobUpdateRequest request) {
        AuthzPrincipal principal = requireCompanyPrincipal();
        JobPostRow before = requireActiveCompanyOwnedJob(principal, jobId);
        JobUpdateRequest normalized = normalizeUpdate(request);
        jobRepository.update(jobId, normalized);
        JobPostRow after = requireActiveJob(jobId);
        auditService.record(principal, "job_post", jobId, "job_update",
                json(jobAuditSnapshot(before)),
                json(jobAuditSnapshot(after)));
        return response(after);
    }

    @Transactional
    public JobReviewResultResponse changeCompanyStatus(long jobId, JobStatusRequest request) {
        AuthzPrincipal principal = requireCompanyPrincipal();
        JobPostRow before = requireActiveCompanyOwnedJob(principal, jobId);
        String action = request == null ? null : normalizeNullable(request.action(), 32);
        String reason = request == null ? null : normalizeNullable(request.reason(), 500);
        if ("submit_review".equals(action)) {
            jobRepository.submitReview(jobId);
            JobPostRow after = requireActiveJob(jobId);
            auditService.record(principal, "job_post", jobId, "job_submit_review",
                    json(jobAuditSnapshot(before)),
                    json(jobAuditSnapshot(after)));
            return reviewResult(after);
        }
        if ("offline".equals(action)) {
            jobRepository.offline(jobId, reason);
            JobPostRow after = requireActiveJob(jobId);
            auditService.record(principal, "job_post", jobId, "job_offline",
                    json(jobAuditSnapshot(before)),
                    json(jobAuditSnapshot(after)));
            return reviewResult(after);
        }
        throw validation("action must be submit_review or offline");
    }

    @Transactional
    public JobReviewResultResponse deleteCompanyJob(long jobId, String reason) {
        AuthzPrincipal principal = requireCompanyPrincipal();
        JobPostRow before = requireJob(jobId);
        dataScopeService.assertWritable(principal, "job_post", ResourceOwner.company(before.companyId()));
        if (before.deleted()) {
            return reviewResult(before);
        }
        String normalizedReason = normalizeNullable(reason, 500);
        jobRepository.softDelete(jobId, principal.userId(), normalizedReason);
        JobPostRow after = requireJob(jobId);
        auditService.record(principal, "job_post", jobId, "job_delete",
                json(jobAuditSnapshot(before)),
                json(jobAuditSnapshot(after)));
        return reviewResult(after);
    }

    @Transactional
    public JobResponse restoreCompanyJobDraft(long jobId, String reason) {
        AuthzPrincipal principal = requireCompanyPrincipal();
        JobPostRow before = requireJob(jobId);
        dataScopeService.assertWritable(principal, "job_post", ResourceOwner.company(before.companyId()));
        if (!before.deleted()) {
            throw validation("job is not deleted");
        }
        jobRepository.restoreDraft(jobId);
        JobPostRow after = requireActiveJob(jobId);
        auditService.record(principal, "job_post", jobId, "job_restore_draft",
                json(restoreAuditSnapshot(before, reason)),
                json(jobAuditSnapshot(after)));
        return response(after);
    }

    public JobPageResponse listForReview(Integer auditStatus, int page, int size) {
        requireAdminReader();
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = (normalizedPage - 1) * normalizedSize;
        List<JobResponse> items = jobRepository
                .listForReview(auditStatus, normalizedSize, offset)
                .stream()
                .map(this::response)
                .toList();
        return new JobPageResponse(items, jobRepository.countForReview(auditStatus));
    }

    public JobResponse getForReview(long jobId) {
        requireAdminReader();
        return response(requireActiveJob(jobId));
    }

    @Transactional
    public JobReviewResultResponse review(JobReviewRequest request) {
        AuthzPrincipal principal = requireOperator();
        long jobId = requirePositive(request == null ? null : request.jobId(), "job_id");
        int auditStatus = requireReviewStatus(request.auditStatus());
        String memo = normalizeNullable(request.memo(), 500);
        if (auditStatus == AUDIT_REJECTED && memo == null) {
            throw validation("memo is required when rejecting job");
        }

        JobPostRow before = requireActiveJob(jobId);
        if (auditStatus == AUDIT_APPROVED && before.companyAuthStatus() != CompanyService.AUTH_APPROVED) {
            throw validation("company is not certified");
        }

        int nextStatus = auditStatus == AUDIT_APPROVED ? JOB_ONLINE : JOB_OFFLINE;
        jobRepository.review(
                jobId,
                auditStatus,
                nextStatus,
                memo,
                auditStatus == AUDIT_REJECTED ? memo : null,
                principal.userId());
        JobPostRow after = requireActiveJob(jobId);
        auditService.record(
                principal,
                "job_post",
                jobId,
                auditStatus == AUDIT_APPROVED ? "job_review_pass" : "job_review_reject",
                json(jobAuditSnapshot(before)),
                json(jobAuditSnapshot(after)));
        return reviewResult(after);
    }

    private JobPostRow requireCompanyOwnedJob(AuthzPrincipal principal, long jobId) {
        JobPostRow row = requireJob(jobId);
        dataScopeService.assertWritable(principal, "job_post", ResourceOwner.company(row.companyId()));
        return row;
    }

    private JobPostRow requireActiveCompanyOwnedJob(AuthzPrincipal principal, long jobId) {
        JobPostRow row = requireActiveJob(jobId);
        dataScopeService.assertWritable(principal, "job_post", ResourceOwner.company(row.companyId()));
        return row;
    }

    private JobPostRow requireJob(long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "job not found"));
    }

    private JobPostRow requireActiveJob(long jobId) {
        JobPostRow row = requireJob(jobId);
        if (row.deleted()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "job not found");
        }
        return row;
    }

    private AuthzPrincipal requireCompanyPrincipal() {
        AuthzPrincipal principal = AuthzContext.requireCurrentPrincipal();
        if (principal.identityType() != IdentityType.COMPANY || principal.companyId() == null) {
            throw forbidden("company identity required");
        }
        return principal;
    }

    private AuthzPrincipal requireOperator() {
        AuthzPrincipal principal = AuthzContext.requireCurrentPrincipal();
        if (!principal.hasRole("operator")) {
            throw forbidden("operator role required");
        }
        return principal;
    }

    private void requireAdminReader() {
        AuthzPrincipal principal = AuthzContext.requireCurrentPrincipal();
        if (!principal.hasRole("operator") && !principal.hasRole("auditor")) {
            throw forbidden("admin review reader role required");
        }
    }

    private JobCreateRequest normalizeCreate(JobCreateRequest request) {
        if (request == null) {
            throw validation("request body is required");
        }
        boolean salaryNegotiable = Boolean.TRUE.equals(request.salaryNegotiable());
        Integer salaryMin = salaryNegotiable ? null : normalizedMoney(request.salaryMin(), "salary_min");
        Integer salaryMax = salaryNegotiable ? null : normalizedMoney(request.salaryMax(), "salary_max");
        assertOrderedRange(salaryMin, salaryMax, "salary range");
        boolean ageUnlimited = Boolean.TRUE.equals(request.ageUnlimited());
        Integer ageMin = ageUnlimited ? null : normalizedRangeValue(request.ageMin(), "age_min", 16, 80);
        Integer ageMax = ageUnlimited ? null : normalizedRangeValue(request.ageMax(), "age_max", 16, 80);
        assertOrderedRange(ageMin, ageMax, "age range");
        return new JobCreateRequest(
                required(request.title(), "title", 150),
                normalizeNullable(request.jobNatureCode(), 32),
                normalizeNullable(request.categoryCode(), 64),
                normalizeNullable(request.categoryName(), 100),
                normalizeNullable(request.experienceCode(), 32),
                normalizeNullable(request.educationCode(), 32),
                normalizedRangeValue(request.recruitCount(), "recruit_count", 1, 9999),
                normalizeNullable(request.cityCode(), 32),
                normalizeNullable(request.workRegionPath(), 160),
                normalizeNullable(request.address(), 255),
                salaryMin,
                salaryMax,
                salaryNegotiable,
                normalizeStringList(request.welfareCodes(), 20, 64),
                normalizeNullable(request.departmentName(), 100),
                ageMin,
                ageMax,
                ageUnlimited,
                normalizeNullable(request.recruitmentTimeCode(), 32),
                normalizeNullable(request.contactMode(), 32),
                normalizeNullable(request.contactName(), 80),
                normalizeNullable(request.contactMobile(), 32),
                normalizeNullable(request.contactPhone(), 64),
                normalizeNullable(request.contactEmail(), 120),
                normalizeNullable(request.contactWechat(), 64),
                !Boolean.FALSE.equals(request.contactHidden()),
                Boolean.TRUE.equals(request.notifyEnabled()),
                Boolean.TRUE.equals(request.resumeSubscriptionEnabled()),
                required(request.jobDesc(), "job_desc", 5000));
    }

    private JobUpdateRequest normalizeUpdate(JobUpdateRequest request) {
        if (request == null) {
            throw validation("request body is required");
        }
        boolean salaryNegotiable = Boolean.TRUE.equals(request.salaryNegotiable());
        Integer salaryMin = salaryNegotiable ? null : normalizedMoney(request.salaryMin(), "salary_min");
        Integer salaryMax = salaryNegotiable ? null : normalizedMoney(request.salaryMax(), "salary_max");
        assertOrderedRange(salaryMin, salaryMax, "salary range");
        boolean ageUnlimited = Boolean.TRUE.equals(request.ageUnlimited());
        Integer ageMin = ageUnlimited ? null : normalizedRangeValue(request.ageMin(), "age_min", 16, 80);
        Integer ageMax = ageUnlimited ? null : normalizedRangeValue(request.ageMax(), "age_max", 16, 80);
        assertOrderedRange(ageMin, ageMax, "age range");
        return new JobUpdateRequest(
                required(request.title(), "title", 150),
                normalizeNullable(request.jobNatureCode(), 32),
                normalizeNullable(request.categoryCode(), 64),
                normalizeNullable(request.categoryName(), 100),
                normalizeNullable(request.experienceCode(), 32),
                normalizeNullable(request.educationCode(), 32),
                normalizedRangeValue(request.recruitCount(), "recruit_count", 1, 9999),
                normalizeNullable(request.cityCode(), 32),
                normalizeNullable(request.workRegionPath(), 160),
                normalizeNullable(request.address(), 255),
                salaryMin,
                salaryMax,
                salaryNegotiable,
                normalizeStringList(request.welfareCodes(), 20, 64),
                normalizeNullable(request.departmentName(), 100),
                ageMin,
                ageMax,
                ageUnlimited,
                normalizeNullable(request.recruitmentTimeCode(), 32),
                normalizeNullable(request.contactMode(), 32),
                normalizeNullable(request.contactName(), 80),
                normalizeNullable(request.contactMobile(), 32),
                normalizeNullable(request.contactPhone(), 64),
                normalizeNullable(request.contactEmail(), 120),
                normalizeNullable(request.contactWechat(), 64),
                !Boolean.FALSE.equals(request.contactHidden()),
                Boolean.TRUE.equals(request.notifyEnabled()),
                Boolean.TRUE.equals(request.resumeSubscriptionEnabled()),
                required(request.jobDesc(), "job_desc", 5000));
    }

    private JobResponse response(JobPostRow row) {
        return new JobResponse(
                row.jobId(),
                row.companyId(),
                row.title(),
                row.jobNatureCode(),
                row.categoryCode(),
                row.categoryName(),
                row.experienceCode(),
                row.educationCode(),
                row.recruitCount(),
                row.cityCode(),
                row.workRegionPath(),
                row.address(),
                row.salaryMin(),
                row.salaryMax(),
                row.salaryNegotiable(),
                row.welfareCodes(),
                row.departmentName(),
                row.ageMin(),
                row.ageMax(),
                row.ageUnlimited(),
                row.recruitmentTimeCode(),
                row.contactMode(),
                row.contactName(),
                row.contactMobile(),
                row.contactPhone(),
                row.contactEmail(),
                row.contactWechat(),
                row.contactHidden(),
                row.notifyEnabled(),
                row.resumeSubscriptionEnabled(),
                row.jobDesc(),
                row.status(),
                row.auditStatus(),
                row.rejectReason(),
                row.updatedAt(),
                row.deletedAt(),
                row.deleteReason());
    }

    private JobReviewResultResponse reviewResult(JobPostRow row) {
        return new JobReviewResultResponse(row.jobId(), row.status(), row.auditStatus(), row.rejectReason());
    }

    private int requireReviewStatus(Integer status) {
        if (status == null || (status != AUDIT_APPROVED && status != AUDIT_REJECTED)) {
            throw validation("audit_status must be 2 or 3");
        }
        return status;
    }

    private long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw validation(fieldName + " is required");
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

    private Integer normalizedMoney(Integer value, String fieldName) {
        return normalizedRangeValue(value, fieldName, 0, 1_000_000_000);
    }

    private Integer normalizedRangeValue(Integer value, String fieldName, int min, int max) {
        if (value == null) {
            return null;
        }
        if (value < min || value > max) {
            throw validation(fieldName + " is out of range");
        }
        return value;
    }

    private void assertOrderedRange(Integer min, Integer max, String label) {
        if (min != null && max != null && min > max) {
            throw validation(label + " is invalid");
        }
    }

    private List<String> normalizeStringList(List<String> values, int maxItems, int maxLength) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map((value) -> normalizeNullable(value, maxLength))
                .filter((value) -> value != null)
                .distinct()
                .limit(maxItems)
                .toList();
    }

    private Map<String, Object> jobAuditSnapshot(JobPostRow row) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("job_id", row.jobId());
        snapshot.put("company_id", row.companyId());
        snapshot.put("title", row.title());
        snapshot.put("job_nature_code", row.jobNatureCode());
        snapshot.put("category_code", row.categoryCode());
        snapshot.put("category_name", row.categoryName());
        snapshot.put("experience_code", row.experienceCode());
        snapshot.put("education_code", row.educationCode());
        snapshot.put("recruit_count", row.recruitCount());
        snapshot.put("city_code", row.cityCode());
        snapshot.put("work_region_path", row.workRegionPath());
        snapshot.put("salary_min", row.salaryMin());
        snapshot.put("salary_max", row.salaryMax());
        snapshot.put("salary_negotiable", row.salaryNegotiable());
        snapshot.put("welfare_count", row.welfareCodes() == null ? 0 : row.welfareCodes().size());
        snapshot.put("department_present", row.departmentName() != null);
        snapshot.put("age_min", row.ageMin());
        snapshot.put("age_max", row.ageMax());
        snapshot.put("age_unlimited", row.ageUnlimited());
        snapshot.put("recruitment_time_code", row.recruitmentTimeCode());
        snapshot.put("contact_mode", row.contactMode());
        snapshot.put("contact_name_present", row.contactName() != null);
        snapshot.put("contact_mobile_present", row.contactMobile() != null);
        snapshot.put("contact_phone_present", row.contactPhone() != null);
        snapshot.put("contact_email_present", row.contactEmail() != null);
        snapshot.put("contact_wechat_present", row.contactWechat() != null);
        snapshot.put("contact_hidden", row.contactHidden());
        snapshot.put("notify_enabled", row.notifyEnabled());
        snapshot.put("resume_subscription_enabled", row.resumeSubscriptionEnabled());
        snapshot.put("job_desc_present", row.jobDesc() != null);
        snapshot.put("status", row.status());
        snapshot.put("audit_status", row.auditStatus());
        snapshot.put("reject_reason", row.rejectReason());
        snapshot.put("reject_reason_present", row.rejectReason() != null);
        snapshot.put("deleted", row.deleted());
        snapshot.put("deleted_at", row.deletedAt());
        snapshot.put("delete_reason_present", row.deleteReason() != null);
        return snapshot;
    }

    private Map<String, Object> restoreAuditSnapshot(JobPostRow row, String reason) {
        Map<String, Object> snapshot = jobAuditSnapshot(row);
        snapshot.put("restore_reason_present", normalizeNullable(reason, 500) != null);
        return snapshot;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return objectMapper.valueToTree(Map.of("serialization", "failed")).toString();
        }
    }

    private ApiException validation(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALID_400", message);
    }

    private ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "AUTHZ_403", message);
    }
}
