package cn.localtalent.backend.admin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
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
                "localtalent.auth.jwt.secret=prompt7-cms-event-secret-change-me-with-enough-length",
                "localtalent.auth.jwt.ttl-seconds=3600"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminCmsEventCrudIT extends AdminIntegrationTestSupport {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_admin_cms_event_test")
            .withUsername("localtalent")
            .withPassword("localtalent");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    void operatorShouldCrudCmsAndEventWithSoftDeleteAndAudit() throws Exception {
        String operatorToken = login("operator", "operator", "LocalTalent@123456", "trace-p7-crud-operator-login");

        HttpJsonResponse createContent = postJson(
                "/api/admin/cms/contents",
                """
                        {
                          "content_type": "notice",
                          "title": "Prompt7 公告",
                          "cover_url": "minio://localtalent/cover.png",
                          "summary": "Prompt7 内容摘要",
                          "body_html": "<p>Prompt7 内容正文</p>",
                          "city_code": "310000",
                          "status": 1
                        }
                        """,
                "trace-p7-cms-create",
                "Bearer " + operatorToken);
        assertSuccess(createContent, 200, "trace-p7-cms-create");
        long contentId = createContent.body().at("/data/content_id").asLong();

        HttpJsonResponse listContent = getJson(
                "/api/admin/cms/contents?content_type=notice&status=1&page=1&size=10",
                "trace-p7-cms-list",
                "Bearer " + operatorToken);
        assertSuccess(listContent, 200, "trace-p7-cms-list");
        assertThat(listContent.body().at("/data/total").asLong()).isEqualTo(1);

        HttpJsonResponse contentDetail = getJson(
                "/api/admin/cms/contents/" + contentId,
                "trace-p7-cms-detail",
                "Bearer " + operatorToken);
        assertSuccess(contentDetail, 200, "trace-p7-cms-detail");
        assertThat(contentDetail.body().at("/data/title").asText()).isEqualTo("Prompt7 公告");

        HttpJsonResponse updateContent = putJson(
                "/api/admin/cms/contents/" + contentId,
                """
                        {
                          "content_type": "notice",
                          "title": "Prompt7 公告更新",
                          "summary": "Prompt7 内容摘要更新",
                          "body_html": "<p>Prompt7 内容正文更新</p>",
                          "city_code": "310000",
                          "status": 1
                        }
                        """,
                "trace-p7-cms-update",
                "Bearer " + operatorToken);
        assertSuccess(updateContent, 200, "trace-p7-cms-update");
        assertThat(updateContent.body().at("/data/title").asText()).isEqualTo("Prompt7 公告更新");

        HttpJsonResponse deleteContent = deleteJson(
                "/api/admin/cms/contents/" + contentId,
                "trace-p7-cms-soft-delete",
                "Bearer " + operatorToken);
        assertSuccess(deleteContent, 200, "trace-p7-cms-soft-delete");
        assertThat(deleteContent.body().at("/data/status").asInt()).isZero();

        HttpJsonResponse deletedContentDetail = getJson(
                "/api/admin/cms/contents/" + contentId,
                "trace-p7-cms-deleted-detail",
                "Bearer " + operatorToken);
        assertSuccess(deletedContentDetail, 200, "trace-p7-cms-deleted-detail");
        assertThat(deletedContentDetail.body().at("/data/status").asInt()).isZero();

        HttpJsonResponse createEvent = postJson(
                "/api/admin/events",
                """
                        {
                          "title": "Prompt7 招聘活动",
                          "type_code": "job_fair",
                          "city_code": "310000",
                          "start_time": "2026-05-01T09:00:00",
                          "end_time": "2026-05-01T17:00:00",
                          "location": "Shanghai Hall",
                          "status": 1
                        }
                        """,
                "trace-p7-event-create",
                "Bearer " + operatorToken);
        assertSuccess(createEvent, 200, "trace-p7-event-create");
        long eventId = createEvent.body().at("/data/event_id").asLong();

        HttpJsonResponse listEvent = getJson(
                "/api/admin/events?type_code=job_fair&city_code=310000&status=1&page=1&size=10",
                "trace-p7-event-list",
                "Bearer " + operatorToken);
        assertSuccess(listEvent, 200, "trace-p7-event-list");
        assertThat(listEvent.body().at("/data/total").asLong()).isEqualTo(1);

        HttpJsonResponse eventDetail = getJson(
                "/api/admin/events/" + eventId,
                "trace-p7-event-detail",
                "Bearer " + operatorToken);
        assertSuccess(eventDetail, 200, "trace-p7-event-detail");
        assertThat(eventDetail.body().at("/data/title").asText()).isEqualTo("Prompt7 招聘活动");

        HttpJsonResponse updateEvent = putJson(
                "/api/admin/events/" + eventId,
                """
                        {
                          "title": "Prompt7 招聘活动更新",
                          "type_code": "job_fair",
                          "city_code": "310000",
                          "start_time": "2026-05-02T09:00:00",
                          "end_time": "2026-05-02T17:00:00",
                          "location": "Shanghai Hall B",
                          "status": 1
                        }
                        """,
                "trace-p7-event-update",
                "Bearer " + operatorToken);
        assertSuccess(updateEvent, 200, "trace-p7-event-update");
        assertThat(updateEvent.body().at("/data/location").asText()).isEqualTo("Shanghai Hall B");

        HttpJsonResponse deleteEvent = deleteJson(
                "/api/admin/events/" + eventId,
                "trace-p7-event-soft-delete",
                "Bearer " + operatorToken);
        assertSuccess(deleteEvent, 200, "trace-p7-event-soft-delete");
        assertThat(deleteEvent.body().at("/data/status").asInt()).isZero();

        HttpJsonResponse deletedEventDetail = getJson(
                "/api/admin/events/" + eventId,
                "trace-p7-event-deleted-detail",
                "Bearer " + operatorToken);
        assertSuccess(deletedEventDetail, 200, "trace-p7-event-deleted-detail");
        assertThat(deletedEventDetail.body().at("/data/status").asInt()).isZero();

        assertThat(countAudit("trace-p7-cms-create", "cms_content", "cms_create")).isEqualTo(1);
        assertThat(countAudit("trace-p7-cms-update", "cms_content", "cms_update")).isEqualTo(1);
        assertThat(countAudit("trace-p7-cms-soft-delete", "cms_content", "cms_soft_delete")).isEqualTo(1);
        assertThat(countAudit("trace-p7-event-create", "activity_event", "event_create")).isEqualTo(1);
        assertThat(countAudit("trace-p7-event-update", "activity_event", "event_update")).isEqualTo(1);
        assertThat(countAudit("trace-p7-event-soft-delete", "activity_event", "event_soft_delete")).isEqualTo(1);
    }
}
