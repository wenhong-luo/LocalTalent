package cn.localtalent.backend.authz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.localtalent.backend.audit.AuditAction;
import cn.localtalent.backend.auth.domain.IdentityType;
import cn.localtalent.backend.common.exception.ApiException;
import cn.localtalent.backend.common.trace.TraceIdContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
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
        properties = {
                "localtalent.auth.jwt.secret=authz-data-scope-secret-change-me-with-enough-length",
                "localtalent.auth.jwt.ttl-seconds=3600"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DataScopeIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_authz_scope_test")
            .withUsername("localtalent")
            .withPassword("localtalent");

    @Autowired
    private DataScopeService dataScopeService;

    @Autowired
    private FieldPolicyEngine fieldPolicyEngine;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuditedFixtureService auditedFixtureService;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @AfterEach
    void clearContexts() {
        AuthzContext.clear();
        TraceIdContext.clear();
    }

    @Test
    void candidateShouldOnlyAccessSelfScope() {
        AuthzPrincipal candidate = principal(IdentityType.CANDIDATE, 10L, null, "candidate");

        dataScopeService.assertAccessible(candidate, "candidate_profile", ResourceOwner.candidate(10L));

        assertThatThrownBy(() -> dataScopeService.assertAccessible(
                candidate,
                "candidate_profile",
                ResourceOwner.candidate(11L)))
                .isInstanceOf(ApiException.class)
                .hasMessage("data scope denied");
    }

    @Test
    void companyAdminShouldOnlyAccessOwnCompanyScope() {
        AuthzPrincipal companyAdmin = principal(IdentityType.COMPANY, 20L, 100L, "company_admin");

        dataScopeService.assertAccessible(companyAdmin, "job_application", ResourceOwner.company(100L));

        assertThatThrownBy(() -> dataScopeService.assertAccessible(
                companyAdmin,
                "job_application",
                ResourceOwner.company(101L)))
                .isInstanceOf(ApiException.class)
                .hasMessage("data scope denied");
    }

    @Test
    void operatorShouldReadAggregateButNotDetailWithoutExplicitAuthorization() {
        AuthzPrincipal operator = principal(IdentityType.OPERATOR, 1L, null, "operator");

        dataScopeService.assertAccessible(operator, "candidate_profile", ResourceOwner.aggregate());
        dataScopeService.assertAccessible(operator, "candidate_profile", ResourceOwner.companyDetail(100L));

        assertThatThrownBy(() -> dataScopeService.assertAccessible(
                operator,
                "candidate_profile",
                ResourceOwner.company(100L)))
                .isInstanceOf(ApiException.class)
                .hasMessage("data scope denied");
    }

    @Test
    void auditorShouldReadAuditScopeButMustNotWrite() {
        AuthzPrincipal auditor = principal(IdentityType.OPERATOR, 2L, null, "auditor");

        dataScopeService.assertAccessible(auditor, "audit_log", ResourceOwner.audit());

        assertThatThrownBy(() -> dataScopeService.assertWritable(auditor, "audit_log", ResourceOwner.audit()))
                .isInstanceOf(ApiException.class)
                .hasMessage("auditor is read only");
    }

    @Test
    void fieldAccessAndAuditLogsShouldBeStoredWithTraceId() {
        AuthzPrincipal operator = principal(IdentityType.OPERATOR, 1L, null, "operator");
        AuthzContext.setCurrentPrincipal(operator);
        TraceIdContext.setCurrentTraceId("trace-authz-audit");
        upsertFieldPolicy("operator", "candidate_profile", "mobile", "MASK", "MOBILE");

        fieldPolicyEngine.apply(operator, "candidate_profile", 9001L, List.of(
                new ProtectedField("mobile", "13812345678", FieldSensitivity.S3)));
        auditedFixtureService.updateCandidateProfile(9001L);

        Integer fieldAccessCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM field_access_log "
                        + "WHERE trace_id = ? AND biz_id = ? AND field_name = ? AND access_type = ?",
                Integer.class,
                "trace-authz-audit",
                9001L,
                "mobile",
                "MASK");
        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log "
                        + "WHERE trace_id = ? AND biz_id = ? AND action_type = ?",
                Integer.class,
                "trace-authz-audit",
                9001L,
                "test_update");

        assertThat(fieldAccessCount).isEqualTo(1);
        assertThat(auditCount).isEqualTo(1);
    }

    private void upsertFieldPolicy(String roleCode, String bizType, String fieldName, String policyType, String maskRule) {
        jdbcTemplate.update(
                "INSERT INTO sys_role_field_policy (role_id, biz_type, field_name, policy_type, mask_rule, status) "
                        + "SELECT id, ?, ?, ?, ?, 1 FROM sys_role WHERE role_code = ? "
                        + "ON DUPLICATE KEY UPDATE policy_type = VALUES(policy_type), "
                        + "mask_rule = VALUES(mask_rule), status = 1",
                bizType,
                fieldName,
                policyType,
                maskRule,
                roleCode);
    }

    private AuthzPrincipal principal(IdentityType identityType, long userId, Long companyId, String roleCode) {
        return new AuthzPrincipal(identityType, userId, companyId, List.of(roleCode), "test-token");
    }

    @TestConfiguration
    static class FixtureConfig {

        @Bean
        AuditedFixtureService auditedFixtureService() {
            return new AuditedFixtureService();
        }
    }

    static class AuditedFixtureService {

        @AuditAction(bizType = "candidate_profile", actionType = "test_update", bizIdArgIndex = 0)
        public void updateCandidateProfile(long candidateId) {
        }
    }
}
