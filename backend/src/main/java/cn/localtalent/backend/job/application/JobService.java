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

    @Transactional
    public JobResponse create(JobCreateRequest request) {
        AuthzPrincipal principal = requireCompanyPrincipal();
        JobCreateRequest normalized = normalizeCreate(request);
        long jobId = jobRepository.create(principal.companyId(), normalized);
        JobPostRow after = requireJob(jobId);
        auditService.record(principal, "job_post", jobId, "job_create", null, json(after));
        return response(after);
    }

    public JobResponse getCompanyJob(long jobId) {
        AuthzPrincipal principal = requireCompanyPrincipal();
        JobPostRow row = requireJob(jobId);
        dataScopeService.assertAccessible(principal, "job_post", ResourceOwner.company(row.companyId()));
        return response(row);
    }

    @Transactional
    public JobResponse update(long jobId, JobUpdateRequest request) {
        AuthzPrincipal principal = requireCompanyPrincipal();
        JobPostRow before = requireCompanyOwnedJob(principal, jobId);
        JobUpdateRequest normalized = normalizeUpdate(request);
        jobRepository.update(jobId, normalized);
        JobPostRow after = requireJob(jobId);
        auditService.record(principal, "job_post", jobId, "job_update", json(before), json(after));
        return response(after);
    }

    @Transactional
    public JobReviewResultResponse changeCompanyStatus(long jobId, JobStatusRequest request) {
        AuthzPrincipal principal = requireCompanyPrincipal();
        JobPostRow before = requireCompanyOwnedJob(principal, jobId);
        String action = request == null ? null : normalizeNullable(request.action(), 32);
        String reason = request == null ? null : normalizeNullable(request.reason(), 500);
        if ("submit_review".equals(action)) {
            jobRepository.submitReview(jobId);
            JobPostRow after = requireJob(jobId);
            auditService.record(principal, "job_post", jobId, "job_submit_review", json(before), json(after));
            return reviewResult(after);
        }
        if ("offline".equals(action)) {
            jobRepository.offline(jobId, reason);
            JobPostRow after = requireJob(jobId);
            auditService.record(principal, "job_post", jobId, "job_offline", json(before), json(after));
            return reviewResult(after);
        }
        throw validation("action must be submit_review or offline");
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
        return response(requireJob(jobId));
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

        JobPostRow before = requireJob(jobId);
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
        JobPostRow after = requireJob(jobId);
        auditService.record(
                principal,
                "job_post",
                jobId,
                auditStatus == AUDIT_APPROVED ? "job_review_pass" : "job_review_reject",
                json(before),
                json(after));
        return reviewResult(after);
    }

    private JobPostRow requireCompanyOwnedJob(AuthzPrincipal principal, long jobId) {
        JobPostRow row = requireJob(jobId);
        dataScopeService.assertWritable(principal, "job_post", ResourceOwner.company(row.companyId()));
        return row;
    }

    private JobPostRow requireJob(long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "job not found"));
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
        return new JobCreateRequest(
                required(request.title(), "title", 150),
                normalizeNullable(request.categoryCode(), 64),
                normalizeNullable(request.cityCode(), 32),
                request.salaryMin(),
                request.salaryMax(),
                required(request.jobDesc(), "job_desc", 5000));
    }

    private JobUpdateRequest normalizeUpdate(JobUpdateRequest request) {
        if (request == null) {
            throw validation("request body is required");
        }
        return new JobUpdateRequest(
                required(request.title(), "title", 150),
                normalizeNullable(request.categoryCode(), 64),
                normalizeNullable(request.cityCode(), 32),
                request.salaryMin(),
                request.salaryMax(),
                required(request.jobDesc(), "job_desc", 5000));
    }

    private JobResponse response(JobPostRow row) {
        return new JobResponse(
                row.jobId(),
                row.companyId(),
                row.title(),
                row.categoryCode(),
                row.cityCode(),
                row.salaryMin(),
                row.salaryMax(),
                row.jobDesc(),
                row.status(),
                row.auditStatus(),
                row.rejectReason(),
                row.updatedAt());
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
