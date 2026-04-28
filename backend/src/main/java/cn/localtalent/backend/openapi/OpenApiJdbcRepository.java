package cn.localtalent.backend.openapi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class OpenApiJdbcRepository {

    private static final TypeReference<Set<String>> SCOPE_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public OpenApiJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public Optional<OpenApiClient> findClient(String clientCode) {
        return jdbcTemplate.query(
                "SELECT id, client_code, client_secret_hash, source_system, api_scope_json, status "
                        + "FROM open_client WHERE client_code = ? LIMIT 1",
                (rs, rowNum) -> new OpenApiClient(
                        rs.getLong("id"),
                        rs.getString("client_code"),
                        rs.getString("client_secret_hash"),
                        rs.getString("source_system"),
                        readScopes(rs.getString("api_scope_json")),
                        rs.getInt("status")),
                clientCode)
                .stream()
                .findFirst();
    }

    public boolean recordNonce(String clientCode, String nonce, LocalDateTime timestampAt) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO open_api_nonce_record (client_code, nonce, timestamp_at) VALUES (?, ?, ?)",
                    clientCode,
                    nonce,
                    timestampAt);
            return true;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }

    public void touchClient(String clientCode) {
        jdbcTemplate.update(
                "UPDATE open_client SET last_call_at = CURRENT_TIMESTAMP WHERE client_code = ?",
                clientCode);
    }

    public long insertOpenApiLog(
            String sourceSystem,
            String clientCode,
            String apiCode,
            String traceId,
            String requestUri,
            String requestMethod,
            String requestHash,
            String idempotencyKey,
            String requestSummaryJson
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO open_api_log "
                            + "(source_system, client_code, api_code, trace_id, request_uri, request_method, "
                            + "request_hash, idempotency_key, http_status, success_flag, request_summary_json) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 200, 1, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, sourceSystem);
            ps.setString(2, clientCode);
            ps.setString(3, apiCode);
            ps.setString(4, traceId);
            ps.setString(5, requestUri);
            ps.setString(6, requestMethod);
            ps.setString(7, requestHash);
            ps.setString(8, idempotencyKey);
            ps.setString(9, requestSummaryJson);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("failed to create open_api_log");
        }
        return key.longValue();
    }

    public void finishOpenApiLog(
            long logId,
            int httpStatus,
            boolean success,
            long durationMs,
            String errorMessage,
            String responseSummaryJson
    ) {
        jdbcTemplate.update(
                "UPDATE open_api_log "
                        + "SET http_status = ?, success_flag = ?, response_time = CURRENT_TIMESTAMP, "
                        + "duration_ms = ?, error_msg = ?, response_summary_json = ? "
                        + "WHERE id = ?",
                httpStatus,
                success ? 1 : 0,
                durationMs,
                limit(errorMessage, 500),
                responseSummaryJson,
                logId);
    }

    public Optional<String> findIdempotentResponse(
            String apiCode,
            OpenApiClient client,
            String idempotencyKey,
            String requestHash
    ) {
        return jdbcTemplate.query(
                "SELECT request_hash, response_json FROM api_idempotency_record "
                        + "WHERE api_code = ? AND principal_type = 'open_client' AND principal_id = ? "
                        + "AND idempotency_key = ? LIMIT 1",
                (rs, rowNum) -> {
                    String storedHash = rs.getString("request_hash");
                    if (!storedHash.equals(requestHash)) {
                        return "__HASH_MISMATCH__";
                    }
                    return rs.getString("response_json");
                },
                apiCode,
                client.id(),
                idempotencyKey)
                .stream()
                .findFirst();
    }

    public void recordIdempotentResponse(
            String apiCode,
            OpenApiClient client,
            String idempotencyKey,
            String requestHash,
            String responseJson,
            String resourceType,
            long resourceId
    ) {
        jdbcTemplate.update(
                "INSERT INTO api_idempotency_record "
                        + "(api_code, principal_type, principal_id, idempotency_key, request_hash, response_json, "
                        + "resource_type, resource_id, status) "
                        + "VALUES (?, 'open_client', ?, ?, ?, ?, ?, ?, 1)",
                apiCode,
                client.id(),
                idempotencyKey,
                requestHash,
                responseJson,
                resourceType,
                resourceId);
    }

    public long createRetryTask(
            String traceId,
            String apiCode,
            long openApiLogId,
            OpenApiClient client,
            String requestHash,
            String errorMessage
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        String payload = writeJson(Map.of(
                "stub", true,
                "request_hash", requestHash,
                "reason", "fault_injection_retry"));
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO integration_sync_task "
                            + "(trace_id, api_code, open_api_log_id, source_system, target_system, biz_type, biz_id, "
                            + "payload_json, sync_status, retry_count, max_retry_count, sync_version, "
                            + "next_retry_time, error_msg) "
                            + "VALUES (?, ?, ?, ?, 'localtalent', ?, ?, ?, 0, 0, 4, 1, "
                            + "DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 1 MINUTE), ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, traceId);
            ps.setString(2, apiCode);
            ps.setLong(3, openApiLogId);
            ps.setString(4, client.sourceSystem());
            ps.setString(5, apiCode);
            ps.setLong(6, openApiLogId);
            ps.setString(7, payload);
            ps.setString(8, limit(errorMessage, 500));
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("failed to create integration_sync_task");
        }
        return key.longValue();
    }

    public int retryDueTasks(LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE integration_sync_task "
                        + "SET retry_count = retry_count + 1, last_sync_time = ?, "
                        + "sync_status = CASE WHEN retry_count + 1 >= max_retry_count THEN 3 ELSE 0 END, "
                        + "next_retry_time = CASE "
                        + "WHEN retry_count + 1 >= max_retry_count THEN NULL "
                        + "WHEN retry_count + 1 = 1 THEN DATE_ADD(?, INTERVAL 5 MINUTE) "
                        + "WHEN retry_count + 1 = 2 THEN DATE_ADD(?, INTERVAL 15 MINUTE) "
                        + "ELSE DATE_ADD(?, INTERVAL 60 MINUTE) END, "
                        + "error_msg = CASE WHEN retry_count + 1 >= max_retry_count "
                        + "THEN 'stub retry max attempts reached' ELSE 'stub retry scheduled' END "
                        + "WHERE sync_status = 0 AND next_retry_time <= ?",
                now,
                now,
                now,
                now,
                now);
    }

    private Set<String> readScopes(String json) {
        try {
            return new TreeSet<>(objectMapper.readValue(json, SCOPE_TYPE));
        } catch (Exception exception) {
            throw new IllegalStateException("invalid open_client api_scope_json", exception);
        }
    }

    public String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to write open api json", exception);
        }
    }

    public String safeSummary(
            String clientCode,
            String requestHash,
            String idempotencyKey,
            boolean hasBody
    ) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("client_code", clientCode);
        summary.put("request_hash", requestHash);
        summary.put("idempotency_key_present", idempotencyKey != null && !idempotencyKey.isBlank());
        summary.put("has_body", hasBody);
        return writeJson(summary);
    }

    public String responseSummary(int status, String code) {
        return writeJson(Map.of("http_status", status, "code", code));
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
