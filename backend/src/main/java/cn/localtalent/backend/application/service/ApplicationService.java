package cn.localtalent.backend.application.service;

import cn.localtalent.backend.application.api.ApplicationCreateRequest;
import cn.localtalent.backend.application.api.ApplicationPageResponse;
import cn.localtalent.backend.application.api.ApplicationResponse;
import cn.localtalent.backend.application.domain.ApplicationRow;
import cn.localtalent.backend.application.infrastructure.ApplicationJdbcRepository;
import cn.localtalent.backend.audit.AuditService;
import cn.localtalent.backend.auth.domain.IdentityType;
import cn.localtalent.backend.authz.AuthzContext;
import cn.localtalent.backend.authz.AuthzPrincipal;
import cn.localtalent.backend.authz.DataScopeService;
import cn.localtalent.backend.authz.ResourceOwner;
import cn.localtalent.backend.common.exception.ApiException;
import cn.localtalent.backend.company.application.CompanyService;
import cn.localtalent.backend.job.application.JobService;
import cn.localtalent.backend.job.domain.JobPostRow;
import cn.localtalent.backend.job.infrastructure.JobJdbcRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService {

    public static final int STATUS_APPLIED = 0;
    public static final int STATUS_INVITED = 2;
    public static final int STATUS_SIGNED_IN = 3;

    private static final int MAX_PAGE_SIZE = 100;

    private final ApplicationJdbcRepository applicationRepository;
    private final JobJdbcRepository jobRepository;
    private final DataScopeService dataScopeService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApplicationService(
            ApplicationJdbcRepository applicationRepository,
            JobJdbcRepository jobRepository,
            DataScopeService dataScopeService,
            AuditService auditService
    ) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.dataScopeService = dataScopeService;
        this.auditService = auditService;
    }

    @Transactional
    public ApplicationResponse create(ApplicationCreateRequest request) {
        AuthzPrincipal principal = requireCandidatePrincipal();
        long jobId = requirePositive(request == null ? null : request.jobId(), "job_id");
        Long resumeId = optionalPositive(request == null ? null : request.resumeId(), "resume_id");
        if (resumeId != null && !applicationRepository.resumeBelongsToCandidate(resumeId, principal.userId())) {
            throw forbidden("resume does not belong to current candidate");
        }

        JobPostRow job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "job not found"));
        if (job.status() != JobService.JOB_ONLINE
                || job.auditStatus() != JobService.AUDIT_APPROVED
                || job.companyAuthStatus() != CompanyService.AUTH_APPROVED) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "job not available");
        }

        dataScopeService.assertWritable(principal, "job_application", ResourceOwner.candidate(principal.userId()));
        try {
            long applicationId = applicationRepository.create(jobId, principal.userId(), resumeId);
            ApplicationRow row = requireApplication(applicationId);
            auditService.record(principal, "job_application", applicationId, "application_create", null, json(row));
            return response(row);
        } catch (DuplicateKeyException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "APPLICATION_409", "application already exists");
        }
    }

    public ApplicationPageResponse listCompanyApplications(Long jobId, Integer status, int page, int size) {
        AuthzPrincipal principal = requireCompanyPrincipal();
        Long normalizedJobId = optionalPositive(jobId, "job_id");
        Integer normalizedStatus = normalizeStatus(status);
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = (normalizedPage - 1) * normalizedSize;
        List<ApplicationResponse> items = applicationRepository
                .listByCompany(principal.companyId(), normalizedJobId, normalizedStatus, normalizedSize, offset)
                .stream()
                .map(this::response)
                .toList();
        long total = applicationRepository.countByCompany(principal.companyId(), normalizedJobId, normalizedStatus);
        return new ApplicationPageResponse(items, total);
    }

    public ApplicationResponse getCompanyApplication(long applicationId) {
        AuthzPrincipal principal = requireCompanyPrincipal();
        ApplicationRow row = requireApplication(applicationId);
        dataScopeService.assertAccessible(principal, "job_application", ResourceOwner.company(row.companyId()));
        return response(row);
    }

    public ApplicationRow requireCompanyOwnedApplication(AuthzPrincipal principal, long applicationId) {
        ApplicationRow row = requireApplication(applicationId);
        dataScopeService.assertWritable(principal, "job_application", ResourceOwner.company(row.companyId()));
        return row;
    }

    public ApplicationRow requireApplication(long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "application not found"));
    }

    public void markInvited(long applicationId) {
        applicationRepository.updateStatus(applicationId, STATUS_INVITED);
    }

    public void markSignedIn(long applicationId) {
        applicationRepository.updateStatus(applicationId, STATUS_SIGNED_IN);
    }

    public ApplicationResponse response(ApplicationRow row) {
        return new ApplicationResponse(
                row.applicationId(),
                row.jobId(),
                row.companyId(),
                row.candidateId(),
                row.resumeId(),
                row.status(),
                row.applyTime(),
                row.jobTitle(),
                row.companyName());
    }

    private AuthzPrincipal requireCandidatePrincipal() {
        AuthzPrincipal principal = AuthzContext.requireCurrentPrincipal();
        if (principal.identityType() != IdentityType.CANDIDATE) {
            throw forbidden("candidate identity required");
        }
        return principal;
    }

    private AuthzPrincipal requireCompanyPrincipal() {
        AuthzPrincipal principal = AuthzContext.requireCurrentPrincipal();
        if (principal.identityType() != IdentityType.COMPANY || principal.companyId() == null) {
            throw forbidden("company identity required");
        }
        return principal;
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

    private Integer normalizeStatus(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < 0 || value > 5) {
            throw validation("status must be between 0 and 5");
        }
        return value;
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
