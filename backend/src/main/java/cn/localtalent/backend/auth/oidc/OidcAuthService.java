package cn.localtalent.backend.auth.oidc;

import cn.localtalent.backend.auth.api.AuthLoginResponse;
import cn.localtalent.backend.auth.application.AuthService;
import cn.localtalent.backend.auth.domain.AuthIdentity;
import cn.localtalent.backend.auth.domain.IdentityType;
import cn.localtalent.backend.auth.infrastructure.AuthJdbcRepository;
import cn.localtalent.backend.common.exception.ApiException;
import cn.localtalent.backend.common.trace.TraceIdContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class OidcAuthService {

    private static final long STATE_TTL_SECONDS = 600;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final OidcProperties properties;
    private final AuthJdbcRepository authRepository;
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private volatile DiscoveryDocument discoveryDocument;

    public OidcAuthService(
            OidcProperties properties,
            AuthJdbcRepository authRepository,
            AuthService authService
    ) {
        this.properties = properties;
        this.authRepository = authRepository;
        this.authService = authService;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
    }

    public OidcConfigResponse config() {
        boolean localFallbackVisible = properties.localLoginEnabled() && !properties.isRestrictedEnvironment();
        return new OidcConfigResponse(
                properties.oidcEnabled(),
                localFallbackVisible,
                "/api/auth/oidc/login",
                "/api/auth/logout");
    }

    public ResponseEntity<Void> login(String identityTypeValue, String redirectPath) {
        if (!properties.oidcEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTHZ_403", "oidc login is disabled");
        }
        IdentityType identityType = IdentityType.parse(identityTypeValue);
        String safeRedirect = safeRedirect(redirectPath, destinationFor(identityType));
        DiscoveryDocument discovery = discovery();
        String nonce = randomNonce();
        String state = signState(new OidcState(identityType.value(), safeRedirect, nonce, Instant.now().getEpochSecond()));

        URI authorizationUri = URI.create(discovery.authorizationEndpoint() + "?"
                + query(Map.of(
                "response_type", "code",
                "client_id", properties.clientId(),
                "redirect_uri", properties.redirectUri(),
                "scope", properties.scopes(),
                "state", state,
                "nonce", nonce)));
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, authorizationUri.toString())
                .build();
    }

    public ResponseEntity<String> callback(String code, String state) {
        try {
            if (!properties.oidcEnabled()) {
                throw forbidden("oidc login is disabled");
            }
            String authCode = required(code, "code");
            OidcState oidcState = verifyState(required(state, "state"));
            TokenPayload tokenPayload = exchangeCode(authCode);
            Map<String, Object> claims = validateIdToken(tokenPayload.idToken(), oidcState.nonce());
            AuthIdentity identity = mapIdentity(oidcState, claims);
            AuthLoginResponse loginResponse = authService.issueLoginResponse(identity);
            return successHtml(loginResponse, oidcState.redirectPath());
        } catch (Exception exception) {
            return failureRedirect(exception);
        }
    }

    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    private AuthIdentity mapIdentity(OidcState oidcState, Map<String, Object> claims) {
        IdentityType requestedType = IdentityType.parse(oidcState.identityType());
        String subject = stringClaim(claims, "sub");
        String email = normalizeEmail(stringClaim(claims, "email"));
        if (!Boolean.TRUE.equals(claims.get("email_verified"))) {
            throw unauthorized("oidc email is not verified");
        }
        if (email == null) {
            throw unauthorized("oidc email is required");
        }

        String subjectSha256 = sha256Hex(subject);
        Optional<AuthIdentity> linkedIdentity = authRepository.findOidcLink(properties.issuerUri(), subjectSha256)
                .filter(link -> link.status() == 1)
                .flatMap(link -> authRepository.findIdentity(link.identityType(), link.userId()))
                .filter(identity -> identity.status() == 1);
        if (linkedIdentity.isPresent()) {
            return linkedIdentity.get();
        }

        AuthIdentity identity = authRepository.findByVerifiedEmail(requestedType, email)
                .filter(identityRow -> identityRow.status() == 1)
                .orElseThrow(() -> unauthorized("oidc account is not linked"));
        authRepository.upsertOidcLink(properties.issuerUri(), subjectSha256, identity, email);
        return identity;
    }

    private TokenPayload exchangeCode(String code) throws Exception {
        DiscoveryDocument discovery = discovery();
        String form = query(Map.of(
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", properties.redirectUri(),
                "client_id", properties.clientId(),
                "client_secret", properties.clientSecret()));
        HttpRequest request = HttpRequest.newBuilder(URI.create(discovery.tokenEndpoint()))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw unauthorized("oidc token exchange failed");
        }
        JsonNode tokenJson = objectMapper.readTree(response.body());
        String idToken = tokenJson.path("id_token").asText(null);
        if (idToken == null || idToken.isBlank()) {
            throw unauthorized("oidc id token is missing");
        }
        return new TokenPayload(idToken);
    }

    private Map<String, Object> validateIdToken(String idToken, String expectedNonce) throws Exception {
        String[] parts = idToken.split("\\.");
        if (parts.length != 3) {
            throw unauthorized("invalid oidc token");
        }
        Map<String, Object> header = objectMapper.readValue(base64UrlDecode(parts[0]), MAP_TYPE);
        Map<String, Object> claims = objectMapper.readValue(base64UrlDecode(parts[1]), MAP_TYPE);
        String algorithm = String.valueOf(header.getOrDefault("alg", ""));
        if (!"RS256".equals(algorithm)) {
            throw unauthorized("unsupported oidc token algorithm");
        }
        PublicKey key = publicKey(String.valueOf(header.getOrDefault("kid", "")));
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(key);
        signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8));
        if (!signature.verify(Base64.getUrlDecoder().decode(parts[2]))) {
            throw unauthorized("invalid oidc token signature");
        }

        String issuer = stringClaim(claims, "iss");
        if (!properties.issuerUri().equals(issuer)) {
            throw unauthorized("invalid oidc issuer");
        }
        if (!audienceContains(claims.get("aud"), properties.clientId())) {
            throw unauthorized("invalid oidc audience");
        }
        long exp = numberClaim(claims, "exp");
        if (exp <= Instant.now().getEpochSecond()) {
            throw unauthorized("expired oidc token");
        }
        String nonce = stringClaim(claims, "nonce");
        if (!expectedNonce.equals(nonce)) {
            throw unauthorized("invalid oidc nonce");
        }
        if (stringClaim(claims, "sub") == null) {
            throw unauthorized("oidc subject is required");
        }
        return claims;
    }

    private PublicKey publicKey(String kid) throws Exception {
        DiscoveryDocument discovery = discovery();
        HttpRequest request = HttpRequest.newBuilder(URI.create(discovery.jwksUri())).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw unauthorized("oidc jwks fetch failed");
        }
        JsonNode keys = objectMapper.readTree(response.body()).path("keys");
        for (JsonNode key : keys) {
            if (kid.equals(key.path("kid").asText()) && "RSA".equals(key.path("kty").asText())) {
                BigInteger modulus = unsignedBigInteger(key.path("n").asText());
                BigInteger exponent = unsignedBigInteger(key.path("e").asText());
                return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
            }
        }
        throw unauthorized("oidc signing key not found");
    }

    private DiscoveryDocument discovery() {
        DiscoveryDocument cached = discoveryDocument;
        if (cached != null) {
            return cached;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(
                            properties.issuerUri() + "/.well-known/openid-configuration"))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw unauthorized("oidc discovery failed");
            }
            JsonNode document = objectMapper.readTree(response.body());
            DiscoveryDocument discovered = new DiscoveryDocument(
                    document.path("authorization_endpoint").asText(),
                    document.path("token_endpoint").asText(),
                    document.path("jwks_uri").asText());
            discoveryDocument = discovered;
            return discovered;
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw unauthorized("oidc discovery failed");
        }
    }

    private String signState(OidcState state) {
        try {
            String payload = base64Url(objectMapper.writeValueAsBytes(Map.of(
                    "identity_type", state.identityType(),
                    "redirect", state.redirectPath(),
                    "nonce", state.nonce(),
                    "iat", state.issuedAt(),
                    "exp", state.issuedAt() + STATE_TTL_SECONDS)));
            return payload + "." + hmac(payload);
        } catch (Exception exception) {
            throw forbidden("failed to create oidc state");
        }
    }

    private OidcState verifyState(String state) {
        try {
            String[] parts = state.split("\\.");
            if (parts.length != 2 || !constantTimeEquals(hmac(parts[0]), parts[1])) {
                throw unauthorized("invalid oidc state");
            }
            Map<String, Object> payload = objectMapper.readValue(base64UrlDecode(parts[0]), MAP_TYPE);
            long exp = numberValue(payload.get("exp"));
            if (exp <= Instant.now().getEpochSecond()) {
                throw unauthorized("expired oidc state");
            }
            return new OidcState(
                    String.valueOf(payload.get("identity_type")),
                    safeRedirect(String.valueOf(payload.get("redirect")), "/candidate/center"),
                    String.valueOf(payload.get("nonce")),
                    numberValue(payload.get("iat")));
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw unauthorized("invalid oidc state");
        }
    }

    private ResponseEntity<String> successHtml(AuthLoginResponse response, String redirectPath) throws Exception {
        IdentityType identityType = IdentityType.parse(response.identity().identityType());
        String roleHint = response.identity().roleCodes().contains("auditor")
                ? "auditor"
                : response.identity().identityType().equals(IdentityType.OPERATOR.value()) ? "operator" : "";
        String redirectTarget = identityCompatibleRedirect(redirectPath, identityType);
        String html = """
                <!doctype html>
                <html lang="zh-CN">
                <head><meta charset="utf-8"><title>LocalTalent SSO</title></head>
                <body>
                <script>
                localStorage.setItem('localtalent_access_token', %s);
                const roleHint = %s;
                if (roleHint) {
                  localStorage.setItem('localtalent_admin_role_hint', roleHint);
                } else {
                  localStorage.removeItem('localtalent_admin_role_hint');
                }
                window.location.replace(%s);
                </script>
                <noscript>SSO 登录成功，请启用 JavaScript 后继续。</noscript>
                </body>
                </html>
                """.formatted(
                objectMapper.writeValueAsString(response.accessToken()),
                objectMapper.writeValueAsString(roleHint),
                objectMapper.writeValueAsString(redirectTarget));
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    private ResponseEntity<String> failureRedirect(Exception exception) {
        String code = exception instanceof ApiException apiException ? apiException.getCode() : "AUTH_401";
        String target = properties.frontendBaseUrl() + "/auth/login?oidc_error=" + encode(code)
                + "&trace_id=" + encode(Optional.ofNullable(TraceIdContext.getCurrentTraceId()).orElse(""));
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, target)
                .build();
    }

    private String query(Map<String, String> values) {
        StringJoiner joiner = new StringJoiner("&");
        values.forEach((key, value) -> joiner.add(encode(key) + "=" + encode(value)));
        return joiner.toString();
    }

    private String safeRedirect(String redirectPath, String fallback) {
        if (redirectPath == null || redirectPath.isBlank()) {
            return fallback;
        }
        String trimmed = redirectPath.trim();
        if (!trimmed.startsWith("/") || trimmed.startsWith("//") || trimmed.contains("://")) {
            return fallback;
        }
        return trimmed;
    }

    private String identityCompatibleRedirect(String redirectPath, IdentityType identityType) {
        String fallback = destinationFor(identityType);
        String safeRedirect = safeRedirect(redirectPath, fallback);
        String requiredSection = switch (identityType) {
            case CANDIDATE -> "/candidate";
            case COMPANY -> "/company";
            case OPERATOR -> "/admin";
        };
        return isSameRouteSection(safeRedirect, requiredSection) ? safeRedirect : fallback;
    }

    private boolean isSameRouteSection(String path, String section) {
        return path.equals(section) || path.startsWith(section + "/") || path.startsWith(section + "?");
    }

    private String destinationFor(IdentityType identityType) {
        return switch (identityType) {
            case CANDIDATE -> "/candidate/center";
            case COMPANY -> "/company";
            case OPERATOR -> "/admin";
        };
    }

    private boolean audienceContains(Object aud, String clientId) {
        if (aud instanceof String audString) {
            return clientId.equals(audString);
        }
        if (aud instanceof List<?> audList) {
            return audList.stream().anyMatch(value -> clientId.equals(String.valueOf(value)));
        }
        return false;
    }

    private String stringClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }

    private long numberClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (value == null) {
            throw unauthorized("oidc numeric claim is required");
        }
        return numberValue(value);
    }

    private long numberValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw unauthorized(fieldName + " is required");
        }
        return value.trim();
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String randomNonce() {
        byte[] bytes = new byte[24];
        java.security.SecureRandom secureRandom = new java.security.SecureRandom();
        secureRandom.nextBytes(bytes);
        return base64Url(bytes);
    }

    private String hmac(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(properties.stateSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return base64Url(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256Hex(String value) {
        if (value == null || value.isBlank()) {
            throw unauthorized("oidc subject is required");
        }
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("failed to hash oidc subject", exception);
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private BigInteger unsignedBigInteger(String base64Url) {
        return new BigInteger(1, base64UrlDecode(base64Url));
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_401", message);
    }

    private ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "AUTHZ_403", message);
    }

    private record DiscoveryDocument(String authorizationEndpoint, String tokenEndpoint, String jwksUri) {
    }

    private record OidcState(String identityType, String redirectPath, String nonce, long issuedAt) {
    }

    private record TokenPayload(String idToken) {
    }
}
