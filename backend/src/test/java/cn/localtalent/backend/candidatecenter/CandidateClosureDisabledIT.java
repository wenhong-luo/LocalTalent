package cn.localtalent.backend.candidatecenter;

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
                "localtalent.auth.jwt.secret=candidate-closure-disabled-secret-change-me",
                "localtalent.auth.jwt.ttl-seconds=3600"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CandidateClosureDisabledIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_candidate_closure_disabled_test")
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
    void candidateClosureShouldBeDisabledByDefaultWhileOverviewStaysReadable() throws Exception {
        String token = registerAndLoginCandidate();

        HttpJsonResponse overview = getJson(
                "/api/candidate/center/overview",
                "trace-p3-disabled-overview",
                "Bearer " + token);
        assertThat(overview.status()).isEqualTo(200);
        assertThat(overview.body().at("/code").asText()).isEqualTo("0");
        assertThat(overview.body().at("/data/features/candidate_closure_enabled").asBoolean()).isFalse();

        HttpJsonResponse resume = getJson(
                "/api/candidate/center/resume",
                "trace-p3-disabled-resume",
                "Bearer " + token);
        assertThat(resume.status()).isEqualTo(403);
        assertThat(resume.body().at("/code").asText()).isEqualTo("FEATURE_DISABLED_403");
        assertThat(resume.body().at("/trace_id").asText()).isEqualTo("trace-p3-disabled-resume");
    }

    private String registerAndLoginCandidate() throws Exception {
        String email = "p3-disabled-" + UUID.randomUUID().toString().replace("-", "") + "@example.com";
        String password = "Candidate@123456";
        postJson(
                "/api/auth/register",
                """
                        {
                          "identity_type": "candidate",
                          "email": "%s",
                          "password": "%s",
                          "display_name": "三期关闭态候选人"
                        }
                        """.formatted(email, password),
                "trace-p3-disabled-register",
                null,
                null);
        HttpJsonResponse login = postJson(
                "/api/auth/login",
                """
                        {
                          "identity_type": "candidate",
                          "account": "%s",
                          "password": "%s"
                        }
                        """.formatted(email, password),
                "trace-p3-disabled-login",
                null,
                null);
        assertThat(login.status()).isEqualTo(200);
        return login.body().at("/data/access_token").asText();
    }

    private HttpJsonResponse postJson(
            String path,
            String body,
            String traceId,
            String authorization,
            String idempotencyKey
    ) throws Exception {
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

    private record HttpJsonResponse(int status, String traceId, JsonNode body) {
    }
}
