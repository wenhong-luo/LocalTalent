package cn.localtalent.backend.admin;

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
                "localtalent.auth.jwt.secret=prompt7-auditor-secret-change-me-with-enough-length",
                "localtalent.auth.jwt.ttl-seconds=3600"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuditorWriteDeniedIT extends AdminIntegrationTestSupport {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_admin_auditor_test")
            .withUsername("localtalent")
            .withPassword("localtalent");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    void auditorShouldReadButNotWriteAdminResources() throws Exception {
        String operatorToken = login("operator", "operator", "LocalTalent@123456", "trace-p7-auditor-operator-login");
        String auditorToken = login(
                "operator",
                insertAuditor(),
                "LocalTalent@123456",
                "trace-p7-auditor-login");
        Account company = registerAndLoginCompany("auditor");
        long jobId = createJob(company.token(), "Prompt7 auditor job", "trace-p7-auditor-job-create");
        long contentId = createContent(operatorToken);
        long eventId = createEvent(operatorToken);

        assertSuccess(getJson(
                "/api/admin/companies/review?page=1&size=10",
                "trace-p7-auditor-company-list",
                "Bearer " + auditorToken), 200, "trace-p7-auditor-company-list");
        assertSuccess(getJson(
                "/api/admin/companies/review/" + company.companyId(),
                "trace-p7-auditor-company-detail",
                "Bearer " + auditorToken), 200, "trace-p7-auditor-company-detail");
        assertSuccess(getJson(
                "/api/admin/jobs/review?page=1&size=10",
                "trace-p7-auditor-job-list",
                "Bearer " + auditorToken), 200, "trace-p7-auditor-job-list");
        assertSuccess(getJson(
                "/api/admin/jobs/review/" + jobId,
                "trace-p7-auditor-job-detail",
                "Bearer " + auditorToken), 200, "trace-p7-auditor-job-detail");
        assertSuccess(getJson(
                "/api/admin/cms/contents?page=1&size=10",
                "trace-p7-auditor-cms-list",
                "Bearer " + auditorToken), 200, "trace-p7-auditor-cms-list");
        assertSuccess(getJson(
                "/api/admin/cms/contents/" + contentId,
                "trace-p7-auditor-cms-detail",
                "Bearer " + auditorToken), 200, "trace-p7-auditor-cms-detail");
        assertSuccess(getJson(
                "/api/admin/events?page=1&size=10",
                "trace-p7-auditor-event-list",
                "Bearer " + auditorToken), 200, "trace-p7-auditor-event-list");
        assertSuccess(getJson(
                "/api/admin/events/" + eventId,
                "trace-p7-auditor-event-detail",
                "Bearer " + auditorToken), 200, "trace-p7-auditor-event-detail");

        assertError(postJson(
                "/api/admin/companies/review",
                """
                        {
                          "company_id": %d,
                          "audit_status": 2,
                          "memo": "auditor cannot approve"
                        }
                        """.formatted(company.companyId()),
                "trace-p7-auditor-company-write-denied",
                "Bearer " + auditorToken), 403, "AUTHZ_403", "trace-p7-auditor-company-write-denied");
        assertError(postJson(
                "/api/admin/jobs/review",
                """
                        {
                          "job_id": %d,
                          "audit_status": 2,
                          "memo": "auditor cannot approve"
                        }
                        """.formatted(jobId),
                "trace-p7-auditor-job-write-denied",
                "Bearer " + auditorToken), 403, "AUTHZ_403", "trace-p7-auditor-job-write-denied");
        assertError(postJson(
                "/api/admin/cms/contents",
                cmsBody("auditor create denied"),
                "trace-p7-auditor-cms-create-denied",
                "Bearer " + auditorToken), 403, "AUTHZ_403", "trace-p7-auditor-cms-create-denied");
        assertError(putJson(
                "/api/admin/cms/contents/" + contentId,
                cmsBody("auditor update denied"),
                "trace-p7-auditor-cms-update-denied",
                "Bearer " + auditorToken), 403, "AUTHZ_403", "trace-p7-auditor-cms-update-denied");
        assertError(deleteJson(
                "/api/admin/cms/contents/" + contentId,
                "trace-p7-auditor-cms-delete-denied",
                "Bearer " + auditorToken), 403, "AUTHZ_403", "trace-p7-auditor-cms-delete-denied");
        assertError(postJson(
                "/api/admin/events",
                eventBody("auditor create denied"),
                "trace-p7-auditor-event-create-denied",
                "Bearer " + auditorToken), 403, "AUTHZ_403", "trace-p7-auditor-event-create-denied");
        assertError(putJson(
                "/api/admin/events/" + eventId,
                eventBody("auditor update denied"),
                "trace-p7-auditor-event-update-denied",
                "Bearer " + auditorToken), 403, "AUTHZ_403", "trace-p7-auditor-event-update-denied");
        assertError(deleteJson(
                "/api/admin/events/" + eventId,
                "trace-p7-auditor-event-delete-denied",
                "Bearer " + auditorToken), 403, "AUTHZ_403", "trace-p7-auditor-event-delete-denied");
    }

    private long createContent(String operatorToken) throws Exception {
        HttpJsonResponse response = postJson(
                "/api/admin/cms/contents",
                cmsBody("auditor read fixture"),
                "trace-p7-auditor-cms-fixture-create",
                "Bearer " + operatorToken);
        assertSuccess(response, 200, "trace-p7-auditor-cms-fixture-create");
        return response.body().at("/data/content_id").asLong();
    }

    private long createEvent(String operatorToken) throws Exception {
        HttpJsonResponse response = postJson(
                "/api/admin/events",
                eventBody("auditor read fixture"),
                "trace-p7-auditor-event-fixture-create",
                "Bearer " + operatorToken);
        assertSuccess(response, 200, "trace-p7-auditor-event-fixture-create");
        return response.body().at("/data/event_id").asLong();
    }

    private String cmsBody(String title) {
        return """
                {
                  "content_type": "notice",
                  "title": "%s",
                  "summary": "auditor fixture",
                  "body_html": "<p>auditor fixture</p>",
                  "city_code": "310000",
                  "status": 1
                }
                """.formatted(title);
    }

    private String eventBody(String title) {
        return """
                {
                  "title": "%s",
                  "type_code": "job_fair",
                  "city_code": "310000",
                  "start_time": "2026-06-01T09:00:00",
                  "end_time": "2026-06-01T17:00:00",
                  "location": "Auditor Hall",
                  "status": 1
                }
                """.formatted(title);
    }
}
