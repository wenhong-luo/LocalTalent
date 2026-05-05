package cn.localtalent.backend.auth.oidc;

import cn.localtalent.backend.auth.domain.IdentityType;

public record OidcIdentityLink(
        String issuer,
        String subjectSha256,
        IdentityType identityType,
        long userId,
        Long companyId,
        int status
) {
}
