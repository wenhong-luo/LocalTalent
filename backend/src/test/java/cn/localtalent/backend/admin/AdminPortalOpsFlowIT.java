package cn.localtalent.backend.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.PreparedStatement;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
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
                "localtalent.auth.jwt.secret=p29-flow-secret-change-me-with-enough-length",
                "localtalent.auth.jwt.ttl-seconds=3600",
                "localtalent.phase3.operator-portal-ops=true"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminPortalOpsFlowIT extends AdminIntegrationTestSupport {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_p29_flow_test")
            .withUsername("localtalent")
            .withPassword("localtalent");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    void operatorShouldConfigureRecommendationsAndPublicPortalShouldHideInvalidTargets() throws Exception {
        String operatorToken = login("operator", "operator", "LocalTalent@123456", "trace-p29-operator-login");
        String auditorToken = login("operator", insertAuditor(), "LocalTalent@123456", "trace-p29-auditor-login");
        Account company = registerAndLoginCompany("p29-recommend");
        jdbcTemplate.update("UPDATE company SET auth_status = 2 WHERE id = ?", company.companyId());
        long jobId = createJob(company.token(), "Prompt29 合规热招职位", "trace-p29-job-create");
        jdbcTemplate.update("UPDATE job_post SET status = 2, audit_status = 2, published_at = CURRENT_TIMESTAMP WHERE id = ?", jobId);

        assertSuccess(getJson(
                "/api/admin/ops/overview",
                "trace-p29-overview",
                "Bearer " + operatorToken), 200, "trace-p29-overview");

        assertError(postJson(
                "/api/admin/recommendations",
                recommendationBody("home_hot_jobs", "job", jobId, "审计员不能写"),
                "trace-p29-auditor-rec-denied",
                "Bearer " + auditorToken,
                "idem-p29-auditor-rec-denied"), 403, "AUTHZ_403", "trace-p29-auditor-rec-denied");

        HttpJsonResponse created = postJson(
                "/api/admin/recommendations",
                recommendationBody("home_hot_jobs", "job", jobId, "Prompt29 首页热招"),
                "trace-p29-rec-create",
                "Bearer " + operatorToken,
                "idem-p29-rec-create");
        assertSuccess(created, 200, "trace-p29-rec-create");
        long recommendationId = created.body().at("/data/recommendation_id").asLong();
        assertThat(created.body().at("/data/target_valid").asBoolean()).isTrue();
        assertThat(countAudit("trace-p29-rec-create", "portal_recommendation", "recommendation_create")).isEqualTo(1);

        HttpJsonResponse publicList = getJson(
                "/api/portal/recommendations?slot_code=home_hot_jobs",
                "trace-p29-public-visible",
                null);
        assertSuccess(publicList, 200, "trace-p29-public-visible");
        assertThat(publicList.body().at("/data/recommendation_list/0/title").asText()).isEqualTo("Prompt29 首页热招");
        assertPublicBodySafe(publicList);

        jdbcTemplate.update("UPDATE job_post SET status = 3 WHERE id = ?", jobId);
        HttpJsonResponse hidden = getJson(
                "/api/portal/recommendations?slot_code=home_hot_jobs",
                "trace-p29-public-hidden",
                null);
        assertSuccess(hidden, 200, "trace-p29-public-hidden");
        assertThat(hidden.body().at("/data/recommendation_list")).isEmpty();

        HttpJsonResponse offline = postJson(
                "/api/admin/recommendations/" + recommendationId + "/offline",
                "{}",
                "trace-p29-rec-offline",
                "Bearer " + operatorToken,
                "idem-p29-rec-offline");
        assertSuccess(offline, 200, "trace-p29-rec-offline");
        assertThat(offline.body().at("/data/status").asInt()).isZero();
        assertThat(countAudit("trace-p29-rec-offline", "portal_recommendation", "recommendation_offline")).isEqualTo(1);
    }

    @Test
    void operatorShouldHandleRiskReviewAndAuditorShouldRemainReadOnly() throws Exception {
        String operatorToken = login("operator", "operator", "LocalTalent@123456", "trace-p29-risk-operator-login");
        String auditorToken = login("operator", insertAuditor(), "LocalTalent@123456", "trace-p29-risk-auditor-login");
        long riskId = insertRiskReview();

        assertSuccess(getJson(
                "/api/admin/risk-reviews?page=1&size=10",
                "trace-p29-risk-list",
                "Bearer " + auditorToken), 200, "trace-p29-risk-list");
        assertSuccess(getJson(
                "/api/admin/risk-reviews/" + riskId,
                "trace-p29-risk-detail",
                "Bearer " + auditorToken), 200, "trace-p29-risk-detail");

        assertError(postJson(
                "/api/admin/risk-reviews/" + riskId + "/handle",
                "{\"status\":2,\"decision\":\"auditor denied\"}",
                "trace-p29-risk-auditor-denied",
                "Bearer " + auditorToken,
                "idem-p29-risk-auditor-denied"), 403, "AUTHZ_403", "trace-p29-risk-auditor-denied");

        HttpJsonResponse handled = postJson(
                "/api/admin/risk-reviews/" + riskId + "/handle",
                "{\"status\":2,\"decision\":\"已核查为低风险公开内容\"}",
                "trace-p29-risk-handle",
                "Bearer " + operatorToken,
                "idem-p29-risk-handle");
        assertSuccess(handled, 200, "trace-p29-risk-handle");
        assertThat(handled.body().at("/data/status").asInt()).isEqualTo(2);
        assertThat(countAudit("trace-p29-risk-handle", "risk_review", "risk_review_handle")).isEqualTo(1);
        assertPublicBodySafe(handled);
    }

    private long insertRiskReview() {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO risk_review "
                            + "(risk_type, target_type, target_id, severity, status, title, summary) "
                            + "VALUES ('content_risk', 'content', 1001, 'medium', 0, 'Prompt29 风险任务', '仅记录摘要')",
                    Statement.RETURN_GENERATED_KEYS);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0 : key.longValue();
    }

    private String recommendationBody(String slotCode, String targetType, long targetId, String title) {
        return """
                {
                  "slot_code": "%s",
                  "target_type": "%s",
                  "target_id": %d,
                  "title_override": "%s",
                  "summary_override": "只展示公开字段",
                  "display_order": 1,
                  "status": 1
                }
                """.formatted(slotCode, targetType, targetId, title);
    }

    private void assertPublicBodySafe(HttpJsonResponse response) {
        String body = response.body().toString();
        assertThat(body)
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
                .doesNotContain("审核材料")
                .doesNotContain("营业执照附件")
                .doesNotContain("报名名单")
                .doesNotContain("签到证据");
    }
}
