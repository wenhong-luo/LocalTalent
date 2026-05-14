package cn.localtalent.backend.company;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
                "localtalent.auth.jwt.secret=company-resume-search-secret-change-me",
                "localtalent.auth.jwt.ttl-seconds=3600",
                "localtalent.phase3.company-workbench=true",
                "localtalent.phase3.company-resume-search=true"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CompanyResumeSearchIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_company_resume_search_test")
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
    void companyResumeSearchShouldUsePublishSnapshotsOnlyWithFiltersAndSafeFields() throws Exception {
        CompanyAccount approvedCompany = registerAndLoginCompany("approved");
        CompanyAccount pendingCompany = registerAndLoginCompany("pending");
        jdbcTemplate.update("UPDATE company SET auth_status = 2 WHERE id = ?", approvedCompany.companyId());

        long candidateId = insertCandidate("resume-search-target");
        long targetSnapshot = insertSnapshot(candidateId, """
                {
                  "display_name_masked": "王*",
                  "age_band": "25-29",
                  "gender": "male",
                  "education_code": "bachelor",
                  "highest_education": "本科",
                  "experience_years": 5,
                  "city_code": "310000",
                  "category_code": "software",
                  "industry_code": "internet",
                  "major_name": "计算机科学与技术",
                  "work_nature": "full_time",
                  "expected_salary": "8k-12k",
                  "expected_salary_code": "8k_12k",
                  "expected_positions": ["Java工程师", "后端工程师"],
                  "expected_positions_text": "Java工程师 / 后端工程师",
                  "expected_cities": ["上海 / 上海市 / 浦东新区"],
                  "expected_cities_text": "上海 / 上海市 / 浦东新区",
                  "resume_tags": ["形象好", "经验丰富"],
                  "resume_tags_text": "形象好 / 经验丰富",
                  "skills_summary": "Java / Spring / MySQL",
                  "mobile": "13812345678",
                  "email": "raw@example.com",
                  "contact_wechat": "raw-wechat",
                  "resume_body": "raw-secret-resume",
                  "attachment_object_key": "private/raw.pdf",
                  "evidence": "raw-evidence",
                  "base_profile_json": "raw-base-json"
                }
                """, 1, 1, 1, 4, 1);
        insertSnapshot(candidateId, snapshotJson("李*", "110000", "software", "Java / Spring"), 1, 1, 1, 4, 1);
        insertSnapshot(candidateId, snapshotJson("张*", "310000", "sales", "销售"), 1, 1, 1, 4, 1);
        long revokedSnapshot = insertSnapshot(candidateId, snapshotJson("撤*", "310000", "software", "Java"), 0, 1, 1, 4, 1);
        insertSnapshot(candidateId, snapshotJson("下*", "310000", "software", "Java"), 1, 1, 0, 4, 1);
        insertSnapshot(candidateId, snapshotJson("私*", "310000", "software", "Java"), 1, 1, 1, 2, 1);

        String query = "/api/company/workbench/resume-search"
                + "?keyword=" + encode("Java")
                + "&city_code=310000"
                + "&category_code=software"
                + "&education_code=bachelor"
                + "&experience_min=3"
                + "&experience_max=6"
                + "&gender=male"
                + "&resume_tag=" + encode("形象好")
                + "&industry_code=internet"
                + "&major=" + encode("计算机")
                + "&work_nature=full_time"
                + "&expected_salary_code=8k_12k"
                + "&updated_within=30"
                + "&sort=experience_desc"
                + "&page=1"
                + "&size=20";
        HttpJsonResponse response = getJson(query, "trace-p3-resume-search-success", "Bearer " + approvedCompany.token());

        assertSuccess(response, 200, "trace-p3-resume-search-success");
        assertThat(response.body().at("/data/total").asLong()).isEqualTo(1);
        assertThat(response.body().at("/data/snapshot_list/0/snapshot_id").asLong()).isEqualTo(targetSnapshot);
        assertThat(response.body().at("/data/snapshot_list/0/display_name_masked").asText()).isEqualTo("王*");
        assertThat(response.body().at("/data/snapshot_list/0/expected_positions/0").asText()).isEqualTo("Java工程师");
        assertThat(response.body().at("/data/snapshot_list/0/expected_cities/0").asText()).contains("浦东新区");
        assertThat(response.body().at("/data/snapshot_list/0/skills_summary").asText()).contains("Java");
        assertNoSensitiveFields(response);
        assertThat(countRows("field_access_log",
                "biz_type = 'candidate_publish_snapshot' AND biz_id = " + targetSnapshot
                        + " AND access_type = 'COMPANY_RESUME_SEARCH'"))
                .isGreaterThanOrEqualTo(1);

        HttpJsonResponse detail = getJson(
                "/api/company/workbench/resume-search/" + targetSnapshot,
                "trace-p3-resume-search-detail",
                "Bearer " + approvedCompany.token());
        assertSuccess(detail, 200, "trace-p3-resume-search-detail");
        assertThat(detail.body().at("/data/snapshot_id").asLong()).isEqualTo(targetSnapshot);
        assertThat(detail.body().at("/data/contact_access_hint").asText()).contains("合规申请");
        assertThat(detail.body().at("/data/contact_access_hint").asText()).contains("不开放联系解锁");
        assertNoSensitiveFields(detail);
        assertThat(countRows("field_access_log",
                "biz_type = 'candidate_publish_snapshot' AND biz_id = " + targetSnapshot
                        + " AND access_type = 'COMPANY_RESUME_SEARCH_DETAIL'"))
                .isGreaterThanOrEqualTo(1);

        HttpJsonResponse report = postJson(
                "/api/company/workbench/resume-search/" + targetSnapshot + "/reports",
                """
                        {
                          "reason_code": "false_information",
                          "remark": "疑似虚假信息，联系方式 13812345678 和 raw@example.com 不应进入日志"
                        }
                        """,
                "trace-p3-resume-search-report",
                "Bearer " + approvedCompany.token(),
                "idem-p3-resume-report-001");
        assertSuccess(report, 200, "trace-p3-resume-search-report");
        assertThat(report.body().at("/data/snapshot_id").asLong()).isEqualTo(targetSnapshot);
        assertThat(report.body().at("/data/report_status").asText()).isEqualTo("submitted");
        assertNoSensitiveFields(report);
        long reportId = report.body().at("/data/report_id").asLong();
        assertThat(countRows("company_resume_snapshot_report",
                "id = " + reportId + " AND company_id = " + approvedCompany.companyId()
                        + " AND snapshot_id = " + targetSnapshot + " AND reason_code = 'false_information'"))
                .isEqualTo(1);
        assertThat(countRows("company_resume_snapshot_report",
                "id = " + reportId + " AND remark_summary LIKE '%redacted-mobile%' AND remark_summary LIKE '%redacted-email%'"))
                .isEqualTo(1);
        assertThat(countRows("risk_review",
                "risk_type = 'resume_snapshot_report' AND target_type = 'talent_snapshot' AND target_id = " + targetSnapshot))
                .isEqualTo(1);
        assertThat(countRows("audit_log",
                "biz_type = 'company_resume_snapshot_report' AND biz_id = " + reportId
                        + " AND action_type = 'resume_snapshot_report'"))
                .isEqualTo(1);

        HttpJsonResponse repeatedReport = postJson(
                "/api/company/workbench/resume-search/" + targetSnapshot + "/reports",
                """
                        {
                          "reason_code": "false_information",
                          "remark": "疑似虚假信息，联系方式 13812345678 和 raw@example.com 不应进入日志"
                        }
                        """,
                "trace-p3-resume-search-report-repeat",
                "Bearer " + approvedCompany.token(),
                "idem-p3-resume-report-001");
        assertSuccess(repeatedReport, 200, "trace-p3-resume-search-report-repeat");
        assertThat(repeatedReport.body().at("/data/report_id").asLong()).isEqualTo(reportId);
        HttpJsonResponse conflictingReport = postJson(
                "/api/company/workbench/resume-search/" + targetSnapshot + "/reports",
                """
                        {
                          "reason_code": "wrong_category",
                          "remark": "不同举报原因"
                        }
                        """,
                "trace-p3-resume-search-report-conflict",
                "Bearer " + approvedCompany.token(),
                "idem-p3-resume-report-001");
        assertError(conflictingReport, 409, "IDEMPOTENCY_409", "trace-p3-resume-search-report-conflict");

        HttpJsonResponse revokedDetail = getJson(
                "/api/company/workbench/resume-search/" + revokedSnapshot,
                "trace-p3-resume-search-detail-revoked",
                "Bearer " + approvedCompany.token());
        assertError(revokedDetail, 404, "NOT_FOUND_404", "trace-p3-resume-search-detail-revoked");
        HttpJsonResponse revokedReport = postJson(
                "/api/company/workbench/resume-search/" + revokedSnapshot + "/reports",
                """
                        {
                          "reason_code": "other",
                          "remark": "不可见快照不得举报"
                        }
                        """,
                "trace-p3-resume-search-report-revoked",
                "Bearer " + approvedCompany.token(),
                "idem-p3-resume-report-revoked");
        assertError(revokedReport, 404, "NOT_FOUND_404", "trace-p3-resume-search-report-revoked");

        HttpJsonResponse pending = getJson(
                "/api/company/workbench/resume-search?page=1&size=20",
                "trace-p3-resume-search-pending",
                "Bearer " + pendingCompany.token());
        assertError(pending, 403, "AUTHZ_403", "trace-p3-resume-search-pending");

        String candidateToken = registerAndLoginCandidate();
        HttpJsonResponse candidateDenied = getJson(
                "/api/company/workbench/resume-search?page=1&size=20",
                "trace-p3-resume-search-candidate",
                "Bearer " + candidateToken);
        assertError(candidateDenied, 403, "AUTHZ_403", "trace-p3-resume-search-candidate");

        String operatorToken = login("operator", "operator", "LocalTalent@123456", "trace-p3-resume-search-operator-login");
        HttpJsonResponse operatorDenied = getJson(
                "/api/company/workbench/resume-search?page=1&size=20",
                "trace-p3-resume-search-operator",
                "Bearer " + operatorToken);
        assertError(operatorDenied, 403, "AUTHZ_403", "trace-p3-resume-search-operator");

        HttpJsonResponse invalidSort = getJson(
                "/api/company/workbench/resume-search?sort=unsafe",
                "trace-p3-resume-search-invalid",
                "Bearer " + approvedCompany.token());
        assertError(invalidSort, 400, "VALID_400", "trace-p3-resume-search-invalid");
    }

    private String snapshotJson(String displayName, String cityCode, String categoryCode, String skills) {
        return """
                {
                  "display_name_masked": "%s",
                  "city_code": "%s",
                  "category_code": "%s",
                  "education_code": "bachelor",
                  "experience_years": 5,
                  "skills_summary": "%s"
                }
                """.formatted(displayName, cityCode, categoryCode, skills);
    }

    private long insertSnapshot(
            long candidateId,
            String snapshotJson,
            int publishableFlag,
            int consentStatus,
            int status,
            int visibilityScope,
            int daysAgo
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO candidate_publish_snapshot "
                            + "(candidate_id, source_type, legal_basis, consent_status, publishable_flag, consent_version, "
                            + "visibility_scope, snapshot_json, sync_version, status, updated_at) "
                            + "VALUES (?, 1, 'consent', ?, ?, 'v-search', ?, ?, 1, ?, TIMESTAMPADD(DAY, ?, CURRENT_TIMESTAMP))",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, candidateId);
            statement.setInt(2, consentStatus);
            statement.setInt(3, publishableFlag);
            statement.setInt(4, visibilityScope);
            statement.setString(5, snapshotJson);
            statement.setInt(6, status);
            statement.setInt(7, -daysAgo);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        assertThat(key).isNotNull();
        return key.longValue();
    }

    private long insertCandidate(String label) {
        String email = "p3-resume-search-" + label + "-" + UUID.randomUUID() + "@example.com";
        jdbcTemplate.update(
                "INSERT INTO candidate_user (email, password_hash, real_name, realname_verified_flag, status) VALUES (?, 'x', ?, 1, 1)",
                email,
                "王测试");
        return jdbcTemplate.queryForObject("SELECT id FROM candidate_user WHERE email = ?", Long.class, email);
    }

    private CompanyAccount registerAndLoginCompany(String label) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "p3-resume-search-company-" + label + "-" + suffix + "@example.com";
        String password = "Company@123456";
        HttpJsonResponse register = postJson(
                "/api/auth/register",
                """
                        {
                          "identity_type": "company",
                          "company_name": "P3 搜索企业 %s",
                          "license_no": "P3-RS-%s",
                          "user_name": "企业管理员",
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(label, suffix.substring(0, 12), email, password),
                "trace-p3-resume-search-register-" + label,
                null);
        assertSuccess(register, 200, "trace-p3-resume-search-register-" + label);
        return new CompanyAccount(
                register.body().at("/data/identity/company_id").asLong(),
                login("company", email, password, "trace-p3-resume-search-login-" + label));
    }

    private String registerAndLoginCandidate() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "p3-resume-search-candidate-" + suffix + "@example.com";
        String password = "Candidate@123456";
        postJson(
                "/api/auth/register",
                """
                        {
                          "identity_type": "candidate",
                          "email": "%s",
                          "password": "%s",
                          "display_name": "P3 搜索求职者"
                        }
                        """.formatted(email, password),
                "trace-p3-resume-search-candidate-register",
                null);
        return login("candidate", email, password, "trace-p3-resume-search-candidate-login");
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

    private HttpJsonResponse postJson(String path, String body, String traceId, String authorization)
            throws Exception {
        return postJson(path, body, traceId, authorization, null);
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
        return new HttpJsonResponse(response.statusCode(), response.body(), objectMapper.readTree(response.body()));
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private long countRows(String table, String where) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + where, Long.class);
        return count == null ? 0 : count;
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
                .doesNotContain("candidate_id")
                .doesNotContain("email")
                .doesNotContain("contact_wechat")
                .doesNotContain("wechat")
                .doesNotContain("resume_body")
                .doesNotContain("attachment_object_key")
                .doesNotContain("evidence")
                .doesNotContain("base_profile_json")
                .doesNotContain("education_json")
                .doesNotContain("experience_json")
                .doesNotContain("skills_json")
                .doesNotContain("raw-secret")
                .doesNotContain("raw@example.com")
                .doesNotContain("private/raw.pdf");
    }

    private record CompanyAccount(long companyId, String token) {
    }

    private record HttpJsonResponse(int status, String rawBody, JsonNode body) {
    }
}
