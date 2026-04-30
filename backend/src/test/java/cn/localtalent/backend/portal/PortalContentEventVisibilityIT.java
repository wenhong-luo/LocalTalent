package cn.localtalent.backend.portal;

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
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "localtalent.auth.jwt.secret=portal-content-secret-change-me-with-enough-length",
                "localtalent.auth.jwt.ttl-seconds=3600"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PortalContentEventVisibilityIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_portal_content_test")
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
    void portalContentsShouldExposePublishedPublicTextOnly() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        long publishedPolicyId = insertContent(
                "policy",
                "Prompt21 就业政策 " + suffix,
                "公开政策摘要",
                "<p>公开政策正文</p><script>13900000000 secret@example.com evidence</script>",
                1);
        insertContent(
                "policy",
                "Prompt21 下线政策 " + suffix,
                "下线摘要不应公开",
                "<p>hidden resume_body attachment_object_key</p>",
                0);
        insertContent(
                "hr_tool",
                "Prompt21 HR 工具 " + suffix,
                "HR 工具摘要",
                "<p>HR 工具正文</p>",
                1);

        HttpJsonResponse list = getJson(
                "/api/portal/contents?content_type=policy&page=1&size=10",
                "trace-p21-content-list");
        assertSuccess(list, 200, "trace-p21-content-list");
        assertThat(list.body().at("/data/total").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(list.rawBody()).contains("Prompt21 就业政策 " + suffix);
        assertThat(list.rawBody()).doesNotContain("Prompt21 下线政策 " + suffix);

        HttpJsonResponse detail = getJson(
                "/api/portal/contents/" + publishedPolicyId,
                "trace-p21-content-detail");
        assertSuccess(detail, 200, "trace-p21-content-detail");
        assertThat(detail.body().at("/data/content_id").asLong()).isEqualTo(publishedPolicyId);
        assertThat(detail.body().at("/data/body_text").asText()).contains("公开政策正文");
        assertThat(detail.rawBody())
                .doesNotContain("<script>")
                .doesNotContain("13900000000")
                .doesNotContain("secret@example.com")
                .doesNotContain("resume_body")
                .doesNotContain("attachment_object_key")
                .doesNotContain("evidence")
                .doesNotContain("password_hash")
                .doesNotContain("审核材料")
                .doesNotContain("后台备注");

        HttpJsonResponse hrTools = getJson(
                "/api/portal/contents?content_type=hr_tool&page=1&size=10",
                "trace-p21-hr-tool-list");
        assertSuccess(hrTools, 200, "trace-p21-hr-tool-list");
        assertThat(hrTools.rawBody()).contains("Prompt21 HR 工具 " + suffix);
        assertThat(hrTools.rawBody()).doesNotContain("Prompt21 就业政策 " + suffix);
    }

    @Test
    void portalJobFairsShouldHideOfflineEventsAndRegistrationDetails() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        long publicEventId = insertEvent("onsite", "Prompt21 现场招聘会 " + suffix, 1);
        insertEvent("online", "Prompt21 下线网络招聘会 " + suffix, 0);
        long candidateId = insertCandidate(suffix);
        jdbcTemplate.update(
                "INSERT INTO activity_registration (event_id, candidate_id, sign_status) VALUES (?, ?, 1)",
                publicEventId,
                candidateId);

        HttpJsonResponse list = getJson(
                "/api/portal/job-fairs?type_code=onsite&page=1&size=10",
                "trace-p21-event-list");
        assertSuccess(list, 200, "trace-p21-event-list");
        assertThat(list.rawBody()).contains("Prompt21 现场招聘会 " + suffix);
        assertThat(list.rawBody()).doesNotContain("Prompt21 下线网络招聘会 " + suffix);

        HttpJsonResponse detail = getJson(
                "/api/portal/job-fairs/" + publicEventId,
                "trace-p21-event-detail");
        assertSuccess(detail, 200, "trace-p21-event-detail");
        assertThat(detail.body().at("/data/event_id").asLong()).isEqualTo(publicEventId);
        assertThat(detail.body().at("/data/status").asInt()).isEqualTo(1);
        assertThat(detail.rawBody())
                .doesNotContain("activity_registration")
                .doesNotContain("candidate_id")
                .doesNotContain("sign_status")
                .doesNotContain("candidate-p21-" + suffix)
                .doesNotContain("mobile")
                .doesNotContain("email")
                .doesNotContain("报名名单")
                .doesNotContain("签到证据");

        HttpJsonResponse hiddenDetail = getJson(
                "/api/portal/job-fairs/" + insertEvent("onsite", "Prompt21 下线详情 " + suffix, 0),
                "trace-p21-event-hidden");
        assertThat(hiddenDetail.status()).isEqualTo(404);
        assertThat(hiddenDetail.headerTraceId()).isEqualTo("trace-p21-event-hidden");
    }

    private long insertContent(String contentType, String title, String summary, String bodyHtml, int status) {
        jdbcTemplate.update(
                "INSERT INTO cms_content "
                        + "(content_type, title, cover_url, summary, body_html, city_code, status, publish_time) "
                        + "VALUES (?, ?, '/demo/cover.png', ?, ?, '310000', ?, CASE WHEN ? = 1 THEN CURRENT_TIMESTAMP ELSE NULL END)",
                contentType,
                title,
                summary,
                bodyHtml,
                status,
                status);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM cms_content WHERE title = ?",
                Long.class,
                title);
        return id == null ? 0 : id;
    }

    private long insertEvent(String typeCode, String title, int status) {
        jdbcTemplate.update(
                "INSERT INTO activity_event "
                        + "(title, type_code, city_code, start_time, end_time, location, status) "
                        + "VALUES (?, ?, '310000', ?, ?, 'LocalTalent 会场', ?)",
                title,
                typeCode,
                LocalDateTime.now().plusDays(7),
                LocalDateTime.now().plusDays(7).plusHours(4),
                status);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM activity_event WHERE title = ?",
                Long.class,
                title);
        return id == null ? 0 : id;
    }

    private long insertCandidate(String suffix) {
        jdbcTemplate.update(
                "INSERT INTO candidate_user (email, password_hash, real_name) VALUES (?, '$2a$10$demo', '报名候选人')",
                "candidate-p21-" + suffix + "@example.com");
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM candidate_user WHERE email = ?",
                Long.class,
                "candidate-p21-" + suffix + "@example.com");
        return id == null ? 0 : id;
    }

    private HttpJsonResponse getJson(String path, String traceId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .header("X-Trace-Id", traceId)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return new HttpJsonResponse(
                response.statusCode(),
                response.body(),
                objectMapper.readTree(response.body()),
                response.headers().firstValue("X-Trace-Id").orElse(null));
    }

    private void assertSuccess(HttpJsonResponse response, int status, String traceId) {
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.headerTraceId()).isEqualTo(traceId);
        assertThat(response.body().at("/trace_id").asText()).isEqualTo(traceId);
        assertThat(response.body().at("/code").asText()).isEqualTo("0");
    }

    private record HttpJsonResponse(int status, String rawBody, JsonNode body, String headerTraceId) {
    }
}
