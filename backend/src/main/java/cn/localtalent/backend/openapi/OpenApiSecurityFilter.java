package cn.localtalent.backend.openapi;

import cn.localtalent.backend.common.api.ApiResponse;
import cn.localtalent.backend.common.exception.ApiException;
import cn.localtalent.backend.common.trace.TraceIdContext;
import cn.localtalent.backend.common.trace.TraceIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class OpenApiSecurityFilter extends OncePerRequestFilter {

    private static final byte[] EMPTY_BODY = new byte[0];
    private static final Pattern FORBIDDEN_ORIGINAL_CANDIDATE_FIELDS = Pattern.compile(
            "\"(mobile|email|resume_body|attachment_object_key|evidence)\"\\s*:",
            Pattern.CASE_INSENSITIVE);

    private final OpenApiJdbcRepository repository;
    private final ObjectMapper objectMapper;
    private final long timestampWindowSeconds;

    public OpenApiSecurityFilter(
            OpenApiJdbcRepository repository,
            @Value("${localtalent.openapi.timestamp-window-seconds}") long timestampWindowSeconds
    ) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper();
        this.timestampWindowSeconds = timestampWindowSeconds;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/open/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        byte[] body = request.getInputStream().readAllBytes();
        if (body == null) {
            body = EMPTY_BODY;
        }
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request, body);
        long startedAt = System.nanoTime();
        long logId = 0L;
        String clientCode = header(request, "X-Client-Code");
        String idempotencyKey = header(request, "X-Idempotency-Key");
        String requestHash = OpenApiCrypto.sha256Hex(body);
        String apiCode = OpenApiApiCode.fromPath(request.getMethod(), request.getRequestURI());
        String requestUri = pathWithQuery(request);

        try {
            validateEndpoint(apiCode);
            validateBodyHash(requestHash, header(request, "X-Body-SHA256"));
            OpenApiClient client = authenticateClient(clientCode);
            logId = repository.insertOpenApiLog(
                    client.sourceSystem(),
                    client.clientCode(),
                    apiCode,
                    TraceIdContext.getCurrentTraceId(),
                    requestUri,
                    request.getMethod(),
                    requestHash,
                    idempotencyKey,
                    repository.safeSummary(client.clientCode(), requestHash, idempotencyKey, body.length > 0));

            validateScope(client, apiCode);
            Instant timestamp = validateTimestamp(header(request, "X-Timestamp"));
            validateSignature(request, requestUri, client, requestHash, idempotencyKey);
            validateWriteIdempotency(request, idempotencyKey);
            validateSensitiveBody(body);
            validateNonce(client, required(header(request, "X-Nonce"), "X-Nonce"), timestamp);

            repository.touchClient(client.clientCode());
            OpenApiContext.set(new OpenApiRequestContext(client, apiCode, requestHash, idempotencyKey, logId));
            filterChain.doFilter(cachedRequest, response);
            finishLog(logId, response.getStatus(), response.getStatus() < 400, startedAt, null);
        } catch (ApiException exception) {
            if (logId == 0L) {
                logId = repository.insertOpenApiLog(
                        "unknown",
                        clientCode,
                        apiCode,
                        TraceIdContext.getCurrentTraceId(),
                        requestUri,
                        request.getMethod(),
                        requestHash,
                        idempotencyKey,
                        repository.safeSummary(clientCode, requestHash, idempotencyKey, body.length > 0));
            }
            finishLog(logId, exception.getStatus().value(), false, startedAt, exception.getMessage());
            writeError(response, exception.getStatus(), exception.getCode(), exception.getMessage());
        } catch (Exception exception) {
            if (logId == 0L) {
                logId = repository.insertOpenApiLog(
                        "unknown",
                        clientCode,
                        apiCode,
                        TraceIdContext.getCurrentTraceId(),
                        requestUri,
                        request.getMethod(),
                        requestHash,
                        idempotencyKey,
                        repository.safeSummary(clientCode, requestHash, idempotencyKey, body.length > 0));
            }
            finishLog(logId, HttpStatus.INTERNAL_SERVER_ERROR.value(), false, startedAt, "internal server error");
            writeError(response, HttpStatus.INTERNAL_SERVER_ERROR, "SYS_500", "internal server error");
        } finally {
            OpenApiContext.clear();
        }
    }

    private void validateEndpoint(String apiCode) {
        if ("open.unknown".equals(apiCode)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "VALID_400", "unknown open api endpoint");
        }
    }

    private OpenApiClient authenticateClient(String clientCode) {
        if (clientCode == null || clientCode.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "OPEN_AUTH_401", "X-Client-Code is required");
        }
        String normalized = clientCode.trim();
        return repository.findClient(normalized)
                .filter(client -> client.status() == 1)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "OPEN_AUTH_401",
                        "invalid open api client"));
    }

    private void validateScope(OpenApiClient client, String apiCode) {
        if (!client.allows(apiCode)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "OPEN_SCOPE_403", "open api scope denied");
        }
    }

    private Instant validateTimestamp(String timestampHeader) {
        if (timestampHeader == null || timestampHeader.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "OPEN_TIMESTAMP_401", "X-Timestamp is required");
        }
        String rawTimestamp = timestampHeader.trim();
        Instant timestamp = parseTimestamp(rawTimestamp);
        long skew = Math.abs(Instant.now().getEpochSecond() - timestamp.getEpochSecond());
        if (skew > timestampWindowSeconds) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "OPEN_TIMESTAMP_401", "open api timestamp expired");
        }
        return timestamp;
    }

    private Instant parseTimestamp(String timestamp) {
        try {
            if (timestamp.matches("\\d+")) {
                return Instant.ofEpochSecond(Long.parseLong(timestamp));
            }
            return Instant.parse(timestamp);
        } catch (Exception ignored) {
            try {
                return OffsetDateTime.parse(timestamp).toInstant();
            } catch (Exception exception) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "OPEN_TIMESTAMP_401", "invalid open api timestamp");
            }
        }
    }

    private void validateBodyHash(String requestHash, String bodyHashHeader) {
        if (bodyHashHeader == null || bodyHashHeader.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "OPEN_SIGNATURE_401", "X-Body-SHA256 is required");
        }
        String expected = bodyHashHeader.trim().toLowerCase(Locale.ROOT);
        if (!MessageDigest.isEqual(
                requestHash.getBytes(StandardCharsets.US_ASCII),
                expected.getBytes(StandardCharsets.US_ASCII))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "OPEN_SIGNATURE_401", "body hash mismatch");
        }
    }

    private void validateSignature(
            HttpServletRequest request,
            String requestUri,
            OpenApiClient client,
            String requestHash,
            String idempotencyKey
    ) {
        String signatureHeader = header(request, "X-Signature");
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "OPEN_SIGNATURE_401", "X-Signature is required");
        }
        String signature = signatureHeader.toLowerCase(Locale.ROOT);
        String signingString = request.getMethod().toUpperCase(Locale.ROOT)
                + "\n" + requestUri
                + "\n" + client.clientCode()
                + "\n" + required(header(request, "X-Timestamp"), "X-Timestamp")
                + "\n" + required(header(request, "X-Nonce"), "X-Nonce")
                + "\n" + (idempotencyKey == null ? "" : idempotencyKey)
                + "\n" + requestHash;
        String expected = OpenApiCrypto.hmacSha256Hex(signingString, client.secretHash());
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                signature.getBytes(StandardCharsets.US_ASCII))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "OPEN_SIGNATURE_401", "invalid open api signature");
        }
    }

    private void validateWriteIdempotency(HttpServletRequest request, String idempotencyKey) {
        if ("POST".equalsIgnoreCase(request.getMethod()) && (idempotencyKey == null || idempotencyKey.isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALID_400", "X-Idempotency-Key is required");
        }
        if (idempotencyKey != null && idempotencyKey.length() > 128) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALID_400", "X-Idempotency-Key is too long");
        }
    }

    private void validateSensitiveBody(byte[] body) {
        if (body.length == 0) {
            return;
        }
        String rawBody = new String(body, StandardCharsets.UTF_8);
        if (FORBIDDEN_ORIGINAL_CANDIDATE_FIELDS.matcher(rawBody).find()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "OPEN_SENSITIVE_400",
                    "open api payload contains forbidden original candidate data");
        }
    }

    private void validateNonce(OpenApiClient client, String nonce, Instant timestamp) {
        if (nonce.length() > 128) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALID_400", "X-Nonce is too long");
        }
        LocalDateTime timestampAt = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
        if (!repository.recordNonce(client.clientCode(), nonce, timestampAt)) {
            throw new ApiException(HttpStatus.CONFLICT, "OPEN_REPLAY_409", "open api nonce already used");
        }
    }

    private void finishLog(long logId, int status, boolean success, long startedAt, String errorMessage) {
        if (logId == 0L) {
            return;
        }
        long durationMs = Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
        String code = success ? "0" : errorCode(errorMessage);
        repository.finishOpenApiLog(
                logId,
                status,
                success,
                durationMs,
                errorMessage,
                repository.responseSummary(status, code));
    }

    private String errorCode(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "SYS_500";
        }
        return "error";
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setHeader(TraceIdFilter.TRACE_ID_HEADER, TraceIdContext.getCurrentTraceId());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(code, message, null));
    }

    private String required(String value, String headerName) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALID_400", headerName + " is required");
        }
        return value.trim();
    }

    private String header(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value == null ? null : value.trim();
    }

    private String pathWithQuery(HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null || query.isBlank() ? request.getRequestURI() : request.getRequestURI() + "?" + query;
    }
}
