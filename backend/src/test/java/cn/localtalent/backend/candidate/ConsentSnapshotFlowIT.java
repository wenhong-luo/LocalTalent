package cn.localtalent.backend.candidate;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
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
                "localtalent.auth.jwt.secret=candidate-flow-secret-change-me-with-enough-length",
                "localtalent.auth.jwt.ttl-seconds=3600"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConsentSnapshotFlowIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_candidate_flow_test")
            .withUsername("localtalent")
            .withPassword("localtalent");

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    void consentShouldPublishSnapshotAndRevokeShouldOfflineItWithAuditAndIdempotency() throws Exception {
        CandidateAccount candidate = registerAndLoginCandidate();
        insertResumeFixture(candidate.userId());

        String consentBody = """
                {
                  "consent_scope": ["talent_service"],
                  "consent_version": "v1.0",
                  "realname_verified": true,
                  "second_confirmed": true
                }
                """;
        HttpJsonResponse consentResponse = postJson(
                "/api/consents",
                consentBody,
                "trace-consent-create",
                "Bearer " + candidate.token(),
                "idem-consent-001");
        assertSuccess(consentResponse, 200, "trace-consent-create");
        long consentId = consentResponse.body().at("/data/consent_id").asLong();
        long snapshotId = consentResponse.body().at("/data/snapshot_id").asLong();
        assertThat(consentId).isPositive();
        assertThat(snapshotId).isPositive();

        assertThat(countRows("candidate_consent", "candidate_id = " + candidate.userId())).isEqualTo(1);
        assertThat(countRows("candidate_publish_snapshot", "candidate_id = " + candidate.userId())).isEqualTo(1);
        assertThat(queryInt(
                "SELECT publishable_flag FROM candidate_control_profile WHERE candidate_id = ?",
                candidate.userId())).isEqualTo(1);

        HttpJsonResponse repeatedConsentResponse = postJson(
                "/api/consents",
                consentBody,
                "trace-consent-repeat",
                "Bearer " + candidate.token(),
                "idem-consent-001");
        assertSuccess(repeatedConsentResponse, 200, "trace-consent-repeat");
        assertThat(repeatedConsentResponse.body().at("/data/consent_id").asLong()).isEqualTo(consentId);
        assertThat(countRows("candidate_consent", "candidate_id = " + candidate.userId())).isEqualTo(1);

        HttpJsonResponse conflictResponse = postJson(
                "/api/consents",
                consentBody.replace("v1.0", "v1.1"),
                "trace-consent-conflict",
                "Bearer " + candidate.token(),
                "idem-consent-001");
        assertError(conflictResponse, 409, "IDEMPOTENCY_409", "trace-consent-conflict");

        HttpJsonResponse portalVisibleResponse = getJson(
                "/api/portal/talent-snapshots?city_code=310000&category_code=software&page=1&size=10",
                "trace-portal-visible",
                null);
        assertSuccess(portalVisibleResponse, 200, "trace-portal-visible");
        assertThat(portalVisibleResponse.body().at("/data/total").asLong()).isEqualTo(1);
        assertThat(portalVisibleResponse.body().at("/data/snapshot_list/0/snapshot_id").asLong()).isEqualTo(snapshotId);
        assertThat(portalVisibleResponse.body().at("/data/snapshot_list/0/display_name_masked").asText()).isEqualTo("候*");
        assertThat(portalVisibleResponse.body().toString())
                .doesNotContain("13812345678")
                .doesNotContain("candidate-flow@example.com")
                .doesNotContain("private/resume.pdf");

        assertThat(countAudit("trace-consent-create", "candidate_consent", "consent_submit")).isEqualTo(1);
        assertThat(countAudit("trace-consent-create", "candidate_publish_snapshot", "snapshot_publish")).isEqualTo(1);
        assertThat(countFieldAccess("trace-consent-create", candidate.userId())).isGreaterThanOrEqualTo(2);

        String revokeBody = """
                {
                  "reason": "candidate request"
                }
                """;
        HttpJsonResponse revokeResponse = postJson(
                "/api/consents/" + consentId + "/revoke",
                revokeBody,
                "trace-consent-revoke",
                "Bearer " + candidate.token(),
                "idem-revoke-001");
        assertSuccess(revokeResponse, 200, "trace-consent-revoke");
        assertThat(revokeResponse.body().at("/data/revoke_status").asInt()).isEqualTo(1);
        assertThat(queryInt(
                "SELECT publishable_flag FROM candidate_control_profile WHERE candidate_id = ?",
                candidate.userId())).isEqualTo(0);

        HttpJsonResponse portalHiddenResponse = getJson(
                "/api/portal/talent-snapshots?city_code=310000&category_code=software&page=1&size=10",
                "trace-portal-hidden",
                null);
        assertSuccess(portalHiddenResponse, 200, "trace-portal-hidden");
        assertThat(portalHiddenResponse.body().at("/data/total").asLong()).isZero();

        HttpJsonResponse repeatedRevokeResponse = postJson(
                "/api/consents/" + consentId + "/revoke",
                revokeBody,
                "trace-revoke-repeat",
                "Bearer " + candidate.token(),
                "idem-revoke-001");
        assertSuccess(repeatedRevokeResponse, 200, "trace-revoke-repeat");
        assertThat(repeatedRevokeResponse.body().at("/data/consent_id").asLong()).isEqualTo(consentId);
        assertThat(countAudit("trace-consent-revoke", "candidate_consent", "consent_revoke")).isEqualTo(1);
        assertThat(countAudit("trace-consent-revoke", "candidate_publish_snapshot", "snapshot_offline")).isEqualTo(1);
    }

    private CandidateAccount registerAndLoginCandidate() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "candidate-flow-" + suffix + "@example.com";
        String password = "Candidate@123456";
        HttpJsonResponse registerResponse = postJson(
                "/api/auth/register",
                """
                        {
                          "identity_type": "candidate",
                          "email": "%s",
                          "password": "%s",
                          "display_name": "候选人一"
                        }
                        """.formatted(email, password),
                "trace-flow-register",
                null,
                null);
        assertSuccess(registerResponse, 200, "trace-flow-register");
        long userId = registerResponse.body().at("/data/identity/user_id").asLong();

        HttpJsonResponse loginResponse = postJson(
                "/api/auth/login",
                """
                        {
                          "identity_type": "candidate",
                          "account": "%s",
                          "password": "%s"
                        }
                        """.formatted(email, password),
                "trace-flow-login",
                null,
                null);
        assertSuccess(loginResponse, 200, "trace-flow-login");
        return new CandidateAccount(userId, loginResponse.body().at("/data/access_token").asText());
    }

    private void insertResumeFixture(long candidateId) {
        jdbcTemplate.update(
                "INSERT INTO candidate_resume "
                        + "(candidate_id, resume_name, base_profile_json, skills_json, attachment_object_key, status) "
                        + "VALUES (?, 'Prompt4 fixture', ?, ?, 'private/resume.pdf', 1)",
                candidateId,
                """
                        {
                          "city_code": "310000",
                          "category_code": "software",
                          "experience_years": 5,
                          "resume_body": "raw private body"
                        }
                        """,
                """
                        ["Java", "Spring Boot", "MySQL"]
                        """);
    }

    private HttpJsonResponse postJson(
            String path,
            String body,
            String traceId,
            String authorization,
            String idempotencyKey
    ) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .header("X-Trace-Id", traceId)
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        if (idempotencyKey != null) {
            builder.header("X-Idempotency-Key", idempotencyKey);
        }
        return send(builder.build());
    }

    private HttpJsonResponse getJson(String path, String traceId, String authorization) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("X-Trace-Id", traceId)
                .GET();
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        return send(builder.build());
    }

    private HttpJsonResponse send(HttpRequest request) throws Exception {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return new HttpJsonResponse(
                response.statusCode(),
                response.headers().firstValue("X-Trace-Id").orElse(null),
                objectMapper.readTree(response.body()));
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private void assertSuccess(HttpJsonResponse response, int expectedStatus, String traceId) {
        assertThat(response.status()).isEqualTo(expectedStatus);
        assertThat(response.traceId()).isEqualTo(traceId);
        assertThat(response.body().at("/code").asText()).isEqualTo("0");
        assertThat(response.body().at("/trace_id").asText()).isEqualTo(traceId);
    }

    private void assertError(HttpJsonResponse response, int expectedStatus, String code, String traceId) {
        assertThat(response.status()).isEqualTo(expectedStatus);
        assertThat(response.traceId()).isEqualTo(traceId);
        assertThat(response.body().at("/code").asText()).isEqualTo(code);
        assertThat(response.body().at("/trace_id").asText()).isEqualTo(traceId);
    }

    private int countRows(String tableName, String whereClause) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE " + whereClause,
                Integer.class);
        return count == null ? 0 : count;
    }

    private int queryInt(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private int countAudit(String traceId, String bizType, String actionType) {
        return queryInt(
                "SELECT COUNT(*) FROM audit_log WHERE trace_id = ? AND biz_type = ? AND action_type = ?",
                traceId,
                bizType,
                actionType);
    }

    private int countFieldAccess(String traceId, long candidateId) {
        return queryInt(
                "SELECT COUNT(*) FROM field_access_log WHERE trace_id = ? AND biz_id = ? "
                        + "AND field_name IN ('real_name', 'base_profile_json', 'skills_json')",
                traceId,
                candidateId);
    }

    private record CandidateAccount(long userId, String token) {
    }

    private record HttpJsonResponse(int status, String traceId, JsonNode body) {
    }
}
