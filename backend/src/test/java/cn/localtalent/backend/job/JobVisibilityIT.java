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
                "localtalent.auth.jwt.secret=job-visibility-secret-change-me-with-enough-length",
                "localtalent.auth.jwt.ttl-seconds=3600"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class JobVisibilityIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_job_visibility_test")
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
    void companyReviewJobReviewAndOfflineShouldDrivePortalVisibilityAndAudit() throws Exception {
        Account company = registerAndLoginCompany();
        String operatorToken = login("operator", "operator", "LocalTalent@123456", "trace-job-operator-login");

        assertThat(queryInt("SELECT auth_status FROM company WHERE id = ?", company.companyId())).isEqualTo(1);

        HttpJsonResponse rejectCompany = postJson(
                "/api/admin/companies/review",
                """
                        {
                          "company_id": %d,
                          "audit_status": 3,
                          "memo": "license image unclear"
                        }
                        """.formatted(company.companyId()),
                "trace-company-review-reject",
                "Bearer " + operatorToken);
        assertSuccess(rejectCompany, 200, "trace-company-review-reject");
        assertThat(rejectCompany.body().at("/data/auth_status").asInt()).isEqualTo(3);
        assertThat(queryString("SELECT auth_reject_reason FROM company WHERE id = ?", company.companyId()))
                .isEqualTo("license image unclear");

        HttpJsonResponse applyAgain = postJson(
                "/api/company/apply",
                """
                        {
                          "company_name": "Prompt5 企业",
                          "license_no": "%s",
                          "industry_code": "internet",
                          "scale_code": "50-100",
                          "city_code": "310000",
                          "address": "Shanghai",
                          "company_profile": "local talent prompt5 fixture"
                        }
                        """.formatted(company.licenseNo()),
                "trace-company-apply-again",
                "Bearer " + company.token());
        assertSuccess(applyAgain, 200, "trace-company-apply-again");
        assertThat(applyAgain.body().at("/data/auth_status").asInt()).isEqualTo(1);

        long jobId = createJob(company.token(), "trace-job-create");

        HttpJsonResponse invisibleBeforeApproval = getJson(
                "/api/portal/jobs?city_code=310000&category_code=software&page=1&size=10",
                "trace-portal-before-approval",
                null);
        assertSuccess(invisibleBeforeApproval, 200, "trace-portal-before-approval");
        assertThat(invisibleBeforeApproval.body().at("/data/total").asLong()).isZero();

        HttpJsonResponse blockedJobApproval = postJson(
                "/api/admin/jobs/review",
                """
                        {
                          "job_id": %d,
                          "audit_status": 2,
                          "memo": "try before company certified"
                        }
                        """.formatted(jobId),
                "trace-job-review-before-company",
                "Bearer " + operatorToken);
        assertError(blockedJobApproval, 400, "VALID_400", "trace-job-review-before-company");
        assertThat(blockedJobApproval.body().at("/message").asText()).isEqualTo("company is not certified");

        HttpJsonResponse approveCompany = postJson(
                "/api/admin/companies/review",
                """
                        {
                          "company_id": %d,
                          "audit_status": 2,
                          "memo": "approved"
                        }
                        """.formatted(company.companyId()),
                "trace-company-review-pass",
                "Bearer " + operatorToken);
        assertSuccess(approveCompany, 200, "trace-company-review-pass");
        assertThat(approveCompany.body().at("/data/auth_status").asInt()).isEqualTo(2);

        HttpJsonResponse approveJob = postJson(
                "/api/admin/jobs/review",
                """
                        {
                          "job_id": %d,
                          "audit_status": 2,
                          "memo": "job approved"
                        }
                        """.formatted(jobId),
                "trace-job-review-pass",
                "Bearer " + operatorToken);
        assertSuccess(approveJob, 200, "trace-job-review-pass");
        assertThat(approveJob.body().at("/data/status").asInt()).isEqualTo(2);
        assertThat(approveJob.body().at("/data/audit_status").asInt()).isEqualTo(2);

        HttpJsonResponse visibleAfterApproval = getJson(
                "/api/portal/jobs?city_code=310000&category_code=software&page=1&size=10",
                "trace-portal-after-approval",
                null);
        assertSuccess(visibleAfterApproval, 200, "trace-portal-after-approval");
        assertThat(visibleAfterApproval.body().at("/data/total").asLong()).isEqualTo(1);
        assertThat(visibleAfterApproval.body().at("/data/job_list/0/job_id").asLong()).isEqualTo(jobId);
        assertPublicJobBodySafe(visibleAfterApproval);

        HttpJsonResponse portalDetail = getJson(
                "/api/portal/jobs/" + jobId,
                "trace-portal-job-detail",
                null);
        assertSuccess(portalDetail, 200, "trace-portal-job-detail");
        assertThat(portalDetail.body().at("/data/title").asText()).isEqualTo("Java 工程师");
        assertPublicJobBodySafe(portalDetail);

        HttpJsonResponse offlineJob = postJson(
                "/api/company/jobs/" + jobId + "/status",
                """
                        {
                          "action": "offline",
                          "reason": "position filled"
                        }
                        """,
                "trace-job-offline",
                "Bearer " + company.token());
        assertSuccess(offlineJob, 200, "trace-job-offline");
        assertThat(offlineJob.body().at("/data/status").asInt()).isEqualTo(3);

        HttpJsonResponse invisibleAfterOffline = getJson(
                "/api/portal/jobs?city_code=310000&category_code=software&page=1&size=10",
                "trace-portal-after-offline",
                null);
        assertSuccess(invisibleAfterOffline, 200, "trace-portal-after-offline");
        assertThat(invisibleAfterOffline.body().at("/data/total").asLong()).isZero();

        assertThat(countAudit("trace-company-review-reject", "company", "company_review_reject")).isEqualTo(1);
        assertThat(countAudit("trace-company-review-pass", "company", "company_review_pass")).isEqualTo(1);
        assertThat(countAudit("trace-job-review-pass", "job_post", "job_review_pass")).isEqualTo(1);
        assertThat(countAudit("trace-job-offline", "job_post", "job_offline")).isEqualTo(1);
    }

    @Test
    void portalJobSearchShouldFilterPublicVisibleCertifiedJobsOnly() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        long certifiedCompanyId = insertCompany(
                "Prompt18 认证企业 " + suffix,
                "P18C-" + suffix.substring(0, 16),
                2,
                "internet",
                "50-100",
                "310000");
        long uncertifiedCompanyId = insertCompany(
                "Prompt18 未认证企业 " + suffix,
                "P18U-" + suffix.substring(0, 16),
                1,
                "internet",
                "50-100",
                "310000");
        long visibleJobId = insertJob(
                certifiedCompanyId,
                "Prompt18 Java 架构师 " + suffix,
                "p18-software",
                "310000",
                20000,
                32000,
                2,
                2);
        long deletedJobId = insertJob(
                certifiedCompanyId,
                "Prompt18 已删除职位 " + suffix,
                "p18-software",
                "310000",
                20000,
                32000,
                2,
                2);
        jdbcTemplate.update(
                "UPDATE job_post SET deleted_at = CURRENT_TIMESTAMP, deleted_by = 1, delete_reason = 'visibility fixture' WHERE id = ?",
                deletedJobId);
        insertJob(certifiedCompanyId, "Prompt18 待审核职位 " + suffix, "p18-software", "310000", 20000, 32000, 1, 1);
        insertJob(certifiedCompanyId, "Prompt18 下线职位 " + suffix, "p18-software", "310000", 20000, 32000, 3, 2);
        insertJob(uncertifiedCompanyId, "Prompt18 未认证职位 " + suffix, "p18-software", "310000", 20000, 32000, 2, 2);

        HttpJsonResponse filtered = getJson(
                ("/api/portal/jobs?keyword=%s&city_code=310000&category_code=p18-software"
                        + "&salary_min=18000&salary_max=35000&industry_code=internet&scale_code=50-100"
                        + "&updated_within=30&sort=salary_desc&page=1&size=10").formatted(suffix),
                "trace-p18-portal-job-search",
                null);
        assertSuccess(filtered, 200, "trace-p18-portal-job-search");
        assertThat(filtered.body().at("/data/total").asLong()).isEqualTo(1);
        assertThat(filtered.body().at("/data/job_list/0/job_id").asLong()).isEqualTo(visibleJobId);
        assertPublicJobBodySafe(filtered);

        HttpJsonResponse deletedDetail = getJson(
                "/api/portal/jobs/" + deletedJobId,
                "trace-p18-portal-deleted-job-detail",
                null);
        assertError(deletedDetail, 404, "NOT_FOUND_404", "trace-p18-portal-deleted-job-detail");

        HttpJsonResponse companyDetail = getJson(
                "/api/portal/companies/" + certifiedCompanyId,
                "trace-p18-company-detail-with-deleted-job",
                null);
        assertSuccess(companyDetail, 200, "trace-p18-company-detail-with-deleted-job");
        assertThat(companyDetail.body().at("/data/open_job_count").asLong()).isEqualTo(1);
        assertThat(companyDetail.body().at("/data/open_jobs/0/job_id").asLong()).isEqualTo(visibleJobId);
        assertThat(companyDetail.body().toString()).doesNotContain("\"job_id\":" + deletedJobId);

        HttpJsonResponse salaryMismatch = getJson(
                "/api/portal/jobs?keyword=%s&salary_min=40000&page=1&size=10".formatted(suffix),
                "trace-p18-portal-salary-filter",
                null);
        assertSuccess(salaryMismatch, 200, "trace-p18-portal-salary-filter");
        assertThat(salaryMismatch.body().at("/data/total").asLong()).isZero();

        HttpJsonResponse industryMismatch = getJson(
                "/api/portal/jobs?keyword=%s&industry_code=manufacturing&page=1&size=10".formatted(suffix),
                "trace-p18-portal-industry-filter",
                null);
        assertSuccess(industryMismatch, 200, "trace-p18-portal-industry-filter");
        assertThat(industryMismatch.body().at("/data/total").asLong()).isZero();
    }

    private Account registerAndLoginCompany() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "job-visible-company-" + suffix + "@example.com";
        String licenseNo = "JV-" + suffix.substring(0, 16);
        String password = "Company@123456";
        HttpJsonResponse registerResponse = postJson(
                "/api/auth/register",
                """
                        {
                          "identity_type": "company",
                          "company_name": "Prompt5 企业",
                          "license_no": "%s",
                          "user_name": "企业管理员",
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(licenseNo, email, password),
                "trace-job-company-register",
                null);
        assertSuccess(registerResponse, 200, "trace-job-company-register");
        long userId = registerResponse.body().at("/data/identity/user_id").asLong();
        long companyId = registerResponse.body().at("/data/identity/company_id").asLong();
        String token = login("company", email, password, "trace-job-company-login");
        return new Account(userId, companyId, licenseNo, token);
    }

    private void assertPublicJobBodySafe(HttpJsonResponse response) {
        assertThat(response.body().toString())
                .doesNotContain("contact_name")
                .doesNotContain("contact_mobile")
                .doesNotContain("contact_phone")
                .doesNotContain("contact_email")
                .doesNotContain("contact_wechat")
                .doesNotContain("contact_hidden")
                .doesNotContain("notify_enabled")
                .doesNotContain("resume_subscription_enabled")
                .doesNotContain("审核材料")
                .doesNotContain("attachment_object_key")
                .doesNotContain("object_key")
                .doesNotContain("支付链接")
                .doesNotContain("联系解锁");
    }

    private long createJob(String token, String traceId) throws Exception {
        HttpJsonResponse response = postJson(
                "/api/company/jobs",
                """
                        {
                          "title": "Java 工程师",
                          "category_code": "software",
                          "city_code": "310000",
                          "salary_min": 18000,
                          "salary_max": 26000,
                          "job_desc": "Build local talent services."
                        }
                        """,
                traceId,
                "Bearer " + token);
        assertSuccess(response, 200, traceId);
        assertThat(response.body().at("/data/status").asInt()).isEqualTo(1);
        assertThat(response.body().at("/data/audit_status").asInt()).isEqualTo(1);
        return response.body().at("/data/job_id").asLong();
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

    private String queryString(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, String.class, args);
    }

    private int countAudit(String traceId, String bizType, String actionType) {
        return queryInt(
                "SELECT COUNT(*) FROM audit_log WHERE trace_id = ? AND biz_type = ? AND action_type = ?",
                traceId,
                bizType,
                actionType);
    }

    private long insertCompany(
            String companyName,
            String licenseNo,
            int authStatus,
            String industryCode,
            String scaleCode,
            String cityCode) {
        jdbcTemplate.update(
                "INSERT INTO company "
                        + "(company_name, license_no, industry_code, scale_code, city_code, auth_status) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                companyName,
                licenseNo,
                industryCode,
                scaleCode,
                cityCode,
                authStatus);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM company WHERE license_no = ?",
                Long.class,
                licenseNo);
        return id == null ? 0 : id;
    }

    private long insertJob(
            long companyId,
            String title,
            String categoryCode,
            String cityCode,
            int salaryMin,
            int salaryMax,
            int status,
            int auditStatus) {
        jdbcTemplate.update(
                "INSERT INTO job_post "
                        + "(company_id, source_type, title, category_code, city_code, salary_min, salary_max, "
                        + "job_desc, status, audit_status, published_at, status_changed_at) "
                        + "VALUES (?, 1, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                companyId,
                title,
                categoryCode,
                cityCode,
                salaryMin,
                salaryMax,
                "Prompt18 public search fixture.",
                status,
                auditStatus);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM job_post WHERE title = ?",
                Long.class,
                title);
        return id == null ? 0 : id;
    }

    private record Account(long userId, long companyId, String licenseNo, String token) {
    }

    private record HttpJsonResponse(int status, String traceId, JsonNode body) {
    }
}
