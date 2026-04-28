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
                "localtalent.auth.jwt.secret=candidate-center-secret-change-me-with-enough-length",
                "localtalent.auth.jwt.ttl-seconds=3600"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CandidateCenterOverviewIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_candidate_center_test")
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
    void overviewShouldBeServerDrivenAndStayConsistentWithPublishSnapshotVisibility() throws Exception {
        CandidateAccount candidate = registerAndLoginCandidate("overview");
        long resumeId = insertResumeFixture(candidate.userId());
        insertApplicationAndSigninFixture(candidate.userId(), resumeId);

        HttpJsonResponse initialOverview = getJson(
                "/api/candidate/center/overview",
                "trace-center-initial",
                "Bearer " + candidate.token());
        assertSuccess(initialOverview, 200, "trace-center-initial");
        assertThat(initialOverview.body().at("/data/resume/completion_percent").asInt()).isEqualTo(100);
        assertThat(initialOverview.body().at("/data/resume/skills_summary").asText())
                .isEqualTo("Java / Spring Boot / MySQL / Redis / MinIO");
        assertThat(initialOverview.body().at("/data/applications/total").asLong()).isEqualTo(1);
        assertThat(initialOverview.body().at("/data/applications/latest_status").asText()).isEqualTo("已签到");
        assertThat(initialOverview.body().at("/data/applications/latest_job_title").asText())
                .isEqualTo("Prompt12.5 后端工程师");
        assertThat(initialOverview.body().at("/data/signin/latest_status").asText()).isEqualTo("已签到");
        assertThat(initialOverview.body().at("/data/consent/publish_status").asText()).isEqualTo("not_publishable");
        assertNoRawCandidateData(initialOverview);

        String consentBody = """
                {
                  "consent_scope": ["talent_service_area"],
                  "consent_version": "phase1-v1",
                  "realname_verified": true,
                  "second_confirmed": true
                }
                """;
        HttpJsonResponse consentResponse = postJson(
                "/api/consents",
                consentBody,
                "trace-center-consent",
                "Bearer " + candidate.token(),
                "idem-center-consent");
        assertSuccess(consentResponse, 200, "trace-center-consent");
        long consentId = consentResponse.body().at("/data/consent_id").asLong();

        HttpJsonResponse consentedOverview = getJson(
                "/api/candidate/center/overview",
                "trace-center-consented",
                "Bearer " + candidate.token());
        assertSuccess(consentedOverview, 200, "trace-center-consented");
        assertThat(consentedOverview.body().at("/data/consent/consent_id").asLong()).isEqualTo(consentId);
        assertThat(consentedOverview.body().at("/data/consent/publish_status").asText()).isEqualTo("consented");
        assertThat(consentedOverview.body().at("/data/consent/publishable_flag").asInt()).isEqualTo(1);
        assertNoRawCandidateData(consentedOverview);

        HttpJsonResponse portalVisibleResponse = getJson(
                "/api/portal/talent-snapshots?city_code=310000&category_code=software&page=1&size=10",
                "trace-center-portal-visible",
                null);
        assertSuccess(portalVisibleResponse, 200, "trace-center-portal-visible");
        assertThat(portalVisibleResponse.body().at("/data/total").asLong()).isEqualTo(1);

        HttpJsonResponse revokeResponse = postJson(
                "/api/consents/" + consentId + "/revoke",
                """
                        {
                          "reason": "candidate center revoke"
                        }
                        """,
                "trace-center-revoke",
                "Bearer " + candidate.token(),
                "idem-center-revoke");
        assertSuccess(revokeResponse, 200, "trace-center-revoke");

        HttpJsonResponse revokedOverview = getJson(
                "/api/candidate/center/overview",
                "trace-center-revoked",
                "Bearer " + candidate.token());
        assertSuccess(revokedOverview, 200, "trace-center-revoked");
        assertThat(revokedOverview.body().at("/data/consent/publish_status").asText()).isEqualTo("revoked");
        assertThat(revokedOverview.body().at("/data/consent/publishable_flag").asInt()).isZero();
        assertNoRawCandidateData(revokedOverview);

        HttpJsonResponse portalHiddenResponse = getJson(
                "/api/portal/talent-snapshots?city_code=310000&category_code=software&page=1&size=10",
                "trace-center-portal-hidden",
                null);
        assertSuccess(portalHiddenResponse, 200, "trace-center-portal-hidden");
        assertThat(portalHiddenResponse.body().at("/data/total").asLong()).isZero();
    }

    @Test
    void overviewShouldRejectMissingTokenAndNonCandidateIdentities() throws Exception {
        HttpJsonResponse missingToken = getJson(
                "/api/candidate/center/overview",
                "trace-center-no-token",
                null);
        assertError(missingToken, 401, "AUTH_401", "trace-center-no-token");

        CandidateAccount company = registerAndLoginCompany("denied");
        HttpJsonResponse companyDenied = getJson(
                "/api/candidate/center/overview",
                "trace-center-company-denied",
                "Bearer " + company.token());
        assertError(companyDenied, 403, "AUTHZ_403", "trace-center-company-denied");

        String operatorToken = login("operator", "operator", "LocalTalent@123456", "trace-center-operator-login");
        HttpJsonResponse operatorDenied = getJson(
                "/api/candidate/center/overview",
                "trace-center-operator-denied",
                "Bearer " + operatorToken);
        assertError(operatorDenied, 403, "AUTHZ_403", "trace-center-operator-denied");

        String auditorUsername = insertAuditor();
        String auditorToken = login("operator", auditorUsername, "LocalTalent@123456", "trace-center-auditor-login");
        HttpJsonResponse auditorDenied = getJson(
                "/api/candidate/center/overview",
                "trace-center-auditor-denied",
                "Bearer " + auditorToken);
        assertError(auditorDenied, 403, "AUTHZ_403", "trace-center-auditor-denied");
    }

    private CandidateAccount registerAndLoginCandidate(String label) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "candidate-center-" + label + "-" + suffix + "@example.com";
        String password = "Candidate@123456";
        HttpJsonResponse registerResponse = postJson(
                "/api/auth/register",
                """
                        {
                          "identity_type": "candidate",
                          "email": "%s",
                          "password": "%s",
                          "display_name": "候选人中心"
                        }
                        """.formatted(email, password),
                "trace-center-candidate-register-" + label,
                null,
                null);
        assertSuccess(registerResponse, 200, "trace-center-candidate-register-" + label);
        long userId = registerResponse.body().at("/data/identity/user_id").asLong();
        String token = login("candidate", email, password, "trace-center-candidate-login-" + label);
        return new CandidateAccount(userId, token);
    }

    private CandidateAccount registerAndLoginCompany(String label) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "candidate-center-company-" + label + "-" + suffix + "@example.com";
        String password = "Company@123456";
        HttpJsonResponse registerResponse = postJson(
                "/api/auth/register",
                """
                        {
                          "identity_type": "company",
                          "company_name": "Prompt12.5 企业%s",
                          "license_no": "P125-C-%s",
                          "user_name": "企业管理员",
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(label, suffix.substring(0, 16), email, password),
                "trace-center-company-register-" + label,
                null,
                null);
        assertSuccess(registerResponse, 200, "trace-center-company-register-" + label);
        long userId = registerResponse.body().at("/data/identity/user_id").asLong();
        String token = login("company", email, password, "trace-center-company-login-" + label);
        return new CandidateAccount(userId, token);
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

    private long insertResumeFixture(long candidateId) {
        return insertWithKey(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO candidate_resume "
                            + "(candidate_id, resume_name, base_profile_json, education_json, experience_json, "
                            + "skills_json, attachment_object_key, status) "
                            + "VALUES (?, 'Prompt12.5 resume', ?, ?, ?, ?, 'private/center-resume.pdf', 1)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, candidateId);
            statement.setString(2, """
                    {
                      "city_code": "310000",
                      "category_code": "software",
                      "experience_years": 6,
                      "resume_body": "raw center private body"
                    }
                    """);
            statement.setString(3, """
                    [{"school": "raw education should not leak"}]
                    """);
            statement.setString(4, """
                    [{"company": "raw experience should not leak"}]
                    """);
            statement.setString(5, """
                    ["Java", "Spring Boot", "MySQL", "Redis", "MinIO", "Kubernetes"]
                    """);
            return statement;
        });
    }

    private void insertApplicationAndSigninFixture(long candidateId, long resumeId) {
        long companyId = insertWithKey(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO company (company_name, license_no, auth_status, source_system) "
                            + "VALUES ('Prompt12.5 招聘企业', ?, 2, 'portal')",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, "P125-J-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            return statement;
        });
        long jobId = insertWithKey(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO job_post "
                            + "(company_id, title, category_code, city_code, job_desc, status, audit_status) "
                            + "VALUES (?, 'Prompt12.5 后端工程师', 'software', '310000', 'Prompt12.5 fixture', 2, 2)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, companyId);
            return statement;
        });
        long applicationId = insertWithKey(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO job_application "
                            + "(job_id, candidate_id, resume_id, source_type, application_status) "
                            + "VALUES (?, ?, ?, 1, 3)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, jobId);
            statement.setLong(2, candidateId);
            statement.setLong(3, resumeId);
            return statement;
        });
        long sessionId = insertWithKey(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO interview_session "
                            + "(application_id, job_id, company_id, session_name, session_time, location, status) "
                            + "VALUES (?, ?, ?, 'Prompt12.5 面试', '2099-01-01 10:00:00', '上海', 1)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, applicationId);
            statement.setLong(2, jobId);
            statement.setLong(3, companyId);
            return statement;
        });
        jdbcTemplate.update(
                "INSERT INTO interview_signin (session_id, candidate_id, sign_channel, device_id, consent_redirect_flag) "
                        + "VALUES (?, ?, 'qrcode', 'device-center', 0)",
                sessionId,
                candidateId);
    }

    private String insertAuditor() {
        String username = "p125-auditor-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbcTemplate.update(
                "INSERT INTO admin_user (username, display_name, password_hash, role_code, status) "
                        + "VALUES (?, 'Prompt12.5 审计员', "
                        + "'$2b$12$5G4kZ1x4dOar/VtPyL38UuxDYUreMg03NyJG11tnsQvlMZTSZhP2.', 'auditor', 1)",
                username);
        return username;
    }

    private long insertWithKey(StatementFactory statementFactory) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> statementFactory.create(connection), keyHolder);
        Number key = keyHolder.getKey();
        assertThat(key).isNotNull();
        return key.longValue();
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
                .doesNotContain("skills_json")
                .doesNotContain("raw center private body")
                .doesNotContain("private/center-resume.pdf")
                .doesNotContain("raw education should not leak")
                .doesNotContain("raw experience should not leak");
    }

    private interface StatementFactory {
        PreparedStatement create(java.sql.Connection connection) throws java.sql.SQLException;
    }

    private record CandidateAccount(long userId, String token) {
    }

    private record HttpJsonResponse(int status, String traceId, JsonNode body) {
    }
}
