package cn.localtalent.backend.application;

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
                "localtalent.auth.jwt.secret=application-idor-secret-change-me-with-enough-length",
                "localtalent.auth.jwt.ttl-seconds=3600"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ApplicationIdorBlockIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_application_idor_test")
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
    void crossCompanyAndCrossCandidateAccessShouldBeBlocked() throws Exception {
        String operatorToken = login("operator", "operator", "LocalTalent@123456", "trace-p6-idor-operator-login");
        Account companyA = registerAndLoginCompany("A");
        Account companyB = registerAndLoginCompany("B");
        approveCompany(companyA.companyId(), operatorToken, "trace-p6-idor-company-a-approve");
        approveCompany(companyB.companyId(), operatorToken, "trace-p6-idor-company-b-approve");
        long jobB = createAndApproveJob(companyB.token(), operatorToken, "trace-p6-idor-job-b-create", "trace-p6-idor-job-b-approve");

        Account candidateA = registerAndLoginCandidate("A");
        Account candidateB = registerAndLoginCandidate("B");
        long applicationB = createApplication(jobB, candidateB.token(), "trace-p6-idor-application-b");

        HttpJsonResponse poolA = getJson(
                "/api/company/applications?page=1&size=20",
                "trace-p6-idor-company-a-pool",
                "Bearer " + companyA.token());
        assertSuccess(poolA, 200, "trace-p6-idor-company-a-pool");
        assertThat(poolA.body().at("/data/total").asLong()).isZero();

        HttpJsonResponse crossRead = getJson(
                "/api/company/applications/" + applicationB,
                "trace-p6-idor-application-read",
                "Bearer " + companyA.token());
        assertError(crossRead, 403, "AUTHZ_403", "trace-p6-idor-application-read");

        HttpJsonResponse crossCreateSession = postJson(
                "/api/company/applications/" + applicationB + "/interview-sessions",
                """
                        {
                          "session_name": "跨企业一面",
                          "session_time": "2099-01-01T10:00:00",
                          "location": "上海"
                        }
                        """,
                "trace-p6-idor-session-create",
                "Bearer " + companyA.token());
        assertError(crossCreateSession, 403, "AUTHZ_403", "trace-p6-idor-session-create");

        long sessionB = createSession(applicationB, companyB.token(), "trace-p6-idor-session-b");
        HttpJsonResponse crossGenerate = postJson(
                "/api/company/interview-sessions/" + sessionB + "/qrcode",
                """
                        {
                          "ttl_minutes": 30
                        }
                        """,
                "trace-p6-idor-code-generate",
                "Bearer " + companyA.token());
        assertError(crossGenerate, 403, "AUTHZ_403", "trace-p6-idor-code-generate");

        String codeB = generateCode(sessionB, companyB.token(), "trace-p6-idor-code-b");
        HttpJsonResponse wrongCandidateSignin = postJson(
                "/api/interview/signin",
                """
                        {
                          "session_code": "%s",
                          "device_id": "wrong-candidate-device",
                          "sign_channel": "qrcode"
                        }
                        """.formatted(codeB),
                "trace-p6-idor-wrong-candidate",
                "Bearer " + candidateA.token());
        assertError(wrongCandidateSignin, 403, "AUTHZ_403", "trace-p6-idor-wrong-candidate");
        assertThat(queryInt("SELECT COUNT(*) FROM interview_signin WHERE session_id = ?", sessionB)).isZero();
    }

    private Account registerAndLoginCompany(String label) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "p6-idor-company-" + label + "-" + suffix + "@example.com";
        String licenseNo = "P6-I-" + label + "-" + suffix.substring(0, 12);
        String password = "Company@123456";
        HttpJsonResponse registerResponse = postJson(
                "/api/auth/register",
                """
                        {
                          "identity_type": "company",
                          "company_name": "Prompt6 越权企业%s",
                          "license_no": "%s",
                          "user_name": "企业管理员",
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(label, licenseNo, email, password),
                "trace-p6-idor-company-register-" + label,
                null);
        assertSuccess(registerResponse, 200, "trace-p6-idor-company-register-" + label);
        long userId = registerResponse.body().at("/data/identity/user_id").asLong();
        long companyId = registerResponse.body().at("/data/identity/company_id").asLong();
        String token = login("company", email, password, "trace-p6-idor-company-login-" + label);
        return new Account(userId, companyId, token);
    }

    private Account registerAndLoginCandidate(String label) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "p6-idor-candidate-" + label + "-" + suffix + "@example.com";
        String password = "Candidate@123456";
        HttpJsonResponse registerResponse = postJson(
                "/api/auth/register",
                """
                        {
                          "identity_type": "candidate",
                          "email": "%s",
                          "password": "%s",
                          "display_name": "Prompt6 越权求职者%s"
                        }
                        """.formatted(email, password, label),
                "trace-p6-idor-candidate-register-" + label,
                null);
        assertSuccess(registerResponse, 200, "trace-p6-idor-candidate-register-" + label);
        long userId = registerResponse.body().at("/data/identity/user_id").asLong();
        String token = login("candidate", email, password, "trace-p6-idor-candidate-login-" + label);
        return new Account(userId, null, token);
    }

    private void approveCompany(long companyId, String operatorToken, String traceId) throws Exception {
        HttpJsonResponse response = postJson(
                "/api/admin/companies/review",
                """
                        {
                          "company_id": %d,
                          "audit_status": 2,
                          "memo": "approved"
                        }
                        """.formatted(companyId),
                traceId,
                "Bearer " + operatorToken);
        assertSuccess(response, 200, traceId);
    }

    private long createAndApproveJob(String companyToken, String operatorToken, String createTrace, String approveTrace) throws Exception {
        HttpJsonResponse create = postJson(
                "/api/company/jobs",
                """
                        {
                          "title": "Prompt6 越权测试职位",
                          "category_code": "software",
                          "city_code": "310000",
                          "salary_min": 12000,
                          "salary_max": 18000,
                          "job_desc": "Prompt6 idor fixture."
                        }
                        """,
                createTrace,
                "Bearer " + companyToken);
        assertSuccess(create, 200, createTrace);
        long jobId = create.body().at("/data/job_id").asLong();
        HttpJsonResponse approve = postJson(
                "/api/admin/jobs/review",
                """
                        {
                          "job_id": %d,
                          "audit_status": 2,
                          "memo": "job approved"
                        }
                        """.formatted(jobId),
                approveTrace,
                "Bearer " + operatorToken);
        assertSuccess(approve, 200, approveTrace);
        return jobId;
    }

    private long createApplication(long jobId, String candidateToken, String traceId) throws Exception {
        HttpJsonResponse response = postJson(
                "/api/applications",
                """
                        {
                          "job_id": %d
                        }
                        """.formatted(jobId),
                traceId,
                "Bearer " + candidateToken);
        assertSuccess(response, 200, traceId);
        return response.body().at("/data/application_id").asLong();
    }

    private long createSession(long applicationId, String companyToken, String traceId) throws Exception {
        HttpJsonResponse response = postJson(
                "/api/company/applications/" + applicationId + "/interview-sessions",
                """
                        {
                          "session_name": "一面",
                          "session_time": "2099-01-01T10:00:00",
                          "location": "上海"
                        }
                        """,
                traceId,
                "Bearer " + companyToken);
        assertSuccess(response, 200, traceId);
        return response.body().at("/data/session_id").asLong();
    }

    private String generateCode(long sessionId, String companyToken, String traceId) throws Exception {
        HttpJsonResponse response = postJson(
                "/api/company/interview-sessions/" + sessionId + "/qrcode",
                """
                        {
                          "ttl_minutes": 30
                        }
                        """,
                traceId,
                "Bearer " + companyToken);
        assertSuccess(response, 200, traceId);
        return response.body().at("/data/session_code").asText();
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
                null);
        assertSuccess(response, 200, traceId);
        return response.body().at("/data/access_token").asText();
    }

    private HttpJsonResponse postJson(String path, String body, String traceId, String authorization) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .header("X-Trace-Id", traceId)
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (authorization != null) {
            builder.header("Authorization", authorization);
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

    private int queryInt(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private record Account(long userId, Long companyId, String token) {
    }

    private record HttpJsonResponse(int status, String traceId, JsonNode body) {
    }
}
