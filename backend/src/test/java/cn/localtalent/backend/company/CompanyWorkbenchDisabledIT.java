package cn.localtalent.backend.company;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
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
                "localtalent.auth.jwt.secret=company-workbench-disabled-secret-change-me",
                "localtalent.auth.jwt.ttl-seconds=3600"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CompanyWorkbenchDisabledIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_company_workbench_disabled_test")
            .withUsername("localtalent")
            .withPassword("localtalent");

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    void companyWorkbenchShouldBeDisabledByDefaultWhileLegacyCompanyApisRemainAvailable() throws Exception {
        String token = registerAndLoginCompany();

        HttpJsonResponse disabled = getJson(
                "/api/company/workbench/overview",
                "trace-p28-disabled-overview",
                "Bearer " + token);
        assertThat(disabled.status()).isEqualTo(403);
        assertThat(disabled.body().at("/code").asText()).isEqualTo("FEATURE_DISABLED_403");
        assertThat(disabled.body().at("/trace_id").asText()).isEqualTo("trace-p28-disabled-overview");

        HttpJsonResponse resumeSearchDisabled = getJson(
                "/api/company/workbench/resume-search?page=1&size=20",
                "trace-p28-disabled-resume-search",
                "Bearer " + token);
        assertThat(resumeSearchDisabled.status()).isEqualTo(403);
        assertThat(resumeSearchDisabled.body().at("/code").asText()).isEqualTo("FEATURE_DISABLED_403");
        assertThat(resumeSearchDisabled.body().at("/trace_id").asText()).isEqualTo("trace-p28-disabled-resume-search");

        HttpJsonResponse legacy = getJson(
                "/api/company/jobs?page=1&size=20",
                "trace-p28-disabled-legacy-jobs",
                "Bearer " + token);
        assertThat(legacy.status()).isEqualTo(200);
        assertThat(legacy.body().at("/code").asText()).isEqualTo("0");
    }

    private String registerAndLoginCompany() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "p28-disabled-company-" + suffix + "@example.com";
        String password = "Company@123456";
        postJson(
                "/api/auth/register",
                """
                        {
                          "identity_type": "company",
                          "company_name": "P28 Disabled 企业",
                          "license_no": "P28-D-%s",
                          "user_name": "企业管理员",
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(suffix.substring(0, 16), email, password),
                "trace-p28-disabled-register",
                null,
                null);
        HttpJsonResponse login = postJson(
                "/api/auth/login",
                """
                        {
                          "identity_type": "company",
                          "account": "%s",
                          "password": "%s"
                        }
                        """.formatted(email, password),
                "trace-p28-disabled-login",
                null,
                null);
        assertThat(login.status()).isEqualTo(200);
        return login.body().at("/data/access_token").asText();
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

    private HttpJsonResponse getJson(String path, String traceId, String authorization) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path)).header("X-Trace-Id", traceId).GET();
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        return send(builder.build());
    }

    private HttpJsonResponse send(HttpRequest request) throws Exception {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return new HttpJsonResponse(response.statusCode(), objectMapper.readTree(response.body()));
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private record HttpJsonResponse(int status, JsonNode body) {
    }
}
