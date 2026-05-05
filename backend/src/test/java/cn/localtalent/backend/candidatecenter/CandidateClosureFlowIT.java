package cn.localtalent.backend.candidatecenter;

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
                "localtalent.auth.jwt.secret=candidate-closure-flow-secret-change-me",
                "localtalent.auth.jwt.ttl-seconds=3600",
                "localtalent.phase3.candidate-closure=true"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CandidateClosureFlowIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_candidate_closure_flow_test")
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
    void candidateClosureShouldSupportPrivateFlowWithIdorAuditAndRevokePropagation() throws Exception {
        CandidateAccount candidate = registerAndLoginCandidate("main");
        CandidateAccount otherCandidate = registerAndLoginCandidate("other");
        long jobId = insertVisibleJob();

        HttpJsonResponse overview = getJson(
                "/api/candidate/center/overview",
                "trace-p3-closure-overview",
                "Bearer " + candidate.token());
        assertSuccess(overview, 200, "trace-p3-closure-overview");
        assertThat(overview.body().at("/data/features/candidate_closure_enabled").asBoolean()).isTrue();
        assertThat(overview.body().at("/data/stats/favorite_count").asLong()).isZero();

        String resumeBody = resumeBody("三期闭环简历", "候选人三期", "Java, Spring");
        HttpJsonResponse savedResume = putJson(
                "/api/candidate/center/resume",
                resumeBody,
                "trace-p3-resume-save",
                "Bearer " + candidate.token(),
                "idem-p3-resume-001");
        assertSuccess(savedResume, 200, "trace-p3-resume-save");
        assertThat(savedResume.body().at("/data/resume_status").asText()).isEqualTo("complete");
        assertThat(savedResume.body().at("/data/base_profile/display_name").asText()).isEqualTo("候选人三期");
        assertNoRawCandidateData(savedResume);

        HttpJsonResponse repeatedResume = putJson(
                "/api/candidate/center/resume",
                resumeBody,
                "trace-p3-resume-repeat",
                "Bearer " + candidate.token(),
                "idem-p3-resume-001");
        assertSuccess(repeatedResume, 200, "trace-p3-resume-repeat");
        HttpJsonResponse conflictResume = putJson(
                "/api/candidate/center/resume",
                resumeBody("三期闭环简历 v2", "候选人三期", "Java"),
                "trace-p3-resume-conflict",
                "Bearer " + candidate.token(),
                "idem-p3-resume-001");
        assertError(conflictResume, 409, "IDEMPOTENCY_409", "trace-p3-resume-conflict");

        long resumeId = savedResume.body().at("/data/resume_id").asLong();
        insertApplication(candidate.userId(), jobId, resumeId);
        HttpJsonResponse applications = getJson(
                "/api/candidate/center/applications?page=1&size=20",
                "trace-p3-applications",
                "Bearer " + candidate.token());
        assertSuccess(applications, 200, "trace-p3-applications");
        assertThat(applications.body().at("/data/total").asLong()).isEqualTo(1);
        assertThat(applications.body().at("/data/application_list/0/job_title").asText()).isEqualTo("P3 Java 工程师");

        HttpJsonResponse favorites = postJson(
                "/api/candidate/center/favorites",
                """
                        {
                          "job_id": %d
                        }
                        """.formatted(jobId),
                "trace-p3-favorite-create",
                "Bearer " + candidate.token(),
                "idem-p3-favorite-001");
        assertSuccess(favorites, 200, "trace-p3-favorite-create");
        long favoriteId = favorites.body().at("/data/favorite_list/0/favorite_id").asLong();
        assertThat(favoriteId).isPositive();

        HttpJsonResponse subscription = postJson(
                "/api/candidate/center/subscriptions",
                """
                        {
                          "subscription_name": "后端职位订阅",
                          "keyword": "Java",
                          "city_code": "310000",
                          "category_code": "software",
                          "salary_min": 15000,
                          "salary_max": 30000
                        }
                        """,
                "trace-p3-subscription-create",
                "Bearer " + candidate.token(),
                "idem-p3-subscription-001");
        assertSuccess(subscription, 200, "trace-p3-subscription-create");
        long subscriptionId = subscription.body().at("/data/subscription_list/0/subscription_id").asLong();
        assertThat(subscriptionId).isPositive();

        HttpJsonResponse notifications = getJson(
                "/api/candidate/center/notifications?page=1&size=20",
                "trace-p3-notifications",
                "Bearer " + candidate.token());
        assertSuccess(notifications, 200, "trace-p3-notifications");
        long notificationId = notifications.body().at("/data/notification_list/0/notification_id").asLong();
        assertThat(notifications.body().at("/data/notification_list/0/read_status").asText()).isEqualTo("unread");

        HttpJsonResponse readNotification = postJson(
                "/api/candidate/center/notifications/" + notificationId + "/read",
                "{}",
                "trace-p3-notification-read",
                "Bearer " + candidate.token(),
                "idem-p3-notification-001");
        assertSuccess(readNotification, 200, "trace-p3-notification-read");
        assertThat(readNotification.body().at("/data/notification_list/0/read_status").asText()).isEqualTo("read");

        HttpJsonResponse otherCancelsSubscription = postJson(
                "/api/candidate/center/subscriptions/" + subscriptionId + "/cancel",
                "{}",
                "trace-p3-other-subscription-cancel",
                "Bearer " + otherCandidate.token(),
                "idem-p3-other-subscription-001");
        assertError(otherCancelsSubscription, 404, "NOT_FOUND_404", "trace-p3-other-subscription-cancel");

        HttpJsonResponse otherReadsNotification = postJson(
                "/api/candidate/center/notifications/" + notificationId + "/read",
                "{}",
                "trace-p3-other-notification-read",
                "Bearer " + otherCandidate.token(),
                "idem-p3-other-notification-001");
        assertError(otherReadsNotification, 404, "NOT_FOUND_404", "trace-p3-other-notification-read");

        HttpJsonResponse cancelFavorite = postJson(
                "/api/candidate/center/favorites/" + favoriteId + "/cancel",
                "{}",
                "trace-p3-favorite-cancel",
                "Bearer " + candidate.token(),
                "idem-p3-favorite-cancel-001");
        assertSuccess(cancelFavorite, 200, "trace-p3-favorite-cancel");
        assertThat(cancelFavorite.body().at("/data/total").asLong()).isZero();

        HttpJsonResponse companyDenied = getJson(
                "/api/candidate/center/resume",
                "trace-p3-company-denied",
                "Bearer " + registerAndLoginCompany());
        assertError(companyDenied, 403, "AUTHZ_403", "trace-p3-company-denied");

        String operatorToken = login("operator", "operator", "LocalTalent@123456", "trace-p3-operator-login");
        HttpJsonResponse operatorDenied = getJson(
                "/api/candidate/center/resume",
                "trace-p3-operator-denied",
                "Bearer " + operatorToken);
        assertError(operatorDenied, 403, "AUTHZ_403", "trace-p3-operator-denied");

        HttpJsonResponse consent = postJson(
                "/api/consents",
                """
                        {
                          "consent_scope": ["talent_service_area"],
                          "consent_version": "phase3-prompt27",
                          "realname_verified": true,
                          "second_confirmed": true
                        }
                        """,
                "trace-p3-consent",
                "Bearer " + candidate.token(),
                "idem-p3-consent-001");
        assertSuccess(consent, 200, "trace-p3-consent");
        long consentId = consent.body().at("/data/consent_id").asLong();
        HttpJsonResponse visible = getJson(
                "/api/portal/talent-snapshots?city_code=310000&category_code=software&page=1&size=10",
                "trace-p3-portal-visible",
                null);
        assertSuccess(visible, 200, "trace-p3-portal-visible");
        assertThat(visible.body().at("/data/total").asLong()).isEqualTo(1);
        assertNoRawCandidateData(visible);

        HttpJsonResponse revoke = postJson(
                "/api/consents/" + consentId + "/revoke",
                """
                        {
                          "reason": "phase3 closure regression"
                        }
                        """,
                "trace-p3-revoke",
                "Bearer " + candidate.token(),
                "idem-p3-revoke-001");
        assertSuccess(revoke, 200, "trace-p3-revoke");
        HttpJsonResponse hidden = getJson(
                "/api/portal/talent-snapshots?city_code=310000&category_code=software&page=1&size=10",
                "trace-p3-portal-hidden",
                null);
        assertSuccess(hidden, 200, "trace-p3-portal-hidden");
        assertThat(hidden.body().at("/data/total").asLong()).isZero();

        assertThat(countRows("audit_log", "action_type IN ('resume_save','favorite_create','favorite_cancel','subscription_create','notification_read')"))
                .isGreaterThanOrEqualTo(5);
        assertThat(countRows("field_access_log", "biz_type = 'candidate_resume' AND operator_id = " + candidate.userId()))
                .isGreaterThanOrEqualTo(4);
    }

    private CandidateAccount registerAndLoginCandidate(String label) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "p3-candidate-" + label + "-" + suffix + "@example.com";
        String password = "Candidate@123456";
        HttpJsonResponse register = postJson(
                "/api/auth/register",
                """
                        {
                          "identity_type": "candidate",
                          "email": "%s",
                          "password": "%s",
                          "display_name": "三期求职者"
                        }
                        """.formatted(email, password),
                "trace-p3-register-" + label,
                null,
                null);
        assertSuccess(register, 200, "trace-p3-register-" + label);
        return new CandidateAccount(
                register.body().at("/data/identity/user_id").asLong(),
                login("candidate", email, password, "trace-p3-login-" + label));
    }

    private String registerAndLoginCompany() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "p3-company-" + suffix + "@example.com";
        String password = "Company@123456";
        postJson(
                "/api/auth/register",
                """
                        {
                          "identity_type": "company",
                          "company_name": "P3 企业",
                          "license_no": "P3-C-%s",
                          "user_name": "企业管理员",
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(suffix.substring(0, 16), email, password),
                "trace-p3-company-register",
                null,
                null);
        return login("company", email, password, "trace-p3-company-login");
    }

    private String login(String identityType, String account, String password, String traceId) throws Exception {
        HttpJsonResponse response = postJson(
                "/api/auth/login",
                """
                        {
                          "identity_type": "%s",
                          "account": "%s",
                          "password": "%s"
                        }
                        """.formatted(identityType, account, password),
                traceId,
                null,
                null);
        assertSuccess(response, 200, traceId);
        return response.body().at("/data/access_token").asText();
    }

    private long insertVisibleJob() {
        long companyId = insertWithKey(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO company (company_name, license_no, auth_status, city_code, industry_code, source_system) "
                            + "VALUES ('P3 认证企业', ?, 2, '310000', 'software', 'portal')",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, "P3-J-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            return statement;
        });
        return insertWithKey(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO job_post "
                            + "(company_id, title, category_code, city_code, salary_min, salary_max, job_desc, status, audit_status) "
                            + "VALUES (?, 'P3 Java 工程师', 'software', '310000', 15000, 30000, 'P3 prompt27 public job', 2, 2)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, companyId);
            return statement;
        });
    }

    private void insertApplication(long candidateId, long jobId, long resumeId) {
        jdbcTemplate.update(
                "INSERT INTO job_application (job_id, candidate_id, resume_id, source_type, application_status) "
                        + "VALUES (?, ?, ?, 1, 2)",
                jobId,
                candidateId,
                resumeId);
    }

    private String resumeBody(String resumeName, String displayName, String skills) {
        return """
                {
                  "resume_name": "%s",
                  "base_profile": {
                    "display_name": "%s",
                    "city_code": "310000",
                    "category_code": "software",
                    "experience_years": 5,
                    "summary": "本人私有摘要"
                  },
                  "education": ["本科"],
                  "experience": ["后端服务建设"],
                  "skills": ["%s"]
                }
                """.formatted(resumeName, displayName, skills);
    }

    private long insertWithKey(StatementFactory statementFactory) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> statementFactory.create(connection), keyHolder);
        Number key = keyHolder.getKey();
        assertThat(key).isNotNull();
        return key.longValue();
    }

    private int countRows(String tableName, String where) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName + " WHERE " + where, Integer.class);
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

    private HttpJsonResponse putJson(
            String path,
            String body,
            String traceId,
            String authorization,
            String idempotencyKey
    ) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .header("X-Trace-Id", traceId)
                .PUT(HttpRequest.BodyPublishers.ofString(body));
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

    private void assertNoRawCandidateData(HttpJsonResponse response) {
        assertThat(response.body().toString())
                .doesNotContain("mobile")
                .doesNotContain("email")
                .doesNotContain("password_hash")
                .doesNotContain("resume_body")
                .doesNotContain("attachment_object_key")
                .doesNotContain("evidence")
                .doesNotContain("base_profile_json")
                .doesNotContain("education_json")
                .doesNotContain("experience_json")
                .doesNotContain("skills_json");
    }

    private interface StatementFactory {
        PreparedStatement create(java.sql.Connection connection) throws java.sql.SQLException;
    }

    private record CandidateAccount(long userId, String token) {
    }

    private record HttpJsonResponse(int status, String traceId, JsonNode body) {
    }
}
