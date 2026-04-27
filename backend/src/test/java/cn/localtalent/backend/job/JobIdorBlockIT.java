package cn.localtalent.backend.job;

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
                "localtalent.auth.jwt.secret=job-idor-secret-change-me-with-enough-length",
                "localtalent.auth.jwt.ttl-seconds=3600"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class JobIdorBlockIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_job_idor_test")
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
    void crossCompanyAndNonOperatorReviewShouldBeBlocked() throws Exception {
        Account companyA = registerAndLoginCompany("A");
        Account companyB = registerAndLoginCompany("B");
        long companyBJobId = createJob(companyB.token(), "trace-idor-job-create-b");

        HttpJsonResponse crossCompanyRead = getJson(
                "/api/company/jobs/" + companyBJobId,
                "trace-job-idor-read",
                "Bearer " + companyA.token());
        assertError(crossCompanyRead, 403, "AUTHZ_403", "trace-job-idor-read");

        HttpJsonResponse crossCompanyUpdate = putJson(
                "/api/company/jobs/" + companyBJobId,
                """
                        {
                          "title": "Edited by wrong company",
                          "category_code": "software",
                          "city_code": "310000",
                          "salary_min": 1,
                          "salary_max": 2,
                          "job_desc": "should be blocked"
                        }
                        """,
                "trace-job-idor-update",
                "Bearer " + companyA.token());
        assertError(crossCompanyUpdate, 403, "AUTHZ_403", "trace-job-idor-update");

        HttpJsonResponse crossCompanyOffline = postJson(
                "/api/company/jobs/" + companyBJobId + "/status",
                """
                        {
                          "action": "offline",
                          "reason": "wrong tenant"
                        }
                        """,
                "trace-job-idor-offline",
                "Bearer " + companyA.token());
        assertError(crossCompanyOffline, 403, "AUTHZ_403", "trace-job-idor-offline");

        HttpJsonResponse companyReviewAttempt = postJson(
                "/api/admin/companies/review",
                """
                        {
                          "company_id": %d,
                          "audit_status": 2,
                          "memo": "company should not review"
                        }
                        """.formatted(companyA.companyId()),
                "trace-company-admin-review-blocked",
                "Bearer " + companyA.token());
        assertError(companyReviewAttempt, 403, "AUTHZ_403", "trace-company-admin-review-blocked");

        String auditorUsername = insertAuditor();
        String auditorToken = login("operator", auditorUsername, "LocalTalent@123456", "trace-auditor-login");
        HttpJsonResponse auditorReviewAttempt = postJson(
                "/api/admin/jobs/review",
                """
                        {
                          "job_id": %d,
                          "audit_status": 2,
                          "memo": "auditor is read only"
                        }
                        """.formatted(companyBJobId),
                "trace-auditor-job-review-blocked",
                "Bearer " + auditorToken);
        assertError(auditorReviewAttempt, 403, "AUTHZ_403", "trace-auditor-job-review-blocked");
    }

    private Account registerAndLoginCompany(String label) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "job-idor-company-" + label + "-" + suffix + "@example.com";
        String licenseNo = "JI-" + label + "-" + suffix.substring(0, 14);
        String password = "Company@123456";
        HttpJsonResponse registerResponse = postJson(
                "/api/auth/register",
                """
                        {
                          "identity_type": "company",
                          "company_name": "Prompt5 越权企业%s",
                          "license_no": "%s",
                          "user_name": "企业管理员",
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(label, licenseNo, email, password),
                "trace-job-idor-register-" + label,
                null);
        assertThat(registerResponse.status()).isEqualTo(200);
        long userId = registerResponse.body().at("/data/identity/user_id").asLong();
        long companyId = registerResponse.body().at("/data/identity/company_id").asLong();
        String token = login("company", email, password, "trace-job-idor-login-" + label);
        return new Account(userId, companyId, token);
    }

    private long createJob(String token, String traceId) throws Exception {
        HttpJsonResponse response = postJson(
                "/api/company/jobs",
                """
                        {
                          "title": "跨企业阻断职位",
                          "category_code": "software",
                          "city_code": "310000",
                          "salary_min": 12000,
                          "salary_max": 18000,
                          "job_desc": "IDOR fixture"
                        }
                        """,
                traceId,
                "Bearer " + token);
        assertThat(response.status()).isEqualTo(200);
        return response.body().at("/data/job_id").asLong();
    }

    private String insertAuditor() {
        String username = "auditor-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbcTemplate.update(
                "INSERT INTO admin_user (username, display_name, password_hash, role_code, status) "
                        + "VALUES (?, '测试审计员', '$2b$12$5G4kZ1x4dOar/VtPyL38UuxDYUreMg03NyJG11tnsQvlMZTSZhP2.', 'auditor', 1)",
                username);
        return username;
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
        assertThat(response.status()).isEqualTo(200);
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

    private HttpJsonResponse putJson(String path, String body, String traceId, String authorization) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .header("X-Trace-Id", traceId)
                .PUT(HttpRequest.BodyPublishers.ofString(body));
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

    private void assertError(HttpJsonResponse response, int expectedStatus, String code, String traceId) {
        assertThat(response.status()).isEqualTo(expectedStatus);
        assertThat(response.traceId()).isEqualTo(traceId);
        assertThat(response.body().at("/code").asText()).isEqualTo(code);
        assertThat(response.body().at("/trace_id").asText()).isEqualTo(traceId);
    }

    private record Account(long userId, long companyId, String token) {
    }

    private record HttpJsonResponse(int status, String traceId, JsonNode body) {
    }
}
