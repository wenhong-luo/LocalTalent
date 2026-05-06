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
                "localtalent.auth.jwt.secret=phase3-audit-regression-secret-change-me",
                "localtalent.auth.jwt.ttl-seconds=3600"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class Phase3AuditTraceRegressionIT extends AuditCenterIntegrationTestSupport {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_p30_audit_trace_test")
            .withUsername("localtalent")
            .withPassword("localtalent");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    void traceShouldJoinAuditFieldAndOpenLogsWithSanitizedResponses() throws Exception {
        String auditorToken = login("operator", insertAuditor(), "LocalTalent@123456", "trace-p30-auditor-login");
        String traceId = "trace-p30-security-chain";

        jdbcTemplate.update(
                "INSERT INTO audit_log "
                        + "(operator_id, operator_role, biz_type, biz_id, action_type, before_json, after_json, trace_id) "
                        + "VALUES (1, 'operator', 'candidate_consent', 101, 'consent_revoke', ?, ?, ?)",
                """
                        {
                          "mobile": "13900001111",
                          "email": "raw-person@example.com",
                          "resume_body": "raw resume body",
                          "attachment_object_key": "private/resume.pdf",
                          "claims": {"sub": "raw-subject"},
                          "nonce": "raw-nonce",
                          "signature": "raw-signature"
                        }
                        """,
                """
                        {
                          "access_token": "raw-access-token",
                          "refresh_token": "raw-refresh-token",
                          "client_secret": "raw-client-secret",
                          "audit_material": "raw audit material",
                          "signin_evidence": "raw signin evidence"
                        }
                        """,
                traceId);
        jdbcTemplate.update(
                "INSERT INTO field_access_log "
                        + "(operator_id, operator_role, biz_type, biz_id, field_name, access_type, trace_id) "
                        + "VALUES (1, 'operator', 'export_application', 301, 'mobile', 'MASK', ?)",
                traceId);
        jdbcTemplate.update(
                "INSERT INTO open_api_log "
                        + "(source_system, client_code, api_code, trace_id, request_uri, request_method, "
                        + "biz_type, biz_id, http_status, success_flag, request_hash, idempotency_key, "
                        + "duration_ms, request_summary_json, response_summary_json, response_time) "
                        + "VALUES ('stub_partner', 'localtalent_stub', 'open.jobs.sync', ?, "
                        + "'/api/open/v1/jobs/sync?access_token=raw-token&signature=raw-signature&nonce=raw-nonce', "
                        + "'POST', 'job', 401, 200, 1, 'hash-p30', 'idem-p30-open', 12, "
                        + "'{\"mobile\":\"13900002222\",\"client_secret\":\"raw-secret\"}', "
                        + "'{\"claims\":\"raw-claims\"}', CURRENT_TIMESTAMP)",
                traceId);

        HttpJsonResponse response = getJson(
                "/api/admin/audit-traces/" + traceId,
                "trace-p30-audit-query",
                "Bearer " + auditorToken);
        assertSuccess(response, 200, "trace-p30-audit-query");
        assertThat(response.body().at("/data/audit_log_list").size()).isEqualTo(1);
        assertThat(response.body().at("/data/access_log_list").size()).isEqualTo(1);
        assertThat(response.body().at("/data/open_api_log_list").size()).isEqualTo(1);

        String body = response.body().toString();
        assertThat(body)
                .contains("[REDACTED]")
                .contains("***@example.com")
                .doesNotContain("13900001111")
                .doesNotContain("13900002222")
                .doesNotContain("raw resume body")
                .doesNotContain("private/resume.pdf")
                .doesNotContain("raw-access-token")
                .doesNotContain("raw-refresh-token")
                .doesNotContain("raw-client-secret")
                .doesNotContain("raw audit material")
                .doesNotContain("raw signin evidence")
                .doesNotContain("raw-signature")
                .doesNotContain("raw-nonce")
                .doesNotContain("raw-subject")
                .doesNotContain("raw-claims")
                .doesNotContain("raw-token");
    }
}
