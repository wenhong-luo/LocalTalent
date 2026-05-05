package cn.localtalent.backend.auth.oidc;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OidcConfigResponse(
        @JsonProperty("oidc_enabled") boolean oidcEnabled,
        @JsonProperty("local_fallback_enabled") boolean localFallbackEnabled,
        @JsonProperty("login_url") String loginUrl,
        @JsonProperty("logout_url") String logoutUrl
) {
}
