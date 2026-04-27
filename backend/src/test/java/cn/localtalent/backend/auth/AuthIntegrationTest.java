package cn.localtalent.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
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
                "localtalent.auth.jwt.secret=auth-integration-secret-change-me-with-enough-length",
                "localtalent.auth.jwt.ttl-seconds=3600"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ExtendWith(OutputCaptureExtension.class)
class AuthIntegrationTest {

    private static final String JWT_SECRET = "auth-integration-secret-change-me-with-enough-length";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_auth_test")
            .withUsername("localtalent")
            .withPassword("localtalent");

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    void candidateShouldRegisterLoginAndReadMe(CapturedOutput output) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String mobile = "139" + suffix.substring(0, 8);
        String email = "candidate-" + suffix + "@example.com";
        String password = "Candidate@123456";

        HttpJsonResponse registerResponse = postJson("/api/auth/register", """
                {
                  "identity_type": "candidate",
                  "mobile": "%s",
                  "email": "%s",
                  "password": "%s",
                  "display_name": "求职者一"
                }
                """.formatted(mobile, email, password), "trace-candidate-register");
        assertSuccess(registerResponse, 200, "trace-candidate-register");
        assertThat(registerResponse.body().at("/data/identity/identity_type").asText()).isEqualTo("candidate");

        String token = login("candidate", email, password, "trace-candidate-login");
        HttpJsonResponse meResponse = getJson("/api/auth/me", "trace-candidate-me", "Bearer " + token);
        assertSuccess(meResponse, 200, "trace-candidate-me");
        assertThat(meResponse.body().at("/data/identity_type").asText()).isEqualTo("candidate");
        assertThat(meResponse.body().at("/data/display_name").asText()).isEqualTo("求职者一");

        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM candidate_user WHERE email = ?",
                String.class,
                email);
        assertThat(passwordHash).isNotEqualTo(password).startsWith("$2");
        assertThat(output).doesNotContain(password);
    }

    @Test
    void companyShouldRegisterLoginAndReadMe() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "company-" + suffix + "@example.com";
        String password = "Company@123456";

        HttpJsonResponse registerResponse = postJson("/api/auth/register", """
                {
                  "identity_type": "company",
                  "company_name": "本地测试企业",
                  "license_no": "LIC-%s",
                  "user_name": "企业管理员",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(suffix.substring(0, 16), email, password), "trace-company-register");
        assertSuccess(registerResponse, 200, "trace-company-register");
        assertThat(registerResponse.body().at("/data/identity/identity_type").asText()).isEqualTo("company");
        assertThat(registerResponse.body().at("/data/identity/company_id").isNumber()).isTrue();

        String token = login("company", email, password, "trace-company-login");
        HttpJsonResponse meResponse = getJson("/api/auth/me", "trace-company-me", "Bearer " + token);
        assertSuccess(meResponse, 200, "trace-company-me");
        assertThat(meResponse.body().at("/data/identity_type").asText()).isEqualTo("company");
        assertThat(meResponse.body().at("/data/company_id").isNumber()).isTrue();
        assertThat(meResponse.body().at("/data/display_name").asText()).isEqualTo("本地测试企业/企业管理员");
    }

    @Test
    void seededOperatorShouldLoginAndReadMe(CapturedOutput output) throws Exception {
        String password = "LocalTalent@123456";
        String token = login("operator", "operator", password, "trace-operator-login");

        HttpJsonResponse meResponse = getJson("/api/auth/me", "trace-operator-me", "Bearer " + token);
        assertSuccess(meResponse, 200, "trace-operator-me");
        assertThat(meResponse.body().at("/data/identity_type").asText()).isEqualTo("operator");
        assertThat(meResponse.body().at("/data/display_name").asText()).isEqualTo("本地开发运营账号");
        assertThat(output).doesNotContain(password);
    }

    @Test
    void authErrorsShouldUseUnifiedEnvelopeAndTraceId() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "duplicate-" + suffix + "@example.com";
        String password = "Duplicate@123456";
        String registerBody = """
                {
                  "identity_type": "candidate",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

        assertSuccess(postJson("/api/auth/register", registerBody, "trace-first-register"), 200, "trace-first-register");

        HttpJsonResponse duplicateResponse = postJson("/api/auth/register", registerBody, "trace-duplicate");
        assertError(duplicateResponse, 409, "AUTH_409", "trace-duplicate");

        HttpJsonResponse wrongPasswordResponse = postJson("/api/auth/login", """
                {
                  "identity_type": "candidate",
                  "account": "%s",
                  "password": "Wrong@123456"
                }
                """.formatted(email), "trace-wrong-password");
        assertError(wrongPasswordResponse, 401, "AUTH_401", "trace-wrong-password");

        HttpJsonResponse missingTokenResponse = getJson("/api/auth/me", "trace-missing-token", null);
        assertError(missingTokenResponse, 401, "AUTH_401", "trace-missing-token");

        String validToken = login("operator", "operator", "LocalTalent@123456", "trace-valid-for-tamper");
        HttpJsonResponse tamperedTokenResponse = getJson(
                "/api/auth/me",
                "trace-tampered-token",
                "Bearer " + tamper(validToken));
        assertError(tamperedTokenResponse, 401, "AUTH_401", "trace-tampered-token");

        HttpJsonResponse expiredTokenResponse = getJson(
                "/api/auth/me",
                "trace-expired-token",
                "Bearer " + expiredToken());
        assertError(expiredTokenResponse, 401, "AUTH_401", "trace-expired-token");
    }

    private String login(String identityType, String account, String password, String traceId) throws Exception {
        HttpJsonResponse response = postJson("/api/auth/login", """
                {
                  "identity_type": "%s",
                  "account": "%s",
                  "password": "%s"
                }
                """.formatted(identityType, account, password), traceId);
        assertSuccess(response, 200, traceId);
        assertThat(response.body().at("/data/access_token").isTextual()).isTrue();
        return response.body().at("/data/access_token").asText();
    }

    private HttpJsonResponse postJson(String path, String body, String traceId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .header("X-Trace-Id", traceId)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return send(request);
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

    private void assertSuccess(HttpJsonResponse response, int expectedStatus, String traceId) {
        assertThat(response.status()).isEqualTo(expectedStatus);
        assertThat(response.traceId()).isEqualTo(traceId);
        assertThat(response.body().at("/code").asText()).isEqualTo("0");
        assertThat(response.body().at("/trace_id").asText()).isEqualTo(traceId);
    }

    private void assertError(HttpJsonResponse response, int expectedStatus, String code, String traceId) {
        assertThat(response.status()).isEqualTo(expectedStatus);
        assertThat(response.traceId()).isEqualTo(traceId);
        assertThat(response.body().at("/code").asText()).isEqualTo(code);
        assertThat(response.body().at("/trace_id").asText()).isEqualTo(traceId);
    }

    private String tamper(String token) {
        char replacement = token.charAt(token.length() - 1) == 'a' ? 'b' : 'a';
        return token.substring(0, token.length() - 1) + replacement;
    }

    private String expiredToken() throws Exception {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", "operator:1");
        payload.put("identity_type", "operator");
        payload.put("user_id", 1L);
        payload.put("iat", 1L);
        payload.put("exp", 2L);
        payload.put("jti", UUID.randomUUID().toString());

        String signingInput = encodeJson(header) + "." + encodeJson(payload);
        return signingInput + "." + sign(signingInput);
    }

    private String encodeJson(Map<String, Object> payload) throws Exception {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(payload));
    }

    private String sign(String signingInput) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(JWT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signature = mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }

    private record HttpJsonResponse(int status, String traceId, JsonNode body) {
    }
}
