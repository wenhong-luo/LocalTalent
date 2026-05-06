package cn.localtalent.backend.candidate;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
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
                "localtalent.auth.jwt.secret=phase3-withdrawal-propagation-secret-change-me",
                "localtalent.auth.jwt.ttl-seconds=3600",
                "localtalent.phase3.operator-portal-ops=true"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class Phase3ConsentWithdrawalPropagationIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_p30_withdrawal_test")
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
    void revokeShouldHideTalentSnapshotFromPortalAndRecommendationSlots() throws Exception {
        CandidateAccount candidate = registerAndLoginCandidate();
        insertResume(candidate.userId());

        HttpJsonResponse consent = postJson(
                "/api/consents",
                """
                        {
                          "consent_scope": ["talent_service_area"],
                          "consent_version": "phase3-p30",
                          "realname_verified": true,
                          "second_confirmed": true
                        }
                        """,
                "trace-p30-consent",
                "Bearer " + candidate.token(),
                "idem-p30-consent");
        assertSuccess(consent, 200, "trace-p30-consent");
        long consentId = consent.body().at("/data/consent_id").asLong();
        long snapshotId = consent.body().at("/data/snapshot_id").asLong();
        insertRecommendation(snapshotId);

        HttpJsonResponse visibleSnapshots = getJson(
                "/api/portal/talent-snapshots?city_code=310000&category_code=software&page=1&size=10",
                "trace-p30-snapshot-visible",
                null);
        assertSuccess(visibleSnapshots, 200, "trace-p30-snapshot-visible");
        assertThat(visibleSnapshots.body().at("/data/total").asLong()).isEqualTo(1);

        HttpJsonResponse visibleRecommendation = getJson(
                "/api/portal/recommendations?slot_code=home_talent&limit=8",
                "trace-p30-recommendation-visible",
                null);
        assertSuccess(visibleRecommendation, 200, "trace-p30-recommendation-visible");
        assertThat(visibleRecommendation.body().at("/data/total").asLong()).isEqualTo(1);
        assertThat(visibleRecommendation.body().toString())
                .contains("发布快照")
                .doesNotContain("mobile")
                .doesNotContain("email")
                .doesNotContain("resume_body")
                .doesNotContain("attachment_object_key");

        HttpJsonResponse revoke = postJson(
                "/api/consents/" + consentId + "/revoke",
                "{\"reason\":\"phase3 prompt30 revoke\"}",
                "trace-p30-consent-revoke",
                "Bearer " + candidate.token(),
                "idem-p30-revoke");
        assertSuccess(revoke, 200, "trace-p30-consent-revoke");

        HttpJsonResponse hiddenSnapshots = getJson(
                "/api/portal/talent-snapshots?city_code=310000&category_code=software&page=1&size=10",
                "trace-p30-snapshot-hidden",
                null);
        assertSuccess(hiddenSnapshots, 200, "trace-p30-snapshot-hidden");
        assertThat(hiddenSnapshots.body().at("/data/total").asLong()).isZero();

        HttpJsonResponse hiddenRecommendation = getJson(
                "/api/portal/recommendations?slot_code=home_talent&limit=8",
                "trace-p30-recommendation-hidden",
                null);
        assertSuccess(hiddenRecommendation, 200, "trace-p30-recommendation-hidden");
        assertThat(hiddenRecommendation.body().at("/data/total").asLong()).isZero();
        assertThat(countAudit("trace-p30-consent-revoke", "candidate_publish_snapshot", "snapshot_offline")).isEqualTo(1);
    }

    private CandidateAccount registerAndLoginCandidate() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "p30-candidate-" + suffix + "@example.com";
        String password = "Candidate@123456";
        HttpJsonResponse register = postJson(
                "/api/auth/register",
                """
                        {
                          "identity_type": "candidate",
                          "email": "%s",
                          "password": "%s",
                          "display_name": "撤回测试候选人"
                        }
                        """.formatted(email, password),
                "trace-p30-register",
                null,
                null);
        assertSuccess(register, 200, "trace-p30-register");
        long userId = register.body().at("/data/identity/user_id").asLong();
        HttpJsonResponse login = postJson(
                "/api/auth/login",
                """
                        {
                          "identity_type": "candidate",
                          "account": "%s",
                          "password": "%s"
                        }
                        """.formatted(email, password),
                "trace-p30-login",
                null,
                null);
        assertSuccess(login, 200, "trace-p30-login");
        return new CandidateAccount(userId, login.body().at("/data/access_token").asText());
    }

    private void insertResume(long candidateId) {
        jdbcTemplate.update(
                "INSERT INTO candidate_resume "
                        + "(candidate_id, resume_name, base_profile_json, skills_json, attachment_object_key, status) "
                        + "VALUES (?, 'P30 resume', ?, ?, 'private/p30.pdf', 1)",
                candidateId,
                """
                        {
                          "city_code": "310000",
                          "category_code": "software",
                          "experience_years": 6,
                          "resume_body": "raw private body"
                        }
                        """,
                "[\"Java\", \"Spring\"]");
    }

    private void insertRecommendation(long snapshotId) {
        jdbcTemplate.update(
                "INSERT INTO portal_recommendation "
                        + "(slot_code, target_type, target_id, title_override, summary_override, display_order, status) "
                        + "VALUES ('home_talent', 'talent_snapshot', ?, '', '', 1, 1)",
                snapshotId);
    }

    private long countAudit(String traceId, String bizType, String actionType) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE trace_id = ? AND biz_type = ? AND action_type = ?",
                Long.class,
                traceId,
                bizType,
                actionType);
        return count == null ? 0 : count;
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

    private void assertSuccess(HttpJsonResponse response, int status, String traceId) {
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.headerTraceId()).isEqualTo(traceId);
        assertThat(response.body().at("/code").asText()).isEqualTo("0");
        assertThat(response.body().at("/trace_id").asText()).isEqualTo(traceId);
    }

    private record CandidateAccount(long userId, String token) {
    }

    private record HttpJsonResponse(int status, String headerTraceId, JsonNode body) {
    }
}
