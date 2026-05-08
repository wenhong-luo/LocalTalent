package cn.localtalent.backend.exporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "localtalent.auth.jwt.secret=prompt8-export-secret-change-me-with-enough-length",
                "localtalent.auth.jwt.ttl-seconds=3600",
                "localtalent.export.download-ttl-seconds=5"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ExportApprovalFlowIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_export_flow_test")
            .withUsername("localtalent")
            .withPassword("localtalent");

    @Container
    static final GenericContainer<?> MINIO = new GenericContainer<>(
            DockerImageName.parse("minio/minio:RELEASE.2025-09-07T16-13-09Z"))
            .withExposedPorts(9000)
            .withEnv("MINIO_ROOT_USER", "localtalent")
            .withEnv("MINIO_ROOT_PASSWORD", "localtalent123")
            .withCommand("server", "/data")
            .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000));

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
        registry.add("localtalent.minio.endpoint", () -> "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000));
        registry.add("localtalent.minio.access-key", () -> "localtalent");
        registry.add("localtalent.minio.secret-key", () -> "localtalent123");
        registry.add("localtalent.minio.bucket", () -> "export-it");
    }

    @Test
    void exportApprovalDownloadExpiryAndFieldPolicyShouldWork() throws Exception {
        String operatorToken = login("operator", "operator", "LocalTalent@123456", "trace-p8-operator-login");
        Account company = registerAndLoginCompany("flow");
        approveCompany(company.companyId(), operatorToken, "trace-p8-company-approve");
        long jobId = createAndApproveJob(company.token(), operatorToken, "trace-p8-job-create", "trace-p8-job-approve");

        Account candidate = registerAndLoginCandidate("flow");
        long resumeId = insertResume(candidate.userId());
        long applicationId = createApplication(jobId, resumeId, candidate.token(), "trace-p8-application-create");

        long exportId = applyExport(jobId, company.token(), "trace-p8-export-apply");

        HttpJsonResponse unapprovedDownload = getJson(
                "/api/company/exports/" + exportId + "/download-url",
                "trace-p8-download-unapproved",
                "Bearer " + company.token());
        assertError(unapprovedDownload, 403, "EXPORT_403", "trace-p8-download-unapproved");

        HttpJsonResponse approve = postJson(
                "/api/admin/exports/review",
                """
                        {
                          "export_id": %d,
                          "approve_status": 1,
                          "memo": "prompt8 export approved"
                        }
                        """.formatted(exportId),
                "trace-p8-export-approve",
                "Bearer " + operatorToken);
        assertSuccess(approve, 200, "trace-p8-export-approve");
        assertThat(approve.body().at("/data/approve_status").asInt()).isEqualTo(1);

        waitUntilGenerated(exportId, company.token());

        LocalDateTime beforeIssue = LocalDateTime.now();
        HttpJsonResponse downloadUrlResponse = getJson(
                "/api/company/exports/" + exportId + "/download-url",
                "trace-p8-download-url",
                "Bearer " + company.token());
        assertSuccess(downloadUrlResponse, 200, "trace-p8-download-url");
        String downloadUrl = downloadUrlResponse.body().at("/data/download_url").asText();
        assertThat(downloadUrl).isNotBlank().contains("/export-it/");
        assertThat(downloadUrl).contains("X-Amz-Expires=5");
        LocalDateTime persistedExpireTime = jdbcTemplate.queryForObject(
                "SELECT expire_time FROM export_apply WHERE id = ?",
                LocalDateTime.class,
                exportId);
        assertThat(persistedExpireTime)
                .as("trace-p8-url-expired should prove LocalTalent issues only a short-lived URL")
                .isAfterOrEqualTo(beforeIssue.plusSeconds(1))
                .isBeforeOrEqualTo(beforeIssue.plusSeconds(10));

        HttpResponse<String> csvResponse = getText(downloadUrl);
        assertThat(csvResponse.statusCode()).isEqualTo(200);
        String csv = csvResponse.body();
        assertThat(csv).contains("application_id,job_title,application_status,apply_time,display_name,mobile,email,skills_summary");
        assertThat(csv).contains(String.valueOf(applicationId));
        assertThat(csv).contains("Prompt8 Java 工程师");
        assertThat(csv).doesNotContain("13900001234");
        assertThat(csv).doesNotContain("p8-candidate-flow");
        assertThat(csv).doesNotContain("SecretSkill");
        assertThat(csv).contains("139****1234");
        assertThat(csv).contains("***@example.com");

        HttpJsonResponse secondIssue = getJson(
                "/api/company/exports/" + exportId + "/download-url",
                "trace-p8-download-repeat",
                "Bearer " + company.token());
        assertError(secondIssue, 409, "EXPORT_409", "trace-p8-download-repeat");

        long rejectedExportId = applyExport(jobId, company.token(), "trace-p8-export-reject-apply");
        HttpJsonResponse reject = postJson(
                "/api/admin/exports/review",
                """
                        {
                          "export_id": %d,
                          "approve_status": 2,
                          "memo": "prompt8 reject reason"
                        }
                        """.formatted(rejectedExportId),
                "trace-p8-export-reject",
                "Bearer " + operatorToken);
        assertSuccess(reject, 200, "trace-p8-export-reject");
        assertThat(reject.body().at("/data/reject_reason").asText()).isEqualTo("prompt8 reject reason");

        HttpJsonResponse rejectedDownload = getJson(
                "/api/company/exports/" + rejectedExportId + "/download-url",
                "trace-p8-download-rejected",
                "Bearer " + company.token());
        assertError(rejectedDownload, 403, "EXPORT_403", "trace-p8-download-rejected");

        assertThat(countAudit("trace-p8-export-apply", "export_apply", "export_apply")).isEqualTo(1);
        assertThat(countAudit("trace-p8-export-approve", "export_apply", "export_approve")).isEqualTo(1);
        assertThat(countAudit("trace-p8-export-approve", "export_apply", "export_generate")).isEqualTo(1);
        assertThat(countAudit("trace-p8-download-url", "export_apply", "export_download_url_issue")).isEqualTo(1);
        assertThat(countAudit("trace-p8-export-reject", "export_apply", "export_reject")).isEqualTo(1);
        assertThat(queryString(
                "SELECT after_json FROM audit_log WHERE trace_id = ? AND action_type = ?",
                "trace-p8-export-reject",
                "export_reject")).contains("prompt8 reject reason");
        assertThat(countFieldAccess("trace-p8-export-approve", "export_application", applicationId)).isGreaterThanOrEqualTo(4);
        assertThat(countFieldAccessByType("trace-p8-export-approve", "export_application", applicationId, "MASK"))
                .isGreaterThanOrEqualTo(3);
    }

    private Account registerAndLoginCompany(String label) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "p8-company-" + label + "-" + suffix + "@example.com";
        String licenseNo = "P8-C-" + label + "-" + suffix.substring(0, 12);
        String password = "Company@123456";
        HttpJsonResponse registerResponse = postJson(
                "/api/auth/register",
                """
                        {
                          "identity_type": "company",
                          "company_name": "Prompt8 企业%s",
                          "license_no": "%s",
                          "user_name": "企业管理员",
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(label, licenseNo, email, password),
                "trace-p8-company-register-" + label,
                null);
        assertSuccess(registerResponse, 200, "trace-p8-company-register-" + label);
        long userId = registerResponse.body().at("/data/identity/user_id").asLong();
        long companyId = registerResponse.body().at("/data/identity/company_id").asLong();
        String token = login("company", email, password, "trace-p8-company-login-" + label);
        return new Account(userId, companyId, token);
    }

    private Account registerAndLoginCandidate(String label) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "p8-candidate-" + label + "-" + suffix + "@example.com";
        String password = "Candidate@123456";
        HttpJsonResponse registerResponse = postJson(
                "/api/auth/register",
                """
                        {
                          "identity_type": "candidate",
                          "mobile": "13900001234",
                          "email": "%s",
                          "password": "%s",
                          "display_name": "张三"
                        }
                        """.formatted(email, password),
                "trace-p8-candidate-register-" + label,
                null);
        assertSuccess(registerResponse, 200, "trace-p8-candidate-register-" + label);
        long userId = registerResponse.body().at("/data/identity/user_id").asLong();
        String token = login("candidate", email, password, "trace-p8-candidate-login-" + label);
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
                          "title": "Prompt8 Java 工程师",
                          "category_code": "software",
                          "city_code": "310000",
                          "salary_min": 18000,
                          "salary_max": 26000,
                          "job_desc": "Prompt8 export fixture."
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

    private long insertResume(long candidateId) {
        jdbcTemplate.update(
                "INSERT INTO candidate_resume "
                        + "(candidate_id, resume_name, base_profile_json, education_json, experience_json, skills_json, "
                        + "attachment_object_key, status) VALUES (?, ?, ?, ?, ?, ?, ?, 1)",
                candidateId,
                "Prompt8 简历",
                "{\"city_code\":\"310000\"}",
                "[]",
                "[]",
                "{\"summary\":\"Java Spring SecretSkill\"}",
                "candidate/private/resume.pdf");
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return id == null ? 0L : id;
    }

    private long createApplication(long jobId, long resumeId, String candidateToken, String traceId) throws Exception {
        HttpJsonResponse response = postJson(
                "/api/applications",
                """
                        {
                          "job_id": %d,
                          "resume_id": %d
                        }
                        """.formatted(jobId, resumeId),
                traceId,
                "Bearer " + candidateToken);
        assertSuccess(response, 200, traceId);
        return response.body().at("/data/application_id").asLong();
    }

    private long applyExport(long jobId, String companyToken, String traceId) throws Exception {
        HttpJsonResponse response = postJson(
                "/api/company/exports",
                """
                        {
                          "biz_type": "application_candidate_detail",
                          "scope": {
                            "job_id": %d
                          },
                          "reason": "Prompt8 approval required export"
                        }
                        """.formatted(jobId),
                traceId,
                "Bearer " + companyToken);
        assertSuccess(response, 200, traceId);
        assertThat(response.body().at("/data/approve_status").asInt()).isZero();
        return response.body().at("/data/export_id").asLong();
    }

    private void waitUntilGenerated(long exportId, String companyToken) throws Exception {
        int lastStatus = -1;
        String lastError = "";
        for (int i = 0; i < 80; i++) {
            HttpJsonResponse detail = getJson(
                    "/api/company/exports/" + exportId,
                    "trace-p8-export-detail-" + i,
                    "Bearer " + companyToken);
            assertSuccess(detail, 200, "trace-p8-export-detail-" + i);
            lastStatus = detail.body().at("/data/generate_status").asInt();
            lastError = detail.body().at("/data/error_msg").asText("");
            if (lastStatus == 2) {
                return;
            }
            if (lastStatus == 3) {
                throw new AssertionError("export generation failed: " + lastError);
            }
            Thread.sleep(500L);
        }
        throw new AssertionError("export generation did not finish, last status=" + lastStatus + ", error=" + lastError);
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
        return sendJson(builder.build());
    }

    private HttpJsonResponse getJson(String path, String traceId, String authorization) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("X-Trace-Id", traceId)
                .GET();
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        return sendJson(builder.build());
    }

    private HttpResponse<String> getText(String absoluteUrl) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(absoluteUrl)).GET().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpJsonResponse sendJson(HttpRequest request) throws Exception {
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

    private int countAudit(String traceId, String bizType, String actionType) {
        return queryInt(
                "SELECT COUNT(*) FROM audit_log WHERE trace_id = ? AND biz_type = ? AND action_type = ?",
                traceId,
                bizType,
                actionType);
    }

    private int countFieldAccess(String traceId, String bizType, long bizId) {
        return queryInt(
                "SELECT COUNT(*) FROM field_access_log WHERE trace_id = ? AND biz_type = ? AND biz_id = ?",
                traceId,
                bizType,
                bizId);
    }

    private int countFieldAccessByType(String traceId, String bizType, long bizId, String accessType) {
        return queryInt(
                "SELECT COUNT(*) FROM field_access_log "
                        + "WHERE trace_id = ? AND biz_type = ? AND biz_id = ? AND access_type = ?",
                traceId,
                bizType,
                bizId,
                accessType);
    }

    private int queryInt(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private String queryString(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, String.class, args);
    }

    private record Account(long userId, Long companyId, String token) {
    }

    private record HttpJsonResponse(int status, String traceId, JsonNode body) {
    }
}
