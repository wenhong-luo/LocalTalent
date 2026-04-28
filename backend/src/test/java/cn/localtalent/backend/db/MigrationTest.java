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
        assertColumnsExist(jdbcTemplate, "candidate_publish_snapshot", List.of("snapshot_json"));
        assertColumnsAbsent(jdbcTemplate, "candidate_publish_snapshot", List.of("mobile", "email", "password_hash"));
        assertColumnsAbsent(jdbcTemplate, "candidate_user", List.of("snapshot_json"));
        assertColumnsExist(jdbcTemplate, "company", List.of(
                "auth_reject_reason",
                "auth_review_user_id",
                "auth_review_time",
                "auth_submit_time"));
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
