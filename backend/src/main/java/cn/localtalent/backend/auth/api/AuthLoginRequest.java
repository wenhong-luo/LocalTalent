package cn.localtalent.backend.auth.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthLoginRequest(
        @JsonProperty("identity_type") String identityType,
        String account,
        String password
) {

    @Override
    public String toString() {
        return "AuthLoginRequest[identityType=%s, account=%s, password=***]".formatted(identityType, account);
    }
}
