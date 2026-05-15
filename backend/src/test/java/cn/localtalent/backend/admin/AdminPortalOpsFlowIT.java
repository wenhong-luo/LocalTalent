package cn.localtalent.backend.admin;

import static org.assertj.core.api.Assertions.assertThat;

import cn.localtalent.backend.company.workbench.infrastructure.CompanyStyleImageStorageService;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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

    @Autowired
    private CompanyStyleImageStorageService imageStorageService;

    @TestConfiguration
    static class ImageStorageTestConfiguration {
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
    void operatorShouldConfigureHomeSlotsAndPublicPortalShouldOnlyReturnActiveSlots() throws Exception {
        String operatorToken = login("operator", "operator", "LocalTalent@123456", "trace-home-slot-operator-login");
        String auditorToken = login("operator", insertAuditor(), "LocalTalent@123456", "trace-home-slot-auditor-login");

        assertError(postJson(
                "/api/admin/home-slots",
                homeSlotBody("home_hero_banner", "审计员不能写", 1),
                "trace-home-slot-auditor-denied",
                "Bearer " + auditorToken,
                "idem-home-slot-auditor-denied"), 403, "AUTHZ_403", "trace-home-slot-auditor-denied");

        HttpJsonResponse created = postJson(
                "/api/admin/home-slots",
                homeSlotBody("home_hero_banner", "首页首屏运营位", 1),
                "trace-home-slot-create",
                "Bearer " + operatorToken,
                "idem-home-slot-create");
        assertThat(created.status()).as(created.rawBody()).isEqualTo(200);
        assertSuccess(created, 200, "trace-home-slot-create");
        long slotId = created.body().at("/data/slot_id").asLong();
        assertThat(created.body().at("/data/operator_id").isMissingNode()).isTrue();
        assertThat(created.body().at("/data/title").asText()).isEqualTo("首页首屏运营位");
        assertThat(countAudit("trace-home-slot-create", "portal_home_operation_slot", "home_slot_create")).isEqualTo(1);

        when(imageStorageService.get(anyString())).thenReturn("home-slot-image".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        HttpJsonResponse uploaded = postMultipart(
                "/api/admin/home-slots/" + slotId + "/image",
                "file",
                "hero.webp",
                "image/webp",
                "hero-image".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "trace-home-slot-image-upload",
                "Bearer " + operatorToken,
                "idem-home-slot-image-upload");
        assertSuccess(uploaded, 200, "trace-home-slot-image-upload");
        assertThat(uploaded.rawBody())
                .contains("hero.webp")
                .contains("/api/admin/home-slots/" + slotId + "/image/content")
                .doesNotContain("image_object_key")
                .doesNotContain("portal-home-slots/")
                .doesNotContain("presigned");
        assertThat(uploaded.body().at("/data/has_image").asBoolean()).isTrue();
        assertThat(countAudit("trace-home-slot-image-upload", "portal_home_operation_slot", "home_slot_image_upload")).isEqualTo(1);
        verify(imageStorageService).put(anyString(), any(byte[].class), anyString());

        HttpResponse<byte[]> adminImage = getBytes(
                "/api/admin/home-slots/" + slotId + "/image/content",
                "trace-home-slot-image-admin-content",
                "Bearer " + auditorToken);
        assertThat(adminImage.statusCode()).isEqualTo(200);
        assertThat(new String(adminImage.body(), java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("home-slot-image");
        assertThat(queryInt(
                "SELECT COUNT(*) FROM field_access_log WHERE trace_id = ? AND biz_type = ? AND access_type = ?",
                "trace-home-slot-image-admin-content",
                "portal_home_operation_slot",
                "HOME_SLOT_IMAGE_READ")).isEqualTo(1);

        HttpJsonResponse list = getJson(
                "/api/admin/home-slots?slot_code=home_hero_banner&page=1&size=10",
                "trace-home-slot-admin-list",
                "Bearer " + auditorToken);
        assertSuccess(list, 200, "trace-home-slot-admin-list");
        assertThat(list.body().at("/data/slot_list/0/slot_id").asLong()).isEqualTo(slotId);

        HttpJsonResponse publicList = getJson(
                "/api/portal/home-slots?slot_codes=home_hero_banner",
                "trace-home-slot-public-visible",
                null);
        assertSuccess(publicList, 200, "trace-home-slot-public-visible");
        assertThat(publicList.body().at("/data/slot_list/0/title").asText()).isEqualTo("首页首屏运营位");
        assertThat(publicList.body().at("/data/slot_list/0/link_url").asText()).isEqualTo("/jobs");
        assertThat(publicList.body().at("/data/slot_list/0/image_url").asText()).isEqualTo("/api/portal/home-slots/" + slotId + "/image");
        assertPublicBodySafe(publicList);

        HttpResponse<byte[]> publicImage = getBytes(
                "/api/portal/home-slots/" + slotId + "/image",
                "trace-home-slot-image-public-content",
                null);
        assertThat(publicImage.statusCode()).isEqualTo(200);
        assertThat(new String(publicImage.body(), java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("home-slot-image");

        assertError(postMultipart(
                "/api/admin/home-slots/" + slotId + "/image",
                "file",
                "auditor.png",
                "image/png",
                "denied".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "trace-home-slot-image-auditor-denied",
                "Bearer " + auditorToken,
                "idem-home-slot-image-auditor-denied"), 403, "AUTHZ_403", "trace-home-slot-image-auditor-denied");

        HttpJsonResponse imageDeleted = deleteJson(
                "/api/admin/home-slots/" + slotId + "/image",
                "trace-home-slot-image-delete",
                "Bearer " + operatorToken,
                "idem-home-slot-image-delete");
        assertSuccess(imageDeleted, 200, "trace-home-slot-image-delete");
        assertThat(imageDeleted.body().at("/data/has_image").asBoolean()).isFalse();
        assertThat(countAudit("trace-home-slot-image-delete", "portal_home_operation_slot", "home_slot_image_delete")).isEqualTo(1);

        HttpJsonResponse offline = postJson(
                "/api/admin/home-slots/" + slotId + "/offline",
                "{}",
                "trace-home-slot-offline",
                "Bearer " + operatorToken,
                "idem-home-slot-offline");
        assertSuccess(offline, 200, "trace-home-slot-offline");
        assertThat(offline.body().at("/data/status").asInt()).isZero();
        assertThat(countAudit("trace-home-slot-offline", "portal_home_operation_slot", "home_slot_offline")).isEqualTo(1);

        HttpJsonResponse hidden = getJson(
                "/api/portal/home-slots?slot_codes=home_hero_banner",
                "trace-home-slot-public-hidden",
                null);
        assertSuccess(hidden, 200, "trace-home-slot-public-hidden");
        assertThat(hidden.body().at("/data/slot_list")).isEmpty();
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
        String boundary = "----LocalTalentBoundary" + java.util.UUID.randomUUID();
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
        return sendLocal(builder.build());
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
        return sendLocal(builder.build());
    }

    private HttpResponse<byte[]> getBytes(String path, String traceId, String authorization) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("X-Trace-Id", traceId)
                .GET();
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private HttpJsonResponse sendLocal(HttpRequest request) throws Exception {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return new HttpJsonResponse(
                response.statusCode(),
                response.headers().firstValue("X-Trace-Id").orElse(null),
                response.body(),
                objectMapper.readTree(response.body()));
    }

    private java.net.URI uri(String path) {
        return java.net.URI.create("http://127.0.0.1:" + port + path);
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

    private String homeSlotBody(String slotCode, String title, int displayOrder) {
        return """
                {
                  "slot_code": "%s",
                  "title": "%s",
                  "subtitle": "仅配置首页运营展示，不做广告售卖",
                  "image_url": "/demo/home-ad-full.svg",
                  "image_alt": "LocalTalent 首页运营位",
                  "link_type": "internal",
                  "link_url": "/jobs",
                  "display_order": %d,
                  "status": 1
                }
                """.formatted(slotCode, title, displayOrder);
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
