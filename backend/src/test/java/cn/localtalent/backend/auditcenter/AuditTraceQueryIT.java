package cn.localtalent.backend.auditcenter;

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
                "localtalent.auth.jwt.secret=prompt10-audit-center-secret-change-me-with-enough-length",
                "localtalent.auth.jwt.ttl-seconds=3600"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuditTraceQueryIT extends AuditCenterIntegrationTestSupport {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_audit_trace_test")
            .withUsername("localtalent")
            .withPassword("localtalent");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    void auditCenterShouldQueryTraceAcrossAuditFieldAndOpenLogs() throws Exception {
        String operatorToken = login("operator", "operator", "LocalTalent@123456", "trace-p10-operator-login");
        String auditorToken = login(
                "operator",
                insertAuditor(),
                "LocalTalent@123456",
                "trace-p10-auditor-login");
        String candidateToken = registerAndLoginCandidate("deny");
        String companyToken = registerAndLoginCompany("deny");
        insertTraceFixture("trace-p10-chain");

        HttpJsonResponse traceResponse = getJson(
                "/api/admin/audit-traces/trace-p10-chain",
                "trace-p10-query-chain",
                "Bearer " + auditorToken);
        assertSuccess(traceResponse, 200, "trace-p10-query-chain");
        assertThat(traceResponse.body().at("/data/trace_id").asText()).isEqualTo("trace-p10-chain");
        assertThat(traceResponse.body().at("/data/audit_log_list").size()).isEqualTo(5);
        assertThat(traceResponse.body().at("/data/access_log_list").size()).isEqualTo(2);
        assertThat(traceResponse.body().at("/data/open_api_log_list").size()).isEqualTo(1);
        assertThat(traceResponse.body().toString())
                .contains("consent_create")
                .contains("consent_revoke")
                .contains("company_review_pass")
                .contains("export_approve")
                .contains("open.jobs.sync");

        HttpJsonResponse auditList = getJson(
                "/api/admin/audit-logs?trace_id=trace-p10-chain&biz_type=candidate_consent&page=1&size=2",
                "trace-p10-query-audit-list",
                "Bearer " + operatorToken);
        assertSuccess(auditList, 200, "trace-p10-query-audit-list");
        assertThat(auditList.body().at("/data/total").asLong()).isEqualTo(2);
        assertThat(auditList.body().at("/data/audit_log_list").size()).isEqualTo(2);

        HttpJsonResponse fieldList = getJson(
                "/api/admin/field-access-logs?trace_id=trace-p10-chain&field_name=mobile",
                "trace-p10-query-field-list",
                "Bearer " + auditorToken);
        assertSuccess(fieldList, 200, "trace-p10-query-field-list");
        assertThat(fieldList.body().at("/data/total").asLong()).isEqualTo(1);
        assertThat(fieldList.body().at("/data/access_log_list/0/field_name").asText()).isEqualTo("mobile");

        HttpJsonResponse openList = getJson(
                "/api/admin/open-api-logs?trace_id=trace-p10-chain&api_code=open.jobs.sync&success_flag=true",
                "trace-p10-query-open-list",
                "Bearer " + auditorToken);
        assertSuccess(openList, 200, "trace-p10-query-open-list");
        assertThat(openList.body().at("/data/total").asLong()).isEqualTo(1);
        assertThat(openList.body().at("/data/open_api_log_list/0/api_code").asText()).isEqualTo("open.jobs.sync");

        assertError(getJson(
                "/api/admin/audit-logs?trace_id=trace-p10-chain",
                "trace-p10-candidate-denied",
                "Bearer " + candidateToken), 403, "AUTHZ_403", "trace-p10-candidate-denied");
        assertError(getJson(
                "/api/admin/open-api-logs?trace_id=trace-p10-chain",
                "trace-p10-company-denied",
                "Bearer " + companyToken), 403, "AUTHZ_403", "trace-p10-company-denied");

        assertError(postJson(
                "/api/admin/cms/contents",
                """
                        {
                          "content_type": "notice",
                          "title": "auditor write denied from audit center prompt",
                          "summary": "auditor cannot write",
                          "body_html": "<p>denied</p>",
                          "city_code": "310000",
                          "status": 1
                        }
                        """,
                "trace-p10-auditor-write-denied",
                "Bearer " + auditorToken), 403, "AUTHZ_403", "trace-p10-auditor-write-denied");
    }

    private void insertTraceFixture(String traceId) {
        insertAudit(traceId, "candidate_consent", 101, "consent_create", null, "{\"consent_status\":1}");
        insertAudit(traceId, "candidate_consent", 101, "consent_revoke", "{\"consent_status\":1}",
                "{\"consent_status\":0,\"reason\":\"user revoke\"}");
        insertAudit(traceId, "company", 201, "company_review_pass", "{\"auth_status\":1}", "{\"auth_status\":2}");
        insertAudit(traceId, "export_apply", 301, "export_approve", "{\"approve_status\":0}",
                "{\"approve_status\":1,\"memo\":\"approved\"}");
        insertAudit(traceId, "open_api", 401, "open_stub_accept", null, "{\"api_code\":\"open.jobs.sync\"}");
        jdbcTemplate.update(
                "INSERT INTO field_access_log "
                        + "(operator_id, operator_role, biz_type, biz_id, field_name, access_type, trace_id) "
                        + "VALUES (1, 'operator', 'export_application', 301, 'mobile', 'MASK', ?), "
                        + "(1, 'operator', 'export_application', 301, 'email', 'MASK', ?)",
                traceId,
                traceId);
        jdbcTemplate.update(
                "INSERT INTO open_api_log "
                        + "(source_system, client_code, api_code, trace_id, request_uri, request_method, "
                        + "biz_type, biz_id, http_status, success_flag, request_hash, idempotency_key, "
                        + "duration_ms, request_summary_json, response_summary_json, response_time) "
                        + "VALUES ('stub_partner', 'localtalent_stub', 'open.jobs.sync', ?, "
                        + "'/api/open/v1/jobs/sync', 'POST', 'job', 401, 200, 1, "
                        + "'hash-p10', 'idem-p10-chain', 12, '{\"accepted\":true}', '{\"stub\":true}', CURRENT_TIMESTAMP)",
                traceId);
    }

    private void insertAudit(
            String traceId,
            String bizType,
            long bizId,
            String actionType,
            String beforeJson,
            String afterJson
    ) {
        jdbcTemplate.update(
                "INSERT INTO audit_log "
                        + "(operator_id, operator_role, biz_type, biz_id, action_type, before_json, after_json, trace_id) "
                        + "VALUES (1, 'operator', ?, ?, ?, ?, ?, ?)",
                bizType,
                bizId,
                actionType,
                beforeJson,
                afterJson,
                traceId);
    }
}
