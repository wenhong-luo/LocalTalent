package cn.localtalent.backend.auth.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthLoginResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn,
        AuthIdentityResponse identity
) {

    @Override
    public String toString() {
        return "AuthLoginResponse[accessToken=***, tokenType=%s, expiresIn=%d, identity=%s]"
                .formatted(tokenType, expiresIn, identity);
    }
}
