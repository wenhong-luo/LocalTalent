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
                "localtalent.auth.jwt.secret=p29-disabled-secret-change-me-with-enough-length",
                "localtalent.auth.jwt.ttl-seconds=3600"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminPortalOpsDisabledIT extends AdminIntegrationTestSupport {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_p29_disabled_test")
            .withUsername("localtalent")
            .withPassword("localtalent");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    void operatorPortalOpsShouldBeDisabledByDefaultWhileLegacyAdminApisRemainAvailable() throws Exception {
        String token = login("operator", "operator", "LocalTalent@123456", "trace-p29-disabled-login");

        assertError(getJson(
                "/api/admin/ops/overview",
                "trace-p29-disabled-overview",
                "Bearer " + token), 403, "FEATURE_DISABLED_403", "trace-p29-disabled-overview");

        assertSuccess(getJson(
                "/api/admin/companies/review?page=1&size=10",
                "trace-p29-disabled-legacy",
                "Bearer " + token), 200, "trace-p29-disabled-legacy");

        HttpJsonResponse publicRecommendations = getJson(
                "/api/portal/recommendations?slot_code=home_hot_jobs",
                "trace-p29-disabled-public",
                null);
        assertSuccess(publicRecommendations, 200, "trace-p29-disabled-public");
        assertThat(publicRecommendations.body().at("/data/recommendation_list")).isEmpty();
    }
}
