package cn.localtalent.backend.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class MigrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_test")
            .withUsername("localtalent")
            .withPassword("localtalent");

    @Test
    void shouldMigrateEmptyDatabaseAndKeepSeedIdempotent() throws SQLException {
        Flyway flyway = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .load();

        flyway.migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource());
        assertTablesExist(jdbcTemplate, List.of(
                "candidate_control_profile",
                "candidate_publish_snapshot",
                "candidate_consent",
                "sys_role",
                "sys_menu",
                "sys_role_menu",
                "sys_role_data_scope",
                "sys_role_field_policy",
                "export_apply",
                "open_client",
                "open_api_log",
                "open_api_nonce_record",
                "auth_oidc_identity_link",
                "candidate_job_favorite",
                "candidate_search_subscription",
                "candidate_notification",
                "candidate_resume_onboarding",
                "candidate_resume_ai_suggestion_task",
                "candidate_resume_ai_suggestion_item",
                "portal_recommendation",
                "risk_review",
                "audit_log",
                "field_access_log",
                "api_idempotency_record"));
        assertTablesExist(jdbcTemplate, List.of("admin_user"));

        assertColumnsExist(jdbcTemplate, "candidate_control_profile", List.of(
                "source_type",
                "legal_basis",
                "consent_status",
                "publishable_flag",
                "visibility_scope",
                "consent_version"));
        assertColumnsExist(jdbcTemplate, "candidate_publish_snapshot", List.of(
                "snapshot_json",
                "city_code",
                "category_code",
                "display_name_masked",
                "experience_years"));
        assertGeneratedColumnsExist(jdbcTemplate, "candidate_publish_snapshot", List.of(
                "city_code",
                "category_code",
                "display_name_masked",
                "experience_years"));
        assertIndexesExist(jdbcTemplate, "candidate_publish_snapshot", List.of(
                "idx_portal_snapshot_visible_time",
                "idx_portal_snapshot_filter_time"));
        assertColumnsAbsent(jdbcTemplate, "candidate_publish_snapshot", List.of("mobile", "email", "password_hash"));
        assertColumnsAbsent(jdbcTemplate, "candidate_user", List.of("snapshot_json"));
        assertColumnsExist(jdbcTemplate, "company", List.of(
                "nature_code",
                "certification_material_summary_json",
                "auth_reject_reason",
                "auth_review_user_id",
                "auth_review_time",
                "auth_submit_time"));
        assertIndexesExist(jdbcTemplate, "company", List.of("idx_company_portal_search"));
        assertColumnsExist(jdbcTemplate, "job_post", List.of(
                "review_memo",
                "reject_reason",
                "review_user_id",
                "review_time",
                "offline_reason",
                "status_changed_at"));
        assertColumnsExist(jdbcTemplate, "interview_session", List.of(
                "application_id",
                "signin_code_hash",
                "signin_code_expires_at",
                "signin_code_used_at"));
        assertColumnsExist(jdbcTemplate, "export_apply", List.of(
                "company_id",
                "apply_identity_type",
                "apply_role_code",
                "reject_reason",
                "generate_status",
                "file_object_key",
                "file_sha256",
                "file_size_bytes",
                "generated_at",
                "download_issued_at",
                "download_count",
                "error_msg"));
        assertColumnsExist(jdbcTemplate, "open_api_log", List.of(
                "client_code",
                "request_hash",
                "idempotency_key",
                "duration_ms",
                "request_summary_json",
                "response_summary_json"));
        assertColumnsExist(jdbcTemplate, "integration_sync_task", List.of(
                "trace_id",
                "api_code",
                "open_api_log_id",
                "max_retry_count"));
        assertIndexesExist(jdbcTemplate, "audit_log", List.of("idx_audit_trace_time"));
        assertIndexesExist(jdbcTemplate, "field_access_log", List.of("idx_field_access_trace_time"));
        assertIndexesExist(jdbcTemplate, "open_api_log", List.of("idx_open_api_trace_time"));
        assertColumnsExist(jdbcTemplate, "auth_oidc_identity_link", List.of(
                "issuer",
                "subject_sha256",
                "identity_type",
                "user_id",
                "company_id",
                "email_hash",
                "last_login_at"));
        assertIndexesExist(jdbcTemplate, "auth_oidc_identity_link", List.of(
                "uk_oidc_issuer_subject",
                "idx_oidc_identity"));
        assertColumnsExist(jdbcTemplate, "candidate_job_favorite", List.of(
                "candidate_id",
                "job_id",
                "status",
                "created_at",
                "updated_at"));
        assertIndexesExist(jdbcTemplate, "candidate_job_favorite", List.of(
                "uk_candidate_job_favorite",
                "idx_candidate_favorite_status",
                "idx_favorite_job"));
        assertColumnsExist(jdbcTemplate, "candidate_search_subscription", List.of(
                "candidate_id",
                "subscription_name",
                "keyword",
                "city_code",
                "category_code",
                "salary_min",
                "salary_max",
                "status",
                "last_triggered_at"));
        assertIndexesExist(jdbcTemplate, "candidate_search_subscription", List.of(
                "idx_candidate_subscription_status",
                "idx_subscription_filter"));
        assertColumnsExist(jdbcTemplate, "candidate_notification", List.of(
                "candidate_id",
                "notification_type",
                "title",
                "content_summary",
                "biz_type",
                "biz_id",
                "read_status",
                "read_at"));
        assertIndexesExist(jdbcTemplate, "candidate_notification", List.of(
                "idx_candidate_notification_read",
                "idx_candidate_notification_biz"));
        assertColumnsExist(jdbcTemplate, "candidate_resume_onboarding", List.of(
                "candidate_id",
                "resume_id",
                "onboarding_status",
                "current_step",
                "completion_score",
                "started_at",
                "completed_at",
                "version"));
        assertIndexesExist(jdbcTemplate, "candidate_resume_onboarding", List.of(
                "uk_candidate_resume_onboarding_candidate",
                "idx_candidate_resume_onboarding_status",
                "idx_candidate_resume_onboarding_resume"));
        assertColumnsExist(jdbcTemplate, "candidate_resume", List.of(
                "attachment_object_key",
                "attachment_file_name",
                "attachment_content_type",
                "attachment_size_bytes",
                "attachment_uploaded_at",
                "attachment_sha256"));
        assertIndexesExist(jdbcTemplate, "candidate_resume", List.of("idx_candidate_resume_attachment_time"));
        assertColumnsExist(jdbcTemplate, "candidate_resume_ai_suggestion_task", List.of(
                "candidate_id",
                "resume_id",
                "task_status",
                "suggestion_count",
                "applied_count",
                "dismissed_count",
                "generated_at"));
        assertIndexesExist(jdbcTemplate, "candidate_resume_ai_suggestion_task", List.of(
                "idx_ai_task_candidate_time",
                "idx_ai_task_resume"));
        assertColumnsExist(jdbcTemplate, "candidate_resume_ai_suggestion_item", List.of(
                "task_id",
                "candidate_id",
                "resume_id",
                "suggestion_type",
                "target_field",
                "title",
                "reason_summary",
                "before_preview",
                "suggested_value",
                "can_apply",
                "apply_status",
                "applied_at",
                "dismissed_at"));
        assertIndexesExist(jdbcTemplate, "candidate_resume_ai_suggestion_item", List.of(
                "idx_ai_item_task",
                "idx_ai_item_candidate_status",
                "idx_ai_item_resume"));
        assertColumnsExist(jdbcTemplate, "job_application", List.of(
                "company_stage_note",
                "stage_changed_by",
                "stage_changed_at"));
        assertIndexesExist(jdbcTemplate, "job_application", List.of("idx_application_company_stage_time"));
        assertColumnsExist(jdbcTemplate, "portal_recommendation", List.of(
                "slot_code",
                "target_type",
                "target_id",
                "title_override",
                "summary_override",
                "display_order",
                "status",
                "start_time",
                "end_time",
                "operator_id"));
        assertIndexesExist(jdbcTemplate, "portal_recommendation", List.of(
                "idx_slot_status_order",
                "idx_target",
                "idx_status_time"));
        assertColumnsExist(jdbcTemplate, "risk_review", List.of(
                "risk_type",
                "target_type",
                "target_id",
                "severity",
                "status",
                "title",
                "summary",
                "decision",
                "handler_id",
                "handled_at"));
        assertIndexesExist(jdbcTemplate, "risk_review", List.of(
                "idx_status_severity_time",
                "idx_target",
                "idx_handler_time"));

        int dictTypeCount = countRows(jdbcTemplate, "sys_dict_type");
        int dictItemCount = countRows(jdbcTemplate, "sys_dict_item");
        int roleCount = countRows(jdbcTemplate, "sys_role");
        int menuCount = countRows(jdbcTemplate, "sys_menu");
        int roleMenuCount = countRows(jdbcTemplate, "sys_role_menu");
        int fieldPolicyCount = countRows(jdbcTemplate, "sys_role_field_policy");
        int adminUserCount = countRows(jdbcTemplate, "admin_user");
        int openClientCount = countRows(jdbcTemplate, "open_client");

        executeSeedScript();
        executeAuthSeedScript();
        executeOpenApiSeedScript();
        executeSeedScript();
        executeAuthSeedScript();
        executeOpenApiSeedScript();

        assertThat(countRows(jdbcTemplate, "sys_dict_type")).isEqualTo(dictTypeCount);
        assertThat(countRows(jdbcTemplate, "sys_dict_item")).isEqualTo(dictItemCount);
        assertThat(countRows(jdbcTemplate, "sys_role")).isEqualTo(roleCount);
        assertThat(countRows(jdbcTemplate, "sys_menu")).isEqualTo(menuCount);
        assertThat(countRows(jdbcTemplate, "sys_role_menu")).isEqualTo(roleMenuCount);
        assertThat(countRows(jdbcTemplate, "sys_role_field_policy")).isEqualTo(fieldPolicyCount);
        assertThat(countRows(jdbcTemplate, "admin_user")).isEqualTo(adminUserCount);
        assertThat(countRows(jdbcTemplate, "open_client")).isEqualTo(openClientCount);
        assertThat(queryString(jdbcTemplate,
                "SELECT client_secret_hash FROM open_client WHERE client_code = 'localtalent_stub'"))
                .isEqualTo("97911de7ae1a8392865d8a6ce438d39b59236664f2985d0e84fd4be47be5779b")
                .doesNotContain("LocalTalentOpen");
    }

    private static DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(MYSQL.getDriverClassName());
        dataSource.setUrl(MYSQL.getJdbcUrl());
        dataSource.setUsername(MYSQL.getUsername());
        dataSource.setPassword(MYSQL.getPassword());
        return dataSource;
    }

    private static void assertTablesExist(JdbcTemplate jdbcTemplate, List<String> tableNames) {
        for (String tableName : tableNames) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                    Integer.class,
                    tableName);
            assertThat(count).as("table %s should exist", tableName).isEqualTo(1);
        }
    }

    private static void assertColumnsExist(JdbcTemplate jdbcTemplate, String tableName, List<String> columnNames) {
        for (String columnName : columnNames) {
            assertThat(columnCount(jdbcTemplate, tableName, columnName))
                    .as("column %s.%s should exist", tableName, columnName)
                    .isEqualTo(1);
        }
    }

    private static void assertColumnsAbsent(JdbcTemplate jdbcTemplate, String tableName, List<String> columnNames) {
        for (String columnName : columnNames) {
            assertThat(columnCount(jdbcTemplate, tableName, columnName))
                    .as("column %s.%s should not exist", tableName, columnName)
                    .isZero();
        }
    }

    private static void assertIndexesExist(JdbcTemplate jdbcTemplate, String tableName, List<String> indexNames) {
        for (String indexName : indexNames) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.statistics "
                            + "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                    Integer.class,
                    tableName,
                    indexName);
            assertThat(count == null ? 0 : count)
                    .as("index %s.%s should exist", tableName, indexName)
                    .isGreaterThan(0);
        }
    }

    private static void assertGeneratedColumnsExist(
            JdbcTemplate jdbcTemplate,
            String tableName,
            List<String> columnNames
    ) {
        for (String columnName : columnNames) {
            String extra = jdbcTemplate.queryForObject(
                    "SELECT extra FROM information_schema.columns "
                            + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                    String.class,
                    tableName,
                    columnName);
            assertThat(extra)
                    .as("column %s.%s should be stored generated", tableName, columnName)
                    .containsIgnoringCase("STORED GENERATED");
        }
    }

    private static int columnCount(JdbcTemplate jdbcTemplate, String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class,
                tableName,
                columnName);
        return count == null ? 0 : count;
    }

    private static int countRows(JdbcTemplate jdbcTemplate, String tableName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
        return count == null ? 0 : count;
    }

    private static String queryString(JdbcTemplate jdbcTemplate, String sql) {
        return jdbcTemplate.queryForObject(sql, String.class);
    }

    private static void executeSeedScript() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())) {
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/R__seed_base_dictionary_roles_menus.sql"));
        }
    }

    private static void executeAuthSeedScript() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())) {
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/R__seed_auth_internal_accounts.sql"));
        }
    }

    private static void executeOpenApiSeedScript() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())) {
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/R__seed_open_api_stub_client.sql"));
        }
    }
}
