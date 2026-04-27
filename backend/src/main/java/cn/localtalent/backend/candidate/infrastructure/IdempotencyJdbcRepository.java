package cn.localtalent.backend.candidate.infrastructure;

import cn.localtalent.backend.authz.AuthzPrincipal;
import cn.localtalent.backend.candidate.domain.IdempotencyStoredResponse;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IdempotencyJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public IdempotencyJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<IdempotencyStoredResponse> find(
            String apiCode,
            AuthzPrincipal principal,
            String idempotencyKey
    ) {
        return jdbcTemplate.query(
                "SELECT request_hash, response_json "
                        + "FROM api_idempotency_record "
                        + "WHERE api_code = ? AND principal_type = ? AND principal_id = ? AND idempotency_key = ? "
                        + "LIMIT 1",
                (rs, rowNum) -> new IdempotencyStoredResponse(
                        rs.getString("request_hash"),
                        rs.getString("response_json")),
                apiCode,
                principal.identityType().value(),
                principal.userId(),
                idempotencyKey)
                .stream()
                .findFirst();
    }

    public void recordSuccess(
            String apiCode,
            AuthzPrincipal principal,
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
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)",
                apiCode,
                principal.identityType().value(),
                principal.userId(),
                idempotencyKey,
                requestHash,
                responseJson,
                resourceType,
                resourceId);
    }
}
