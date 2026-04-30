package cn.localtalent.backend.candidate;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
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
                "localtalent.auth.jwt.secret=portal-blacklist-secret-change-me-with-enough-length",
                "localtalent.auth.jwt.ttl-seconds=3600"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PortalFieldBlacklistTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_portal_blacklist_test")
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
    void portalShouldOnlyReturnWhitelistedSnapshotFields() throws Exception {
        long candidateId = insertCandidate();
        long snapshotId = insertSnapshotWithUnexpectedRawFields(candidateId);

        HttpJsonResponse response = getJson(
                "/api/portal/talent-snapshots?city_code=110000&category_code=operations&page=1&size=10",
                "trace-portal-blacklist");
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body().at("/code").asText()).isEqualTo("0");
        assertThat(response.body().at("/data/total").asLong()).isEqualTo(1);
        assertThat(response.body().at("/data/snapshot_list/0/snapshot_id").asLong()).isEqualTo(snapshotId);
        assertThat(response.body().at("/data/snapshot_list/0/display_name_masked").asText()).isEqualTo("测*");

        String responseJson = response.body().toString();
        assertThat(responseJson)
                .contains("display_name_masked")
                .contains("city_code")
                .contains("category_code")
                .contains("skills_summary")
                .contains("experience_years")
                .doesNotContain("mobile")
                .doesNotContain("email")
                .doesNotContain("password_hash")
                .doesNotContain("resume_body")
                .doesNotContain("attachment_object_key")
                .doesNotContain("evidence")
                .doesNotContain("base_profile_json")
                .doesNotContain("education_json")
                .doesNotContain("experience_json")
                .doesNotContain("skills_json")
                .doesNotContain("raw-secret-value")
                .doesNotContain("raw@example.com");
    }

    @Test
    void portalShouldFilterVisibleSnapshotsByLowRiskPublicColumnsOnly() throws Exception {
        long candidateId = insertCandidate();
        long targetSnapshotId = insertSnapshot(
                candidateId,
                "李*",
                "310000",
                "software",
                "目标公开技能",
                6,
                1,
                1,
                1);
        insertSnapshot(candidateId, "低*", "310000", "software", "经验不足", 2, 1, 1, 1);
        insertSnapshot(candidateId, "高*", "310000", "software", "经验过高", 8, 1, 1, 1);
        insertSnapshot(candidateId, "旧*", "310000", "software", "旧快照", 6, 1, 1, 20);
        insertSnapshot(candidateId, "城*", "110000", "software", "城市不匹配", 6, 1, 1, 1);
        insertSnapshot(candidateId, "下*", "310000", "software", "下线不可见", 6, 0, 1, 1);
        insertSnapshot(candidateId, "撤*", "310000", "software", "不可发布不可见", 6, 1, 0, 1);

        HttpJsonResponse response = getJson(
                "/api/portal/talent-snapshots"
                        + "?city_code=310000"
                        + "&category_code=software"
                        + "&experience_min=5"
                        + "&experience_max=7"
                        + "&updated_within=7"
                        + "&sort=experience_desc"
                        + "&page=1"
                        + "&size=10",
                "trace-portal-filter");

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body().at("/code").asText()).isEqualTo("0");
        assertThat(response.body().at("/data/total").asLong()).isEqualTo(1);
        assertThat(response.body().at("/data/snapshot_list/0/snapshot_id").asLong()).isEqualTo(targetSnapshotId);
        assertThat(response.body().at("/data/snapshot_list/0/skills_summary").asText()).isEqualTo("目标公开技能");
        assertThat(response.body().toString())
                .doesNotContain("下线不可见")
                .doesNotContain("不可发布不可见")
                .doesNotContain("城市不匹配")
                .doesNotContain("旧快照");
    }

    private long insertCandidate() {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO candidate_user (email, password_hash, real_name, status) "
                            + "VALUES (?, '$2a$10$placeholder', '测试候选人', 1)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, "portal-blacklist-" + UUID.randomUUID() + "@example.com");
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        assertThat(key).isNotNull();
        return key.longValue();
    }

    private long insertSnapshotWithUnexpectedRawFields(long candidateId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO candidate_publish_snapshot "
                            + "(candidate_id, source_type, legal_basis, consent_status, publishable_flag, "
                            + "consent_version, visibility_scope, snapshot_json, sync_version, status) "
                            + "VALUES (?, 1, 'consent', 1, 1, 'v-test', 4, ?, 1, 1)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, candidateId);
            statement.setString(2, """
                    {
                      "display_name_masked": "测*",
                      "city_code": "110000",
                      "category_code": "operations",
                      "skills_summary": "公共展示技能",
                      "experience_years": 3,
                      "mobile": "13812345678",
                      "email": "raw@example.com",
                      "password_hash": "raw-secret-value",
                      "resume_body": "raw-secret-value",
                      "attachment_object_key": "raw-secret-value",
                      "evidence": "raw-secret-value",
                      "base_profile_json": "raw-secret-value",
                      "education_json": "raw-secret-value",
                      "experience_json": "raw-secret-value",
                      "skills_json": "raw-secret-value"
                    }
                    """);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        assertThat(key).isNotNull();
        return key.longValue();
    }

    private long insertSnapshot(
            long candidateId,
            String displayNameMasked,
            String cityCode,
            String categoryCode,
            String skillsSummary,
            int experienceYears,
            int status,
            int publishableFlag,
            int daysAgo
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO candidate_publish_snapshot "
                            + "(candidate_id, source_type, legal_basis, consent_status, publishable_flag, "
                            + "consent_version, visibility_scope, snapshot_json, sync_version, status) "
                            + "VALUES (?, 1, 'consent', 1, ?, 'v-filter', 4, ?, 1, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, candidateId);
            statement.setInt(2, publishableFlag);
            statement.setString(3, """
                    {
                      "display_name_masked": "%s",
                      "city_code": "%s",
                      "category_code": "%s",
                      "skills_summary": "%s",
                      "experience_years": %d
                    }
                    """.formatted(displayNameMasked, cityCode, categoryCode, skillsSummary, experienceYears));
            statement.setInt(4, status);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        assertThat(key).isNotNull();
        long snapshotId = key.longValue();
        jdbcTemplate.update(
                "UPDATE candidate_publish_snapshot SET updated_at = TIMESTAMPADD(DAY, ?, CURRENT_TIMESTAMP) WHERE id = ?",
                -daysAgo,
                snapshotId);
        return snapshotId;
    }

    private HttpJsonResponse getJson(String path, String traceId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("X-Trace-Id", traceId)
                .GET()
                .build();
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
