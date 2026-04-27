package cn.localtalent.backend.authz;

import cn.localtalent.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class DataScopeService {

    public void assertAccessible(AuthzPrincipal principal, String bizType, ResourceOwner owner) {
        if (!isAccessible(principal, bizType, owner)) {
            throw forbidden("data scope denied");
        }
    }

    public void assertWritable(AuthzPrincipal principal, String bizType, ResourceOwner owner) {
        if (principal.hasRole("auditor")) {
            throw forbidden("auditor is read only");
        }
        assertAccessible(principal, bizType, owner);
    }

    public boolean isAccessible(AuthzPrincipal principal, String bizType, ResourceOwner owner) {
        if (owner == null) {
            return false;
        }
        if (principal.hasRole("candidate") && owner.candidateId() != null) {
            return owner.candidateId() == principal.userId();
        }
        if (hasCompanyRole(principal) && owner.companyId() != null && principal.companyId() != null) {
            return owner.companyId().equals(principal.companyId());
        }
        if (principal.hasRole("operator")) {
            return owner.aggregateOnly() || owner.detailAuthorized();
        }
        if (principal.hasRole("auditor")) {
            return owner.auditOnly() || "audit_log".equals(bizType) || "field_access_log".equals(bizType);
        }
        if (principal.hasRole("open_client")) {
            return "open_scope".equals(bizType);
        }
        return false;
    }

    private boolean hasCompanyRole(AuthzPrincipal principal) {
        return principal.hasRole("company_admin")
                || principal.hasRole("recruiter")
                || principal.hasRole("interviewer");
    }

    private ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "AUTHZ_403", message);
    }
}
