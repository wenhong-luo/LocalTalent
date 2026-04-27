package cn.localtalent.backend.authz;

import cn.localtalent.backend.auth.domain.IdentityType;
import java.util.List;

public record AuthzPrincipal(
        IdentityType identityType,
        long userId,
        Long companyId,
        List<String> roleCodes,
        String tokenId
) {

    public AuthzPrincipal {
        roleCodes = List.copyOf(roleCodes);
    }

    public boolean hasRole(String roleCode) {
        return roleCodes.contains(roleCode);
    }

    public String primaryRole() {
        return roleCodes.isEmpty() ? identityType.value() : roleCodes.getFirst();
    }
}
