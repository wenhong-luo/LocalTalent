package cn.localtalent.backend.auth.domain;

import java.time.Instant;

public record JwtPrincipal(
        IdentityType identityType,
        long userId,
        String tokenId,
        Instant expiresAt
) {
}
