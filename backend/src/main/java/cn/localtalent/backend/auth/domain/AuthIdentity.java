package cn.localtalent.backend.auth.domain;

public record AuthIdentity(
        IdentityType identityType,
        long userId,
        Long companyId,
        String displayName,
        int status
) {
}
