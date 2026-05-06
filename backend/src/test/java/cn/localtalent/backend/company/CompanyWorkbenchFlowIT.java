package cn.localtalent.backend.company;

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
                "localtalent.auth.jwt.secret=company-workbench-flow-secret-change-me",
                "localtalent.auth.jwt.ttl-seconds=3600",
                "localtalent.phase3.company-workbench=true"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CompanyWorkbenchFlowIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_company_workbench_flow_test")
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
    void companyWorkbenchShouldSupportPrivateFlowWithDataScopeAuditIdempotencyAndSafeFields() throws Exception {
        CompanyAccount company = registerAndLoginCompany("main");
        CompanyAccount otherCompany = registerAndLoginCompany("other");

        HttpJsonResponse overview = getJson(
                "/api/company/workbench/overview",
                "trace-p28-overview",
                "Bearer " + company.token());
        assertSuccess(overview, 200, "trace-p28-overview");
        assertThat(overview.body().at("/data/features/company_workbench_enabled").asBoolean()).isTrue();

        HttpJsonResponse profile = putJson(
                "/api/company/workbench/profile",
                """
                        {
                          "company_name": "P28 生产化企业",
                          "industry_code": "internet",
                          "nature_code": "private",
                          "scale_code": "50-100",
                          "city_code": "310000",
                          "address": "仅企业私有域可见地址",
                          "company_profile": "企业工作台私有简介。"
                        }
                        """,
                "trace-p28-profile",
                "Bearer " + company.token(),
                "idem-p28-profile-001");
        assertSuccess(profile, 200, "trace-p28-profile");
        assertThat(profile.body().at("/data/company_name").asText()).isEqualTo("P28 生产化企业");

        HttpJsonResponse certification = postJson(
                "/api/company/workbench/certification",
                """
                        {
                          "company_name": "P28 生产化企业",
                          "license_no": "P28-LIC-%s",
                          "industry_code": "internet",
                          "nature_code": "private",
                          "scale_code": "50-100",
                          "city_code": "310000",
                          "address": "认证私有地址",
                          "company_profile": "认证提交简介。",
                          "certification_material_summary": {
                            "license_verified": "true",
                            "review_note": "仅保存材料摘要",
                            "attachment_object_key": "must-not-store"
                          }
                        }
                        """.formatted(UUID.randomUUID().toString().replace("-", "").substring(0, 12)),
                "trace-p28-certification",
                "Bearer " + company.token(),
                "idem-p28-certification-001");
        assertSuccess(certification, 200, "trace-p28-certification");
        assertThat(certification.body().at("/data/auth_status").asInt()).isEqualTo(1);
        assertThat(certification.rawBody()).doesNotContain("must-not-store");

        HttpJsonResponse draftJob = postJson(
                "/api/company/workbench/jobs",
                jobBody("P28 Java 工程师"),
                "trace-p28-job-create",
                "Bearer " + company.token(),
                "idem-p28-job-create-001");
        assertSuccess(draftJob, 200, "trace-p28-job-create");
        long jobId = draftJob.body().at("/data/job_id").asLong();

        HttpJsonResponse blockedReview = postJson(
                "/api/company/workbench/jobs/" + jobId + "/submit-review",
                "{}",
                "trace-p28-review-blocked",
                "Bearer " + company.token(),
                "idem-p28-review-blocked-001");
        assertError(blockedReview, 403, "AUTHZ_403", "trace-p28-review-blocked");

        jdbcTemplate.update("UPDATE company SET auth_status = 2 WHERE id = ?", company.companyId());
        HttpJsonResponse submitReview = postJson(
                "/api/company/workbench/jobs/" + jobId + "/submit-review",
                "{}",
                "trace-p28-review-submit",
                "Bearer " + company.token(),
                "idem-p28-review-submit-001");
        assertSuccess(submitReview, 200, "trace-p28-review-submit");
        jdbcTemplate.update("UPDATE job_post SET status = 2, audit_status = 2, published_at = CURRENT_TIMESTAMP WHERE id = ?", jobId);

        long candidateId = insertCandidate();
        long resumeId = insertResume(candidateId);
        long applicationId = insertApplication(jobId, candidateId, resumeId);

        HttpJsonResponse applications = getJson(
                "/api/company/workbench/applications?page=1&size=20",
                "trace-p28-applications",
                "Bearer " + company.token());
        assertSuccess(applications, 200, "trace-p28-applications");
        assertThat(applications.body().at("/data/total").asLong()).isEqualTo(1);
        assertNoSensitiveFields(applications);

        HttpJsonResponse detail = getJson(
                "/api/company/workbench/applications/" + applicationId,
                "trace-p28-application-detail",
                "Bearer " + company.token());
        assertSuccess(detail, 200, "trace-p28-application-detail");
        assertThat(detail.body().at("/data/display_name_masked").asText()).contains("*");
        assertNoSensitiveFields(detail);

        HttpJsonResponse crossCompanyDetail = getJson(
                "/api/company/workbench/applications/" + applicationId,
                "trace-p28-cross-company-detail",
                "Bearer " + otherCompany.token());
        assertError(crossCompanyDetail, 404, "NOT_FOUND_404", "trace-p28-cross-company-detail");

        HttpJsonResponse stage = postJson(
                "/api/company/workbench/applications/" + applicationId + "/stage",
                """
                        {
                          "stage": "interview_invited",
                          "note": "站内邀约，不外发短信微信。"
                        }
                        """,
                "trace-p28-stage",
                "Bearer " + company.token(),
                "idem-p28-stage-001");
        assertSuccess(stage, 200, "trace-p28-stage");
        assertThat(stage.body().at("/data/application_status").asInt()).isEqualTo(2);

        HttpJsonResponse repeatedStage = postJson(
                "/api/company/workbench/applications/" + applicationId + "/stage",
                """
                        {
                          "stage": "interview_invited",
                          "note": "站内邀约，不外发短信微信。"
                        }
                        """,
                "trace-p28-stage-repeat",
                "Bearer " + company.token(),
                "idem-p28-stage-001");
        assertSuccess(repeatedStage, 200, "trace-p28-stage-repeat");
        HttpJsonResponse conflictingStage = postJson(
                "/api/company/workbench/applications/" + applicationId + "/stage",
                """
                        {
                          "stage": "rejected",
                          "note": "不同请求体"
                        }
                        """,
                "trace-p28-stage-conflict",
                "Bearer " + company.token(),
                "idem-p28-stage-001");
        assertError(conflictingStage, 409, "IDEMPOTENCY_409", "trace-p28-stage-conflict");

        HttpJsonResponse interview = postJson(
                "/api/company/workbench/applications/" + applicationId + "/interview-sessions",
                """
                        {
                          "session_name": "P28 一面",
                          "session_time": "2099-01-01T10:00:00",
                          "location": "线上会议占位"
                        }
                        """,
                "trace-p28-interview",
                "Bearer " + company.token(),
                "idem-p28-interview-001");
        assertSuccess(interview, 200, "trace-p28-interview");
        HttpJsonResponse sessions = getJson(
                "/api/company/workbench/interview-sessions?page=1&size=20",
                "trace-p28-interview-list",
                "Bearer " + company.token());
        assertSuccess(sessions, 200, "trace-p28-interview-list");
        assertThat(sessions.body().at("/data/total").asLong()).isEqualTo(1);

        HttpJsonResponse export = postJson(
                "/api/company/workbench/exports",
                """
                        {
                          "reason": "企业工作台导出申请，仅进入审批链。",
                          "scope": {
                            "job_id": %d
                          }
                        }
                        """.formatted(jobId),
                "trace-p28-export",
                "Bearer " + company.token(),
                "idem-p28-export-001");
        assertSuccess(export, 200, "trace-p28-export");
        assertThat(export.body().at("/data/approve_status").asInt()).isZero();

        HttpJsonResponse companyDenied = getJson(
                "/api/company/workbench/overview",
                "trace-p28-candidate-denied",
                "Bearer " + registerAndLoginCandidate());
        assertError(companyDenied, 403, "AUTHZ_403", "trace-p28-candidate-denied");

        assertThat(countRows("audit_log", "action_type IN ('company_profile_save','company_certification_submit','application_stage_change','workbench_interview_invite','export_apply')"))
                .isGreaterThanOrEqualTo(5);
        assertThat(countRows("field_access_log", "biz_type = 'job_application' AND access_type = 'COMPANY_APPLICATION_DETAIL'"))
                .isGreaterThanOrEqualTo(1);
    }

    private CompanyAccount registerAndLoginCompany(String label) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "p28-company-" + label + "-" + suffix + "@example.com";
        String password = "Company@123456";
        HttpJsonResponse register = postJson(
                "/api/auth/register",
                """
                        {
                          "identity_type": "company",
                          "company_name": "P28 企业 %s",
                          "license_no": "P28-%s",
                          "user_name": "企业管理员",
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(label, suffix.substring(0, 12), email, password),
                "trace-p28-company-register-" + label,
                null,
                null);
        assertSuccess(register, 200, "trace-p28-company-register-" + label);
        return new CompanyAccount(
                register.body().at("/data/identity/company_id").asLong(),
                login("company", email, password, "trace-p28-company-login-" + label));
    }

    private String registerAndLoginCandidate() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "p28-candidate-" + suffix + "@example.com";
        String password = "Candidate@123456";
        postJson(
                "/api/auth/register",
                """
                        {
                          "identity_type": "candidate",
                          "email": "%s",
                          "password": "%s",
                          "display_name": "P28 求职者"
                        }
                        """.formatted(email, password),
                "trace-p28-candidate-register",
                null,
                null);
        return login("candidate", email, password, "trace-p28-candidate-login");
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

    private String jobBody(String title) {
        return """
                {
                  "title": "%s",
                  "category_code": "software",
                  "city_code": "310000",
                  "salary_min": 15000,
                  "salary_max": 25000,
                  "job_desc": "企业工作台职位描述，不含联系方式。"
                }
                """.formatted(title);
    }

    private long insertCandidate() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update(
                "INSERT INTO candidate_user (email, password_hash, real_name, realname_verified_flag) VALUES (?, 'x', ?, 1)",
                "p28-fixture-" + suffix + "@example.com",
                "王测试");
        return jdbcTemplate.queryForObject("SELECT id FROM candidate_user WHERE email = ?", Long.class,
                "p28-fixture-" + suffix + "@example.com");
    }

    private long insertResume(long candidateId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO candidate_resume "
                            + "(candidate_id, resume_name, base_profile_json, education_json, experience_json, skills_json, attachment_object_key) "
                            + "VALUES (?, 'P28 简历', CAST(? AS JSON), CAST('[]' AS JSON), CAST('[]' AS JSON), CAST(? AS JSON), ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, candidateId);
            statement.setString(2, "{\"display_name\":\"王测试\",\"city_code\":\"310000\",\"experience_years\":5}");
            statement.setString(3, "[\"Java\",\"Spring\"]");
            statement.setString(4, "private/resume.pdf");
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private long insertApplication(long jobId, long candidateId, long resumeId) {
        jdbcTemplate.update(
                "INSERT INTO job_application (job_id, candidate_id, resume_id, source_type, application_status) "
                        + "VALUES (?, ?, ?, 2, 0)",
                jobId,
                candidateId,
                resumeId);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM job_application WHERE job_id = ? AND candidate_id = ?",
                Long.class,
                jobId,
                candidateId);
    }

    private long countRows(String table, String where) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + where, Long.class);
        return count == null ? 0 : count;
    }

    private HttpJsonResponse postJson(String path, String body, String traceId, String authorization, String idempotencyKey)
            throws Exception {
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

    private HttpJsonResponse putJson(String path, String body, String traceId, String authorization, String idempotencyKey)
            throws Exception {
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
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path)).header("X-Trace-Id", traceId).GET();
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        return send(builder.build());
    }

    private HttpJsonResponse send(HttpRequest request) throws Exception {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return new HttpJsonResponse(response.statusCode(), response.body(), objectMapper.readTree(response.body()));
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private void assertSuccess(HttpJsonResponse response, int status, String traceId) {
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.body().at("/code").asText()).isEqualTo("0");
        assertThat(response.body().at("/trace_id").asText()).isEqualTo(traceId);
    }

    private void assertError(HttpJsonResponse response, int status, String code, String traceId) {
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.body().at("/code").asText()).isEqualTo(code);
        assertThat(response.body().at("/trace_id").asText()).isEqualTo(traceId);
    }

    private void assertNoSensitiveFields(HttpJsonResponse response) {
        assertThat(response.rawBody())
                .doesNotContain("mobile")
                .doesNotContain("email")
                .doesNotContain("resume_body")
                .doesNotContain("attachment_object_key")
                .doesNotContain("evidence")
                .doesNotContain("password_hash")
                .doesNotContain("base_profile_json")
                .doesNotContain("education_json")
                .doesNotContain("experience_json")
                .doesNotContain("skills_json")
                .doesNotContain("private/resume.pdf")
                .doesNotContain("营业执照附件")
                .doesNotContain("审核材料")
                .doesNotContain("后台备注");
    }

    private record CompanyAccount(long companyId, String token) {
    }

    private record HttpJsonResponse(int status, String rawBody, JsonNode body) {
    }
}
