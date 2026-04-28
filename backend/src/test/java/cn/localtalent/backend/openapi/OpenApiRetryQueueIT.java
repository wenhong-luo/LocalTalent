package cn.localtalent.backend.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
                "localtalent.auth.jwt.secret=prompt9-open-api-retry-secret-change-me-with-enough-length",
                "localtalent.openapi.timestamp-window-seconds=300"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OpenApiRetryQueueIT extends OpenApiTestSupport {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_openapi_retry_test")
            .withUsername("localtalent")
            .withPassword("localtalent");

    @Autowired
    private OpenApiRetryQueueService retryQueueService;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    void failureInjectionShouldEnqueueAndRetryWithBackoff() throws Exception {
        OpenApiResponse response = postOpen(
                "/api/open/v1/applications/sync",
                """
                        {
                          "source_system": "stub_partner",
                          "application_external_id": "P9-RETRY-001",
                          "stub_mode": "retry"
                        }
                        """,
                "trace-p9-retry-enqueue",
                nonce(),
                idempotencyKey());
        assertThat(response.status()).isEqualTo(202);
        assertThat(response.headerTraceId()).isEqualTo("trace-p9-retry-enqueue");
        assertThat(response.body().at("/code").asText()).isEqualTo("OPEN_RETRY_202");
        long taskId = response.body().at("/data/task_id").asLong();
        assertThat(taskId).isPositive();

        assertThat(queryInt(
                "SELECT COUNT(*) FROM integration_sync_task "
                        + "WHERE id = ? AND trace_id = ? AND api_code = ? AND retry_count = 0 AND sync_status = 0",
                taskId,
                "trace-p9-retry-enqueue",
                "open.applications.sync")).isEqualTo(1);
        assertThat(queryInt(
                "SELECT COUNT(*) FROM open_api_log WHERE trace_id = ? AND api_code = ?",
                "trace-p9-retry-enqueue",
                "open.applications.sync")).isEqualTo(1);

        jdbcTemplate.update(
                "UPDATE integration_sync_task SET next_retry_time = DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 SECOND) "
                        + "WHERE id = ?",
                taskId);
        int retried = retryQueueService.retryDueTasks(LocalDateTime.now());
        assertThat(retried).isEqualTo(1);
        assertThat(queryInt("SELECT retry_count FROM integration_sync_task WHERE id = ?", taskId)).isEqualTo(1);
        assertThat(queryString("SELECT error_msg FROM integration_sync_task WHERE id = ?", taskId))
                .isEqualTo("stub retry scheduled");

        jdbcTemplate.update(
                "UPDATE integration_sync_task "
                        + "SET retry_count = max_retry_count - 1, next_retry_time = DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 SECOND) "
                        + "WHERE id = ?",
                taskId);
        int exhausted = retryQueueService.retryDueTasks(LocalDateTime.now());
        assertThat(exhausted).isEqualTo(1);
        assertThat(queryInt("SELECT sync_status FROM integration_sync_task WHERE id = ?", taskId)).isEqualTo(3);
        assertThat(queryString("SELECT error_msg FROM integration_sync_task WHERE id = ?", taskId))
                .isEqualTo("stub retry max attempts reached");
    }
}
