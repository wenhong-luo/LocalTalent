package cn.localtalent.backend.company.application;

import cn.localtalent.backend.audit.AuditService;
import cn.localtalent.backend.auth.domain.IdentityType;
import cn.localtalent.backend.authz.AuthzContext;
import cn.localtalent.backend.authz.AuthzPrincipal;
import cn.localtalent.backend.authz.DataScopeService;
import cn.localtalent.backend.authz.ResourceOwner;
import cn.localtalent.backend.common.exception.ApiException;
import cn.localtalent.backend.company.api.CompanyApplyRequest;
import cn.localtalent.backend.company.api.CompanyReviewItemResponse;
import cn.localtalent.backend.company.api.CompanyReviewPageResponse;
import cn.localtalent.backend.company.api.CompanyReviewRequest;
import cn.localtalent.backend.company.api.CompanyStatusResponse;
import cn.localtalent.backend.company.domain.CompanyReviewRow;
import cn.localtalent.backend.company.infrastructure.CompanyJdbcRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {

    public static final int AUTH_PENDING = 1;
    public static final int AUTH_APPROVED = 2;
    public static final int AUTH_REJECTED = 3;

    private static final int MAX_PAGE_SIZE = 100;

    private final CompanyJdbcRepository companyRepository;
    private final DataScopeService dataScopeService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CompanyService(
            CompanyJdbcRepository companyRepository,
            DataScopeService dataScopeService,
            AuditService auditService
    ) {
        this.companyRepository = companyRepository;
        this.dataScopeService = dataScopeService;
        this.auditService = auditService;
    }

    @Transactional
    public CompanyStatusResponse apply(CompanyApplyRequest request) {
        AuthzPrincipal principal = requireCompanyPrincipal();
        long companyId = principal.companyId();
        dataScopeService.assertWritable(principal, "company", ResourceOwner.company(companyId));
        CompanyReviewRow before = requireCompany(companyId);
        CompanyApplyRequest normalized = normalizeApply(request);

        companyRepository.submitApplication(companyId, normalized);
        CompanyReviewRow after = requireCompany(companyId);
        auditService.record(
                principal,
                "company",
                companyId,
                "company_apply",
                json(before),
                json(after));
        return statusResponse(after);
    }

    public CompanyReviewPageResponse listForReview(Integer authStatus, int page, int size) {
        AuthzPrincipal principal = requireOperator();
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = (normalizedPage - 1) * normalizedSize;
        List<CompanyReviewItemResponse> items = companyRepository
                .listForReview(authStatus, normalizedSize, offset)
                .stream()
                .map(this::reviewItem)
                .toList();
        return new CompanyReviewPageResponse(items, companyRepository.countForReview(authStatus));
    }

    @Transactional
    public CompanyStatusResponse review(CompanyReviewRequest request) {
        AuthzPrincipal principal = requireOperator();
        long companyId = requirePositive(request.companyId(), "company_id");
        int authStatus = requireReviewStatus(request.auditStatus());
        String memo = normalizeNullable(request.memo(), 500);
        if (authStatus == AUTH_REJECTED && memo == null) {
            throw validation("memo is required when rejecting company");
        }

        CompanyReviewRow before = requireCompany(companyId);
        companyRepository.review(companyId, authStatus, authStatus == AUTH_REJECTED ? memo : null, principal.userId());
        CompanyReviewRow after = requireCompany(companyId);
        auditService.record(
                principal,
                "company",
                companyId,
                authStatus == AUTH_APPROVED ? "company_review_pass" : "company_review_reject",
                json(before),
                json(after));
        return statusResponse(after);
    }

    private CompanyApplyRequest normalizeApply(CompanyApplyRequest request) {
        if (request == null) {
            throw validation("request body is required");
        }
        return new CompanyApplyRequest(
                required(request.companyName(), "company_name", 200),
                required(request.licenseNo(), "license_no", 64),
                normalizeNullable(request.industryCode(), 64),
                normalizeNullable(request.scaleCode(), 64),
                normalizeNullable(request.cityCode(), 32),
                normalizeNullable(request.address(), 255),
                normalizeNullable(request.companyProfile(), 2000));
    }

    private CompanyReviewRow requireCompany(long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "company not found"));
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

    private CompanyStatusResponse statusResponse(CompanyReviewRow row) {
        return new CompanyStatusResponse(row.companyId(), row.authStatus(), row.authRejectReason());
    }

    private CompanyReviewItemResponse reviewItem(CompanyReviewRow row) {
        return new CompanyReviewItemResponse(
                row.companyId(),
                row.companyName(),
                row.licenseNo(),
                row.industryCode(),
                row.cityCode(),
                row.authStatus(),
                row.authRejectReason(),
                row.authSubmitTime());
    }

    private int requireReviewStatus(Integer status) {
        if (status == null || (status != AUTH_APPROVED && status != AUTH_REJECTED)) {
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
