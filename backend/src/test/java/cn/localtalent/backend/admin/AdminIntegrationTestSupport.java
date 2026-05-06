package cn.localtalent.backend.admin;

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

abstract class AdminIntegrationTestSupport {

    protected final HttpClient httpClient = HttpClient.newHttpClient();
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @LocalServerPort
    protected int port;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected Account registerAndLoginCompany(String label) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "p7-company-" + label + "-" + suffix + "@example.com";
        String licenseNo = "P7-" + label + "-" + suffix.substring(0, 14);
        String password = "Company@123456";
        HttpJsonResponse registerResponse = postJson(
                "/api/auth/register",
                """
                        {
                          "identity_type": "company",
                          "company_name": "Prompt7 企业%s",
                          "license_no": "%s",
                          "user_name": "企业管理员",
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(label, licenseNo, email, password),
                "trace-p7-company-register-" + label,
                null);
        assertSuccess(registerResponse, 200, "trace-p7-company-register-" + label);
        long companyId = registerResponse.body().at("/data/identity/company_id").asLong();
        String token = login("company", email, password, "trace-p7-company-login-" + label);
        return new Account(companyId, licenseNo, token);
    }

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

    protected void applyCompany(Account company, String traceId) throws Exception {
        HttpJsonResponse response = postJson(
                "/api/company/apply",
                """
                        {
                          "company_name": "Prompt7 企业重提",
                          "license_no": "%s",
                          "industry_code": "internet",
                          "scale_code": "50-100",
                          "city_code": "310000",
                          "address": "Shanghai",
                          "company_profile": "prompt7 review fixture"
                        }
                        """.formatted(company.licenseNo()),
                traceId,
                "Bearer " + company.token());
        assertSuccess(response, 200, traceId);
    }

    protected long createJob(String companyToken, String title, String traceId) throws Exception {
        HttpJsonResponse response = postJson(
                "/api/company/jobs",
                """
                        {
                          "title": "%s",
                          "category_code": "software",
                          "city_code": "310000",
                          "salary_min": 12000,
                          "salary_max": 18000,
                          "job_desc": "Prompt7 admin review fixture"
                        }
                        """.formatted(title),
                traceId,
                "Bearer " + companyToken);
        assertSuccess(response, 200, traceId);
        return response.body().at("/data/job_id").asLong();
    }

    protected void submitJobReview(String companyToken, long jobId, String traceId) throws Exception {
        HttpJsonResponse response = postJson(
                "/api/company/jobs/" + jobId + "/status",
                """
                        {
                          "action": "submit_review",
                          "reason": "resubmit"
                        }
                        """,
                traceId,
                "Bearer " + companyToken);
        assertSuccess(response, 200, traceId);
    }

    protected String insertAuditor() {
        String username = "p7-auditor-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbcTemplate.update(
                "INSERT INTO admin_user (username, display_name, password_hash, role_code, status) "
                        + "VALUES (?, 'Prompt7 审计员', '$2b$12$5G4kZ1x4dOar/VtPyL38UuxDYUreMg03NyJG11tnsQvlMZTSZhP2.', 'auditor', 1)",
                username);
        return username;
    }

    protected int queryInt(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    protected String queryString(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, String.class, args);
    }

    protected long countAudit(String traceId, String bizType, String actionType) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE trace_id = ? AND biz_type = ? AND action_type = ?",
                Long.class,
                traceId,
                bizType,
                actionType);
        return count == null ? 0 : count;
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

    protected HttpJsonResponse postJson(
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

    protected HttpJsonResponse putJson(
            String path,
            String body,
            String traceId,
            String authorization,
            String idempotencyKey
    ) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .header("X-Trace-Id", traceId)
                .PUT(HttpRequest.BodyPublishers.ofString(body));
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        if (idempotencyKey != null) {
            builder.header("X-Idempotency-Key", idempotencyKey);
        }
        return send(builder.build());
    }

    protected HttpJsonResponse putJson(String path, String body, String traceId, String authorization) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .header("X-Trace-Id", traceId)
                .PUT(HttpRequest.BodyPublishers.ofString(body));
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

    protected HttpJsonResponse deleteJson(String path, String traceId, String authorization) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("X-Trace-Id", traceId)
                .DELETE();
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

    protected record Account(long companyId, String licenseNo, String token) {
    }

    protected record HttpJsonResponse(int status, String headerTraceId, JsonNode body) {
    }
}
