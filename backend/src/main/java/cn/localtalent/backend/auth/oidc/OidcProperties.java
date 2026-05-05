package cn.localtalent.backend.auth.oidc;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OidcProperties {

    private final String environment;
    private final boolean oidcEnabled;
    private final boolean localLoginEnabled;
    private final Set<String> localLoginWhitelist;
    private final String issuerUri;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String scopes;
    private final String stateSecret;
    private final String frontendBaseUrl;

    public OidcProperties(
            @Value("${localtalent.env:local}") String environment,
            @Value("${localtalent.auth.oidc.enabled:false}") boolean oidcEnabled,
            @Value("${localtalent.auth.local-login.enabled:true}") boolean localLoginEnabled,
            @Value("${localtalent.auth.local-login.whitelist:}") String localLoginWhitelist,
            @Value("${localtalent.auth.oidc.issuer-uri:https://issuer.example.invalid}") String issuerUri,
            @Value("${localtalent.auth.oidc.client-id:CHANGE_ME_CLIENT_ID}") String clientId,
            @Value("${localtalent.auth.oidc.client-secret:CHANGE_ME_CLIENT_SECRET}") String clientSecret,
            @Value("${localtalent.auth.oidc.redirect-uri:http://localhost:8080/api/auth/oidc/callback}") String redirectUri,
            @Value("${localtalent.auth.oidc.scopes:openid email profile}") String scopes,
            @Value("${localtalent.auth.oidc.state-secret:CHANGE_ME_OIDC_STATE_SECRET}") String stateSecret,
            @Value("${localtalent.auth.oidc.frontend-base-url:http://localhost:3000}") String frontendBaseUrl
    ) {
        this.environment = normalize(environment);
        this.oidcEnabled = oidcEnabled;
        this.localLoginEnabled = localLoginEnabled;
        this.localLoginWhitelist = parseWhitelist(localLoginWhitelist);
        this.issuerUri = trimTrailingSlash(issuerUri);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.scopes = scopes;
        this.stateSecret = stateSecret;
        this.frontendBaseUrl = trimTrailingSlash(frontendBaseUrl);
    }

    public String environment() {
        return environment;
    }

    public boolean oidcEnabled() {
        return oidcEnabled;
    }

    public boolean localLoginEnabled() {
        return localLoginEnabled;
    }

    public boolean isRestrictedEnvironment() {
        return "staging".equals(environment) || "gray".equals(environment) || "prod".equals(environment)
                || "production".equals(environment);
    }

    public boolean isLocalLoginAllowed(String account) {
        if (!localLoginEnabled) {
            return false;
        }
        if (!isRestrictedEnvironment()) {
            return true;
        }
        return account != null && localLoginWhitelist.contains(account.trim().toLowerCase());
    }

    public String issuerUri() {
        return issuerUri;
    }

    public String clientId() {
        return clientId;
    }

    public String clientSecret() {
        return clientSecret;
    }

    public String redirectUri() {
        return redirectUri;
    }

    public String scopes() {
        return scopes;
    }

    public String stateSecret() {
        return stateSecret;
    }

    public String frontendBaseUrl() {
        return frontendBaseUrl;
    }

    private Set<String> parseWhitelist(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(rawValue.split(","))
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
