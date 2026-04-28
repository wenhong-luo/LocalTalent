package cn.localtalent.backend.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

abstract class OpenApiTestSupport {

    protected static final String CLIENT_CODE = "localtalent_stub";
    protected static final String RAW_SECRET = "LocalTalentOpen@123456";
    protected static final String SECRET_HASH = "97911de7ae1a8392865d8a6ce438d39b59236664f2985d0e84fd4be47be5779b";

    protected final HttpClient httpClient = HttpClient.newHttpClient();
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @LocalServerPort
    protected int port;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected OpenApiResponse postOpen(
            String path,
            String body,
            String traceId,
            String nonce,
            String idempotencyKey
    ) throws Exception {
        return postOpen(path, body, traceId, nonce, idempotencyKey, Instant.now().toString(), false);
    }

    protected OpenApiResponse postOpen(
            String path,
            String body,
            String traceId,
            String nonce,
            String idempotencyKey,
            String timestamp,
            boolean invalidSignature
    ) throws Exception {
        String bodyHash = OpenApiCrypto.sha256Hex(body.getBytes(StandardCharsets.UTF_8));
        String signature = signature("POST", path, timestamp, nonce, idempotencyKey, bodyHash);
        if (invalidSignature) {
            signature = ("0".equals(signature.substring(0, 1)) ? "1" : "0") + signature.substring(1);
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .header("X-Client-Code", CLIENT_CODE)
                .header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce)
                .header("X-Body-SHA256", bodyHash)
                .header("X-Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (traceId != null) {
            builder.header("X-Trace-Id", traceId);
        }
        if (idempotencyKey != null) {
            builder.header("X-Idempotency-Key", idempotencyKey);
        }
        return send(builder.build());
    }

    protected OpenApiResponse postOpenWithTraceparent(
            String path,
            String body,
            String traceparent,
            String nonce,
            String idempotencyKey
    ) throws Exception {
        String timestamp = Instant.now().toString();
        String bodyHash = OpenApiCrypto.sha256Hex(body.getBytes(StandardCharsets.UTF_8));
        String signature = signature("POST", path, timestamp, nonce, idempotencyKey, bodyHash);
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .header("traceparent", traceparent)
                .header("X-Client-Code", CLIENT_CODE)
                .header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce)
                .header("X-Body-SHA256", bodyHash)
                .header("X-Idempotency-Key", idempotencyKey)
                .header("X-Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return send(request);
    }

    protected OpenApiResponse getOpen(String path, String traceId, String nonce) throws Exception {
        String timestamp = Instant.now().toString();
        String bodyHash = OpenApiCrypto.sha256Hex(new byte[0]);
        String signature = signature("GET", path, timestamp, nonce, null, bodyHash);
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("X-Trace-Id", traceId)
                .header("X-Client-Code", CLIENT_CODE)
                .header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce)
                .header("X-Body-SHA256", bodyHash)
                .header("X-Signature", signature)
                .GET()
                .build();
        return send(request);
    }

    protected void assertSuccess(OpenApiResponse response, int status, String traceId) {
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.headerTraceId()).isEqualTo(traceId);
        assertThat(response.body().at("/code").asText()).isEqualTo("0");
        assertThat(response.body().at("/trace_id").asText()).isEqualTo(traceId);
    }

    protected void assertError(OpenApiResponse response, int status, String code, String traceId) {
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.headerTraceId()).isEqualTo(traceId);
        assertThat(response.body().at("/code").asText()).isEqualTo(code);
        assertThat(response.body().at("/trace_id").asText()).isEqualTo(traceId);
    }

    protected String nonce() {
        return "nonce-" + UUID.randomUUID().toString().replace("-", "");
    }

    protected String idempotencyKey() {
        return "idem-" + UUID.randomUUID().toString().replace("-", "");
    }

    protected long countOpenApiLog(String traceId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM open_api_log WHERE trace_id = ?",
                Long.class,
                traceId);
        return count == null ? 0 : count;
    }

    protected String queryString(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, String.class, args);
    }

    protected int queryInt(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private OpenApiResponse send(HttpRequest request) throws Exception {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return new OpenApiResponse(
                response.statusCode(),
                response.headers().firstValue("X-Trace-Id").orElse(null),
                objectMapper.readTree(response.body()));
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private String signature(
            String method,
            String path,
            String timestamp,
            String nonce,
            String idempotencyKey,
            String bodyHash
    ) {
        String signingString = method + "\n"
                + path + "\n"
                + CLIENT_CODE + "\n"
                + timestamp + "\n"
                + nonce + "\n"
                + (idempotencyKey == null ? "" : idempotencyKey) + "\n"
                + bodyHash;
        return OpenApiCrypto.hmacSha256Hex(signingString, SECRET_HASH);
    }

    protected record OpenApiResponse(int status, String headerTraceId, JsonNode body) {
    }
}
