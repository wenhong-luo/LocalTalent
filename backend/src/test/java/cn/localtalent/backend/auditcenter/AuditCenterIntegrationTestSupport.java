package cn.localtalent.backend.auditcenter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

abstract class AuditCenterIntegrationTestSupport {

    protected final HttpClient httpClient = HttpClient.newHttpClient();
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @LocalServerPort
    protected int port;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected String login(String identityType, String account, String password, String traceId) throws Exception {
        HttpJsonResponse response = postJson(
                "/api/auth/login",
                """
                        {
                          "identity_type": "%s",
                          "account": "%s",
                          "password": "%s"
                        }
                        """.formatted(identityType, account, password),
                traceId,
                null);
        assertSuccess(response, 200, traceId);
        return response.body().at("/data/access_token").asText();
    }

    protected String registerAndLoginCandidate(String label) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "p10-candidate-" + label + "-" + suffix + "@example.com";
        String password = "Candidate@123456";
        HttpJsonResponse registerResponse = postJson(
                "/api/auth/register",
                """
                        {
                          "identity_type": "candidate",
                          "email": "%s",
                          "password": "%s",
                          "display_name": "Prompt10 Candidate"
                        }
                        """.formatted(email, password),
                "trace-p10-candidate-register-" + label,
                null);
        assertSuccess(registerResponse, 200, "trace-p10-candidate-register-" + label);
        return login("candidate", email, password, "trace-p10-candidate-login-" + label);
    }

    protected String registerAndLoginCompany(String label) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "p10-company-" + label + "-" + suffix + "@example.com";
        String licenseNo = "P10-" + label + "-" + suffix.substring(0, 14);
        String password = "Company@123456";
        HttpJsonResponse registerResponse = postJson(
                "/api/auth/register",
                """
                        {
                          "identity_type": "company",
                          "company_name": "Prompt10 企业%s",
                          "license_no": "%s",
                          "user_name": "企业管理员",
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(label, licenseNo, email, password),
                "trace-p10-company-register-" + label,
                null);
        assertSuccess(registerResponse, 200, "trace-p10-company-register-" + label);
        return login("company", email, password, "trace-p10-company-login-" + label);
    }

    protected String insertAuditor() {
        String username = "p10-auditor-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbcTemplate.update(
                "INSERT INTO admin_user (username, display_name, password_hash, role_code, status) "
                        + "VALUES (?, 'Prompt10 审计员', "
                        + "'$2b$12$5G4kZ1x4dOar/VtPyL38UuxDYUreMg03NyJG11tnsQvlMZTSZhP2.', 'auditor', 1)",
                username);
        return username;
    }

    protected HttpJsonResponse postJson(String path, String body, String traceId, String authorization) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .header("X-Trace-Id", traceId)
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        return send(builder.build());
    }

    protected HttpJsonResponse getJson(String path, String traceId, String authorization) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("X-Trace-Id", traceId)
                .GET();
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        return send(builder.build());
    }

    protected void assertSuccess(HttpJsonResponse response, int status, String traceId) {
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.headerTraceId()).isEqualTo(traceId);
        assertThat(response.body().at("/code").asText()).isEqualTo("0");
        assertThat(response.body().at("/trace_id").asText()).isEqualTo(traceId);
    }

    protected void assertError(HttpJsonResponse response, int status, String code, String traceId) {
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.headerTraceId()).isEqualTo(traceId);
        assertThat(response.body().at("/code").asText()).isEqualTo(code);
        assertThat(response.body().at("/trace_id").asText()).isEqualTo(traceId);
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

    protected record HttpJsonResponse(int status, String headerTraceId, JsonNode body) {
    }
}
