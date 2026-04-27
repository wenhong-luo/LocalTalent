package cn.localtalent.backend.auth.infrastructure;

import cn.localtalent.backend.auth.domain.AuthIdentity;
import cn.localtalent.backend.auth.domain.IdentityType;
import cn.localtalent.backend.auth.domain.JwtPrincipal;
import cn.localtalent.backend.common.exception.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final String secret;
    private final long ttlSeconds;

    public JwtService(
            @Value("${localtalent.auth.jwt.secret}") String secret,
            @Value("${localtalent.auth.jwt.ttl-seconds}") long ttlSeconds
    ) {
        this.objectMapper = new ObjectMapper();
        this.secret = secret;
        this.ttlSeconds = ttlSeconds;
    }

    public String issueToken(AuthIdentity identity) {
        Instant now = Instant.now();
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", identity.identityType().value() + ":" + identity.userId());
        payload.put("identity_type", identity.identityType().value());
        payload.put("user_id", identity.userId());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plusSeconds(ttlSeconds).getEpochSecond());
        payload.put("jti", UUID.randomUUID().toString());

        String signingInput = encodeJson(header) + "." + encodeJson(payload);
        return signingInput + "." + signToBase64Url(signingInput);
    }

    public JwtPrincipal parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw unauthorized();
            }

            Map<String, Object> header = decodeJson(parts[0]);
            if (!"HS256".equals(header.get("alg")) || !"JWT".equals(header.get("typ"))) {
                throw unauthorized();
            }

            String signingInput = parts[0] + "." + parts[1];
            String expectedSignature = signToBase64Url(signingInput);
            if (!MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.US_ASCII),
                    parts[2].getBytes(StandardCharsets.US_ASCII))) {
                throw unauthorized();
            }

            Map<String, Object> payload = decodeJson(parts[1]);
            long expiresAt = numberClaim(payload, "exp");
            if (Instant.now().getEpochSecond() >= expiresAt) {
                throw unauthorized();
            }

            IdentityType identityType = IdentityType.parse(stringClaim(payload, "identity_type"));
            long userId = numberClaim(payload, "user_id");
            String tokenId = stringClaim(payload, "jti");
            return new JwtPrincipal(identityType, userId, tokenId, Instant.ofEpochSecond(expiresAt));
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw unauthorized();
        }
    }

    public long ttlSeconds() {
        return ttlSeconds;
    }

    private String encodeJson(Map<String, Object> payload) {
        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(payload);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(jsonBytes);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to encode jwt", exception);
        }
    }

    private Map<String, Object> decodeJson(String encoded) {
        try {
            byte[] jsonBytes = Base64.getUrlDecoder().decode(encoded);
            return objectMapper.readValue(jsonBytes, MAP_TYPE);
        } catch (Exception exception) {
            throw unauthorized();
        }
    }

    private String signToBase64Url(String signingInput) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to sign jwt", exception);
        }
    }

    private String stringClaim(Map<String, Object> payload, String name) {
        Object value = payload.get(name);
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        throw unauthorized();
    }

    private long numberClaim(Map<String, Object> payload, String name) {
        Object value = payload.get(name);
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }
        throw unauthorized();
    }

    private ApiException unauthorized() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_401", "invalid or expired token");
    }
}
