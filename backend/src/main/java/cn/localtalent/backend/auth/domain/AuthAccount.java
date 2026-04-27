package cn.localtalent.backend.auth.domain;

public record AuthAccount(
        IdentityType identityType,
        long userId,
        Long companyId,
        String displayName,
        int status,
        String passwordHash
) {
}
