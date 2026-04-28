package cn.localtalent.backend.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "localtalent.auth.jwt.secret=prompt9-open-api-secret-change-me-with-enough-length",
                "localtalent.openapi.timestamp-window-seconds=300"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OpenApiContractTest extends OpenApiTestSupport {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_openapi_contract_test")
            .withUsername("localtalent")
            .withPassword("localtalent");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    void signedRequestIdempotencyTraceparentAndLogShouldWork() throws Exception {
        String body = """
                {
                  "source_system": "stub_partner",
                  "external_job_id": "P9-JOB-001",
                  "sync_version": 1,
                  "status": "stub_only"
                }
                """;
        String idem = idempotencyKey();
        OpenApiResponse response = postOpen(
                "/api/open/v1/jobs/sync",
                body,
                "trace-p9-contract-ok",
                nonce(),
                idem);
        assertSuccess(response, 200, "trace-p9-contract-ok");
        assertThat(response.body().at("/data/accepted").asBoolean()).isTrue();
        assertThat(response.body().at("/data/stub").asBoolean()).isTrue();
        assertThat(response.body().at("/data/api_code").asText()).isEqualTo("open.jobs.sync");

        assertThat(countOpenApiLog("trace-p9-contract-ok")).isEqualTo(1);
        assertThat(queryInt(
                "SELECT COUNT(*) FROM open_api_log "
                        + "WHERE trace_id = ? AND client_code = ? AND request_hash IS NOT NULL "
                        + "AND duration_ms IS NOT NULL AND request_summary_json IS NOT NULL",
                "trace-p9-contract-ok",
                CLIENT_CODE)).isEqualTo(1);

        OpenApiResponse idempotentReplay = postOpen(
                "/api/open/v1/jobs/sync",
                body,
                "trace-p9-idempotency-replay",
                nonce(),
                idem);
        assertSuccess(idempotentReplay, 200, "trace-p9-idempotency-replay");
        assertThat(idempotentReplay.body().at("/data/api_code").asText()).isEqualTo("open.jobs.sync");

        OpenApiResponse idempotentConflict = postOpen(
                "/api/open/v1/jobs/sync",
                """
                        {
                          "source_system": "stub_partner",
                          "external_job_id": "P9-JOB-002",
                          "sync_version": 2
                        }
                        """,
                "trace-p9-idempotency-conflict",
                nonce(),
                idem);
        assertError(idempotentConflict, 409, "IDEMPOTENCY_409", "trace-p9-idempotency-conflict");

        OpenApiResponse mapping = getOpen(
                "/api/open/v1/mappings/query?source_system=stub_partner&local_biz_type=job&local_id=100",
                "trace-p9-mapping-query",
                nonce());
        assertSuccess(mapping, 200, "trace-p9-mapping-query");
        assertThat(mapping.body().at("/data/found").asBoolean()).isFalse();

        String traceparentTraceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        OpenApiResponse traceparentResponse = postOpenWithTraceparent(
                "/api/open/v1/applications/sync",
                """
                        {
                          "source_system": "stub_partner",
                          "application_external_id": "P9-APP-001",
                          "status": "stub_only"
                        }
                        """,
                "00-" + traceparentTraceId + "-00f067aa0ba902b7-01",
                nonce(),
                idempotencyKey());
        assertThat(traceparentResponse.status()).isEqualTo(200);
        assertThat(traceparentResponse.headerTraceId()).isEqualTo(traceparentTraceId);
        assertThat(traceparentResponse.body().at("/trace_id").asText()).isEqualTo(traceparentTraceId);
    }

    @Test
    void invalidSignatureExpiredTimestampReplayAndSensitivePayloadShouldBeRejected() throws Exception {
        String body = """
                {
                  "source_system": "stub_partner",
                  "external_job_id": "P9-JOB-FAIL",
                  "sync_version": 1
                }
                """;

        OpenApiResponse invalidSignature = postOpen(
                "/api/open/v1/jobs/sync",
                body,
                "trace-p9-signature-invalid",
                nonce(),
                idempotencyKey(),
                Instant.now().toString(),
                true);
        assertError(invalidSignature, 401, "OPEN_SIGNATURE_401", "trace-p9-signature-invalid");

        OpenApiResponse expiredTimestamp = postOpen(
                "/api/open/v1/jobs/sync",
                body,
                "trace-p9-timestamp-expired",
                nonce(),
                idempotencyKey(),
                Instant.now().minusSeconds(600).toString(),
                false);
        assertError(expiredTimestamp, 401, "OPEN_TIMESTAMP_401", "trace-p9-timestamp-expired");

        String replayNonce = nonce();
        OpenApiResponse firstNonceUse = postOpen(
                "/api/open/v1/jobs/sync",
                body,
                "trace-p9-nonce-first",
                replayNonce,
                idempotencyKey());
        assertSuccess(firstNonceUse, 200, "trace-p9-nonce-first");
        OpenApiResponse nonceReplay = postOpen(
                "/api/open/v1/jobs/sync",
                body,
                "trace-p9-nonce-replay",
                replayNonce,
                idempotencyKey());
        assertError(nonceReplay, 409, "OPEN_REPLAY_409", "trace-p9-nonce-replay");

        OpenApiResponse missingIdempotency = postOpen(
                "/api/open/v1/jobs/sync",
                body,
                "trace-p9-idempotency-missing",
                nonce(),
                null);
        assertError(missingIdempotency, 400, "VALID_400", "trace-p9-idempotency-missing");

        OpenApiResponse sensitivePayload = postOpen(
                "/api/open/v1/candidates/publishable-sync",
                """
                        {
                          "source_system": "stub_partner",
                          "candidate_external_id": "P9-C-001",
                          "mobile": "13900009999",
                          "snapshot_json": {"city_code": "310000"}
                        }
                        """,
                "trace-p9-sensitive-payload",
                nonce(),
                idempotencyKey());
        assertError(sensitivePayload, 400, "OPEN_SENSITIVE_400", "trace-p9-sensitive-payload");

        String logText = queryString(
                "SELECT CONCAT("
                        + "COALESCE(request_summary_json, ''), '|', "
                        + "COALESCE(response_summary_json, ''), '|', "
                        + "COALESCE(error_msg, '')) "
                        + "FROM open_api_log WHERE trace_id = ?",
                "trace-p9-sensitive-payload");
        assertThat(logText)
                .as("open_api_log should not contain sensitive plaintext")
                .doesNotContain("13900009999")
                .doesNotContain("mobile");
    }
}
