package cn.localtalent.backend.authz;

import cn.localtalent.backend.auth.domain.IdentityType;

record AuthzAccountSnapshot(
        IdentityType identityType,
        long userId,
        Long companyId,
        String defaultRoleCode,
        int status
) {
}
