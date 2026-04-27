package cn.localtalent.backend.authz;

import static org.assertj.core.api.Assertions.assertThat;

import cn.localtalent.backend.common.api.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "localtalent.auth.jwt.secret=authz-idor-secret-change-me-with-enough-length",
                "localtalent.auth.jwt.ttl-seconds=3600"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IdorBlockIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_authz_idor_test")
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
    void candidateShouldBeBlockedWhenReadingAnotherCandidateObject() throws Exception {
        grantApiPermission("candidate", "test.candidate.read");
        Account candidateA = registerCandidate();
        Account candidateB = registerCandidate();

        HttpJsonResponse response = getJson(
                "/__test/authz/candidates/" + candidateB.userId(),
                "trace-idor-candidate",
                "Bearer " + candidateA.token());

        assertError(response, 403, "AUTHZ_403", "trace-idor-candidate");
    }

    @Test
    void companyShouldBeBlockedWhenReadingAnotherCompanyObject() throws Exception {
        grantApiPermission("company_admin", "test.company.read");
        Account companyA = registerCompany();
        Account companyB = registerCompany();

        HttpJsonResponse response = getJson(
                "/__test/authz/companies/" + companyB.companyId(),
                "trace-idor-company",
                "Bearer " + companyA.token());

        assertError(response, 403, "AUTHZ_403", "trace-idor-company");
    }

    @Test
    void authenticatedUserWithoutMenuPermissionShouldBeBlocked() throws Exception {
        String operatorToken = login("operator", "operator", "LocalTalent@123456", "trace-operator-forbidden-login");

        HttpJsonResponse response = getJson(
                "/__test/authz/missing",
                "trace-missing-permission",
                "Bearer " + operatorToken);

        assertError(response, 403, "AUTHZ_403", "trace-missing-permission");
    }

    private Account registerCandidate() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "idor-candidate-" + suffix + "@example.com";
        String password = "Candidate@123456";
        HttpJsonResponse registerResponse = postJson("/api/auth/register", """
                {
                  "identity_type": "candidate",
                  "email": "%s",
                  "password": "%s",
                  "display_name": "测试求职者"
                }
                """.formatted(email, password), "trace-register-candidate-" + suffix.substring(0, 8));
        assertThat(registerResponse.status()).isEqualTo(200);
        long userId = registerResponse.body().at("/data/identity/user_id").asLong();
        String token = login("candidate", email, password, "trace-login-candidate-" + suffix.substring(0, 8));
        return new Account(userId, null, token);
    }

    private Account registerCompany() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "idor-company-" + suffix + "@example.com";
        String password = "Company@123456";
        HttpJsonResponse registerResponse = postJson("/api/auth/register", """
                {
                  "identity_type": "company",
                  "company_name": "越权测试企业%s",
                  "license_no": "IDOR-%s",
                  "user_name": "企业管理员",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(suffix.substring(0, 6), suffix.substring(0, 16), email, password), "trace-register-company-" + suffix.substring(0, 8));
        assertThat(registerResponse.status()).isEqualTo(200);
        long userId = registerResponse.body().at("/data/identity/user_id").asLong();
        long companyId = registerResponse.body().at("/data/identity/company_id").asLong();
        String token = login("company", email, password, "trace-login-company-" + suffix.substring(0, 8));
        return new Account(userId, companyId, token);
    }

    private String login(String identityType, String account, String password, String traceId) throws Exception {
        HttpJsonResponse response = postJson("/api/auth/login", """
                {
                  "identity_type": "%s",
                  "account": "%s",
                  "password": "%s"
                }
                """.formatted(identityType, account, password), traceId);
        assertThat(response.status()).isEqualTo(200);
        return response.body().at("/data/access_token").asText();
    }

    private void grantApiPermission(String roleCode, String apiCode) {
        String menuCode = "menu_" + apiCode.replace('.', '_');
        jdbcTemplate.update(
                "INSERT INTO sys_menu (menu_code, menu_name, menu_type, api_code, sort_no, status) "
                        + "VALUES (?, ?, 'api', ?, 900, 1) "
                        + "ON DUPLICATE KEY UPDATE api_code = VALUES(api_code), status = 1",
                menuCode,
                apiCode,
                apiCode);
        jdbcTemplate.update(
                "INSERT INTO sys_role_menu (role_id, menu_id) "
                        + "SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.menu_code = ? "
                        + "WHERE r.role_code = ? "
                        + "ON DUPLICATE KEY UPDATE sys_role_menu.id = sys_role_menu.id",
                menuCode,
                roleCode);
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
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("X-Trace-Id", traceId)
                .header("Authorization", authorization)
                .GET()
                .build();
        return send(request);
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

    private void assertError(HttpJsonResponse response, int expectedStatus, String code, String traceId) {
        assertThat(response.status()).isEqualTo(expectedStatus);
        assertThat(response.traceId()).isEqualTo(traceId);
        assertThat(response.body().at("/code").asText()).isEqualTo(code);
        assertThat(response.body().at("/trace_id").asText()).isEqualTo(traceId);
    }

    private record Account(long userId, Long companyId, String token) {
    }

    private record HttpJsonResponse(int status, String traceId, JsonNode body) {
    }

    @TestConfiguration
    static class FixtureConfig {

        @Bean
        TestProtectedController testProtectedController(TestProtectedService service) {
            return new TestProtectedController(service);
        }

        @Bean
        TestProtectedService testProtectedService(DataScopeService dataScopeService) {
            return new TestProtectedService(dataScopeService);
        }
    }

    @RestController
    @RequestMapping("/__test/authz")
    static class TestProtectedController {

        private final TestProtectedService service;

        TestProtectedController(TestProtectedService service) {
            this.service = service;
        }

        @GetMapping("/candidates/{candidateId}")
        @RequirePermission("test.candidate.read")
        ApiResponse<Map<String, Object>> candidate(@PathVariable long candidateId) {
            return ApiResponse.success(service.readCandidate(candidateId));
        }

        @GetMapping("/companies/{companyId}")
        @RequirePermission("test.company.read")
        ApiResponse<Map<String, Object>> company(@PathVariable long companyId) {
            return ApiResponse.success(service.readCompany(companyId));
        }

        @GetMapping("/missing")
        @RequirePermission("test.missing.permission")
        ApiResponse<Map<String, Object>> missingPermission() {
            return ApiResponse.success(Map.of("status", "should-not-return"));
        }
    }

    static class TestProtectedService {

        private final DataScopeService dataScopeService;

        TestProtectedService(DataScopeService dataScopeService) {
            this.dataScopeService = dataScopeService;
        }

        Map<String, Object> readCandidate(long candidateId) {
            dataScopeService.assertAccessible(
                    AuthzContext.requireCurrentPrincipal(),
                    "candidate_profile",
                    ResourceOwner.candidate(candidateId));
            return Map.of("candidate_id", candidateId);
        }

        Map<String, Object> readCompany(long companyId) {
            dataScopeService.assertAccessible(
                    AuthzContext.requireCurrentPrincipal(),
                    "company_profile",
                    ResourceOwner.company(companyId));
            return Map.of("company_id", companyId);
        }
    }
}
