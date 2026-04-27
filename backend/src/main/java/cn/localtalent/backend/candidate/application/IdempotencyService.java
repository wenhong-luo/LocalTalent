package cn.localtalent.backend.candidate.application;

import cn.localtalent.backend.authz.AuthzPrincipal;
import cn.localtalent.backend.candidate.domain.IdempotencyStoredResponse;
import cn.localtalent.backend.candidate.domain.IdempotentActionResult;
import cn.localtalent.backend.candidate.infrastructure.IdempotencyJdbcRepository;
import cn.localtalent.backend.common.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

    private static final int MAX_KEY_LENGTH = 128;

    private final IdempotencyJdbcRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyJdbcRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper();
    }

    public <T> T execute(
            String apiCode,
            AuthzPrincipal principal,
            String idempotencyKey,
            Object fingerprintPayload,
            Class<T> responseType,
            Supplier<IdempotentActionResult<T>> action
    ) {
        String normalizedKey = normalizeKey(idempotencyKey);
        String requestHash = hash(fingerprintPayload);
        IdempotencyStoredResponse storedResponse = repository.find(apiCode, principal, normalizedKey).orElse(null);
        if (storedResponse != null) {
            if (!storedResponse.requestHash().equals(requestHash)) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "IDEMPOTENCY_409",
                        "idempotency key reused with different request");
            }
            return readResponse(storedResponse.responseJson(), responseType);
        }

        IdempotentActionResult<T> result = action.get();
        repository.recordSuccess(
                apiCode,
                principal,
                normalizedKey,
                requestHash,
                writeResponse(result.response()),
                result.resourceType(),
                result.resourceId());
        return result.response();
    }

    private String normalizeKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw validation("X-Idempotency-Key is required");
        }
        String normalized = idempotencyKey.trim();
        if (normalized.length() > MAX_KEY_LENGTH) {
            throw validation("X-Idempotency-Key is too long");
        }
        return normalized;
    }

    private String hash(Object payload) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(payload);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(json);
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to hash idempotency payload", exception);
        }
    }

    private <T> T readResponse(String responseJson, Class<T> responseType) {
        try {
            return objectMapper.readValue(responseJson, responseType);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to read stored idempotency response", exception);
        }
    }

    private String writeResponse(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to write idempotency response", exception);
        }
    }

    private ApiException validation(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALID_400", message);
    }
}
