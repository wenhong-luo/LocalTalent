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
import java.util.List;
import java.util.UUID;
import cn.localtalent.backend.company.workbench.infrastructure.CompanyStyleImageStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "localtalent.auth.jwt.secret=company-workbench-flow-secret-change-me",
                "localtalent.auth.jwt.ttl-seconds=3600",
                "localtalent.phase3.company-workbench=true",
                "localtalent.phase3.company-style-upload=true",
                "localtalent.phase3.company-logo-upload=true"
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

    @Autowired
    private CompanyStyleImageStorageService styleImageStorageService;

    @TestConfiguration
    static class StyleImageStorageTestConfiguration {
        @Bean
        @Primary
        CompanyStyleImageStorageService companyStyleImageStorageService() {
            return mock(CompanyStyleImageStorageService.class);
        }
    }

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
        assertThat(overview.body().at("/data/features/company_style_upload_enabled").asBoolean()).isTrue();
        assertThat(overview.body().at("/data/features/company_logo_upload_enabled").asBoolean()).isTrue();

        when(styleImageStorageService.get(anyString())).thenReturn("fake-image".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        HttpJsonResponse emptyLogo = getJson(
                "/api/company/workbench/logo",
                "trace-p28-logo-empty",
                "Bearer " + company.token());
        assertSuccess(emptyLogo, 200, "trace-p28-logo-empty");
        assertThat(emptyLogo.body().at("/data/has_logo").asBoolean()).isFalse();

        HttpJsonResponse uploadedLogo = postMultipart(
                "/api/company/workbench/logo",
                "file",
                "logo.png",
                "image/png",
                "logo-image".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "trace-p28-logo-upload",
                "Bearer " + company.token(),
                "idem-p28-logo-upload-001");
        assertSuccess(uploadedLogo, 200, "trace-p28-logo-upload");
        assertThat(uploadedLogo.rawBody())
                .contains("logo.png")
                .contains("/api/company/workbench/logo/content")
                .doesNotContain("object_key")
                .doesNotContain("company-logo-assets/")
                .doesNotContain("presigned");

        HttpResponse<byte[]> logoContent = getBytes(
                "/api/company/workbench/logo/content",
                "trace-p28-logo-content",
                "Bearer " + company.token());
        assertThat(logoContent.statusCode()).isEqualTo(200);
        assertThat(new String(logoContent.body(), java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("fake-image");
        HttpJsonResponse crossCompanyLogo = getJson(
                "/api/company/workbench/logo/content",
                "trace-p28-logo-cross-company",
                "Bearer " + otherCompany.token());
        assertError(crossCompanyLogo, 404, "NOT_FOUND_404", "trace-p28-logo-cross-company");
        HttpJsonResponse deletedLogo = deleteJson(
                "/api/company/workbench/logo",
                "trace-p28-logo-delete",
                "Bearer " + company.token(),
                "idem-p28-logo-delete-001");
        assertSuccess(deletedLogo, 200, "trace-p28-logo-delete");
        assertThat(deletedLogo.body().at("/data/has_logo").asBoolean()).isFalse();

        HttpJsonResponse emptyStyleImages = getJson(
                "/api/company/workbench/style-images",
                "trace-p28-style-empty",
                "Bearer " + company.token());
        assertSuccess(emptyStyleImages, 200, "trace-p28-style-empty");
        assertThat(emptyStyleImages.body().at("/data/total").asLong()).isZero();

        HttpJsonResponse uploadedStyleImage = postMultipart(
                "/api/company/workbench/style-images",
                "file",
                "office.webp",
                "image/webp",
                "style-image".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "trace-p28-style-upload",
                "Bearer " + company.token(),
                "idem-p28-style-upload-001");
        assertSuccess(uploadedStyleImage, 200, "trace-p28-style-upload");
        assertThat(uploadedStyleImage.rawBody())
                .contains("office.webp")
                .doesNotContain("object_key")
                .doesNotContain("company-style-images/")
                .doesNotContain("presigned");
        long styleImageId = uploadedStyleImage.body().at("/data/image_id").asLong();
        verify(styleImageStorageService, org.mockito.Mockito.times(2)).put(anyString(), any(byte[].class), anyString());

        HttpResponse<byte[]> styleContent = getBytes(
                "/api/company/workbench/style-images/" + styleImageId + "/content",
                "trace-p28-style-content",
                "Bearer " + company.token());
        assertThat(styleContent.statusCode()).isEqualTo(200);
        assertThat(new String(styleContent.body(), java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("fake-image");
        HttpJsonResponse crossCompanyStyle = getJson(
                "/api/company/workbench/style-images/" + styleImageId + "/content",
                "trace-p28-style-cross-company",
                "Bearer " + otherCompany.token());
        assertError(crossCompanyStyle, 404, "NOT_FOUND_404", "trace-p28-style-cross-company");

        HttpJsonResponse sortedStyleImages = putJson(
                "/api/company/workbench/style-images/order",
                """
                        {
                          "image_ids": [%d]
                        }
                        """.formatted(styleImageId),
                "trace-p28-style-order",
                "Bearer " + company.token(),
                "idem-p28-style-order-001");
        assertSuccess(sortedStyleImages, 200, "trace-p28-style-order");
        HttpJsonResponse deletedStyleImage = deleteJson(
                "/api/company/workbench/style-images/" + styleImageId,
                "trace-p28-style-delete",
                "Bearer " + company.token(),
                "idem-p28-style-delete-001");
        assertSuccess(deletedStyleImage, 200, "trace-p28-style-delete");

        HttpJsonResponse profile = putJson(
                "/api/company/workbench/profile",
                """
                        {
                          "company_name": "P28 生产化企业",
                          "industry_code": "internet",
                          "nature_code": "private",
                          "scale_code": "50-200",
                          "city_code": "310000",
                          "address": "仅企业私有域可见地址",
                          "company_profile": "企业工作台私有简介。",
                          "registered_capital_amount": "1000",
                          "registered_capital_unit": "cny_10k",
                          "website_url": "https://p28.example.local",
                          "benefit_codes": ["five_insurance", "annual_leave", "meal_allowance"],
                          "contact_name": "王招聘",
                          "contact_mobile": "18877776666",
                          "contact_mobile_hidden": true,
                          "contact_wechat": "p28-hr",
                          "contact_wechat_same_mobile": false,
                          "contact_phone": "021-88889999",
                          "contact_email": "hr-p28@example.local",
                          "contact_qq": "123456"
                        }
                        """,
                "trace-p28-profile",
                "Bearer " + company.token(),
                "idem-p28-profile-001");
        assertSuccess(profile, 200, "trace-p28-profile");
        assertThat(profile.body().at("/data/company_name").asText()).isEqualTo("P28 生产化企业");
        assertThat(profile.body().at("/data/registered_capital_amount").asText()).isEqualTo("1000");
        assertThat(profile.body().at("/data/benefit_codes/0").asText()).isEqualTo("five_insurance");
        assertThat(profile.body().at("/data/contact_name").asText()).isEqualTo("王招聘");
        assertThat(profile.body().at("/data/contact_mobile_hidden").asBoolean()).isTrue();

        HttpJsonResponse certification = postJson(
                "/api/company/workbench/certification",
                """
                        {
                          "company_name": "P28 生产化企业",
                          "license_no": "P28-LIC-%s",
                          "industry_code": "internet",
                          "nature_code": "private",
                          "scale_code": "50-200",
                          "city_code": "310000",
                          "address": "认证私有地址",
                          "company_profile": "认证提交简介。",
                          "registered_capital_amount": "1500",
                          "registered_capital_unit": "cny_10k",
                          "website_url": "https://cert.example.local",
                          "benefit_codes": ["five_insurance", "weekend_double"],
                          "contact_name": "认证联系人",
                          "contact_mobile": "18899990000",
                          "contact_mobile_hidden": true,
                          "contact_wechat": "cert-hr",
                          "contact_wechat_same_mobile": false,
                          "contact_phone": "021-99998888",
                          "contact_email": "cert@example.local",
                          "contact_qq": "654321",
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
        assertThat(certification.body().at("/data/registered_capital_amount").asText()).isEqualTo("1500");
        assertThat(certification.body().at("/data/benefit_codes/1").asText()).isEqualTo("weekend_double");
        assertThat(certification.body().at("/data/contact_name").asText()).isEqualTo("认证联系人");
        assertThat(certification.rawBody()).doesNotContain("must-not-store");

        HttpJsonResponse draftJob = postJson(
                "/api/company/workbench/jobs",
                jobBody("P28 Java 工程师"),
                "trace-p28-job-create",
                "Bearer " + company.token(),
                "idem-p28-job-create-001");
        assertSuccess(draftJob, 200, "trace-p28-job-create");
        long jobId = draftJob.body().at("/data/job_id").asLong();
        assertThat(draftJob.body().at("/data/job_nature_code").asText()).isEqualTo("full_time");
        assertThat(draftJob.body().at("/data/category_name").asText()).isEqualTo("互联网/电子商务");
        assertThat(draftJob.body().at("/data/work_region_path").asText()).isEqualTo("上海 / 上海市 / 浦东新区");
        assertThat(draftJob.body().at("/data/welfare_codes/1").asText()).isEqualTo("weekend_double");
        assertThat(draftJob.body().at("/data/contact_mobile").asText()).isEqualTo("18877776666");

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

        HttpJsonResponse publicJob = getJson(
                "/api/portal/jobs/" + jobId,
                "trace-p28-public-job",
                null);
        assertSuccess(publicJob, 200, "trace-p28-public-job");
        assertThat(publicJob.rawBody())
                .doesNotContain("contact_mobile")
                .doesNotContain("contact_email")
                .doesNotContain("contact_wechat")
                .doesNotContain("notify_enabled")
                .doesNotContain("resume_subscription_enabled")
                .doesNotContain("18877776666")
                .doesNotContain("hr-job@example.local")
                .doesNotContain("job-wechat");

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

        assertThat(countRows("audit_log", "action_type IN ('company_logo_upload','company_logo_delete','company_style_image_upload','company_style_image_order','company_style_image_delete','company_profile_save','company_certification_submit','job_create','job_submit_review','application_stage_change','workbench_interview_invite','export_apply')"))
                .isGreaterThanOrEqualTo(10);
        String jobAudit = jdbcTemplate.queryForObject(
                "SELECT CAST(after_json AS CHAR) FROM audit_log WHERE action_type = 'job_create' AND biz_id = ? ORDER BY id DESC LIMIT 1",
                String.class,
                jobId);
        assertThat(jobAudit)
                .contains("contact_mobile_present")
                .doesNotContain("18877776666")
                .doesNotContain("hr-job@example.local")
                .doesNotContain("job-wechat");
        assertThat(countRows("field_access_log", "biz_type = 'job_application' AND access_type = 'COMPANY_APPLICATION_DETAIL'"))
                .isGreaterThanOrEqualTo(1);
        assertThat(countRows("field_access_log", "biz_type = 'company_style_image' AND access_type = 'COMPANY_STYLE_IMAGE_READ'"))
                .isGreaterThanOrEqualTo(1);
        assertThat(countRows("field_access_log", "biz_type = 'company_logo_asset' AND access_type = 'COMPANY_LOGO_READ'"))
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
                  "job_nature_code": "full_time",
                  "category_code": "software",
                  "category_name": "互联网/电子商务",
                  "experience_code": "1_3_years",
                  "education_code": "college",
                  "recruit_count": 3,
                  "city_code": "310000",
                  "work_region_path": "上海 / 上海市 / 浦东新区",
                  "address": "上海市浦东新区演示大道 100 号",
                  "salary_min": 15000,
                  "salary_max": 25000,
                  "salary_negotiable": false,
                  "welfare_codes": ["five_insurance", "weekend_double"],
                  "department_name": "研发部",
                  "age_min": 22,
                  "age_max": 40,
                  "age_unlimited": false,
                  "recruitment_time_code": "long_term",
                  "contact_mode": "custom",
                  "contact_name": "王招聘",
                  "contact_mobile": "18877776666",
                  "contact_phone": "021-88889999",
                  "contact_email": "hr-job@example.local",
                  "contact_wechat": "job-wechat",
                  "contact_hidden": true,
                  "notify_enabled": true,
                  "resume_subscription_enabled": true,
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

    private HttpJsonResponse deleteJson(String path, String traceId, String authorization, String idempotencyKey)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("X-Trace-Id", traceId)
                .DELETE();
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        if (idempotencyKey != null) {
            builder.header("X-Idempotency-Key", idempotencyKey);
        }
        return send(builder.build());
    }

    private HttpJsonResponse postMultipart(
            String path,
            String fieldName,
            String fileName,
            String contentType,
            byte[] content,
            String traceId,
            String authorization,
            String idempotencyKey
    ) throws Exception {
        String boundary = "----LocalTalentBoundary" + UUID.randomUUID();
        byte[] prefix = (
                "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"\r\n"
                        + "Content-Type: " + contentType + "\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("X-Trace-Id", traceId)
                .POST(HttpRequest.BodyPublishers.ofByteArrays(List.of(prefix, content, suffix)));
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

    private HttpResponse<byte[]> getBytes(String path, String traceId, String authorization) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path)).header("X-Trace-Id", traceId).GET();
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
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
