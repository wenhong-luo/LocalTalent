package cn.localtalent.backend.company;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
                "localtalent.auth.jwt.secret=portal-company-secret-change-me-with-enough-length",
                "localtalent.auth.jwt.ttl-seconds=3600"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PortalCompanyVisibilityIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_portal_company_test")
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
    void portalCompanyListAndDetailShouldExposeOnlyCertifiedPublicFieldsAndOnlineJobs() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        long certifiedCompanyId = insertCompany(
                "Prompt19 认证企业 " + suffix,
                "P19C-" + suffix.substring(0, 16),
                2,
                "internet",
                "private",
                "50-100",
                "310000");
        long pendingCompanyId = insertCompany(
                "Prompt19 待审企业 " + suffix,
                "P19P-" + suffix.substring(0, 16),
                1,
                "internet",
                "private",
                "50-100",
                "310000");
        long visibleJobId = insertJob(certifiedCompanyId, "Prompt19 Java 工程师 " + suffix, 2, 2);
        insertJob(certifiedCompanyId, "Prompt19 待审核职位 " + suffix, 1, 1);
        insertJob(certifiedCompanyId, "Prompt19 下线职位 " + suffix, 3, 2);
        insertJob(pendingCompanyId, "Prompt19 未认证企业职位 " + suffix, 2, 2);

        HttpJsonResponse list = getJson(
                "/api/portal/companies?keyword=%s&city_code=310000&industry_code=internet"
                        .formatted(suffix)
                        + "&nature_code=private&scale_code=50-100&verified=1&page=1&size=10",
                "trace-p19-company-list");
        assertSuccess(list, 200, "trace-p19-company-list");
        assertThat(list.body().at("/data/total").asLong()).isEqualTo(1);
        JsonNode company = list.body().at("/data/company_list/0");
        assertThat(company.at("/company_id").asLong()).isEqualTo(certifiedCompanyId);
        assertThat(company.at("/company_verified").asBoolean()).isTrue();
        assertThat(company.at("/open_job_count").asLong()).isEqualTo(1);

        String listBody = list.rawBody();
        assertThat(listBody)
                .doesNotContain("license_no")
                .doesNotContain("address")
                .doesNotContain("mobile")
                .doesNotContain("email")
                .doesNotContain("auth_reject_reason")
                .doesNotContain("auth_review_user_id")
                .doesNotContain("营业执照附件")
                .doesNotContain("审核材料")
                .doesNotContain("后台备注");

        HttpJsonResponse unverifiedOnly = getJson(
                "/api/portal/companies?keyword=%s&verified=0&page=1&size=10".formatted(suffix),
                "trace-p19-unverified");
        assertSuccess(unverifiedOnly, 200, "trace-p19-unverified");
        assertThat(unverifiedOnly.body().at("/data/total").asLong()).isZero();

        HttpJsonResponse detail = getJson(
                "/api/portal/companies/" + certifiedCompanyId,
                "trace-p19-company-detail");
        assertSuccess(detail, 200, "trace-p19-company-detail");
        assertThat(detail.body().at("/data/company_id").asLong()).isEqualTo(certifiedCompanyId);
        assertThat(detail.body().at("/data/open_jobs").size()).isEqualTo(1);
        assertThat(detail.body().at("/data/open_jobs/0/job_id").asLong()).isEqualTo(visibleJobId);
        assertThat(detail.rawBody())
                .doesNotContain("license_no")
                .doesNotContain("address")
                .doesNotContain("mobile")
                .doesNotContain("email")
                .doesNotContain("营业执照附件")
                .doesNotContain("审核材料");
    }

    private long insertCompany(
            String companyName,
            String licenseNo,
            int authStatus,
            String industryCode,
            String natureCode,
            String scaleCode,
            String cityCode
    ) {
        jdbcTemplate.update(
                "INSERT INTO company "
                        + "(company_name, license_no, industry_code, nature_code, scale_code, city_code, address, "
                        + "company_profile, auth_status, auth_reject_reason) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                companyName,
                licenseNo,
                industryCode,
                natureCode,
                scaleCode,
                cityCode,
                "internal address should not leak",
                "公开简介：专注地方人才服务，不含联系方式。",
                authStatus,
                authStatus == 3 ? "审核材料不完整" : null);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM company WHERE license_no = ?",
                Long.class,
                licenseNo);
        return id == null ? 0 : id;
    }

    private long insertJob(long companyId, String title, int status, int auditStatus) {
        jdbcTemplate.update(
                "INSERT INTO job_post "
                        + "(company_id, source_type, title, category_code, city_code, salary_min, salary_max, "
                        + "job_desc, status, audit_status, published_at, status_changed_at) "
                        + "VALUES (?, 1, ?, 'software', '310000', 16000, 26000, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                companyId,
                title,
                "公开职位描述，不含联系人手机号邮箱。",
                status,
                auditStatus);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM job_post WHERE company_id = ? AND title = ?",
                Long.class,
                companyId,
                title);
        return id == null ? 0 : id;
    }

    private HttpJsonResponse getJson(String path, String traceId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .header("X-Trace-Id", traceId)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return new HttpJsonResponse(
                response.statusCode(),
                response.body(),
                objectMapper.readTree(response.body()),
                response.headers().firstValue("X-Trace-Id").orElse(null));
    }

    private void assertSuccess(HttpJsonResponse response, int status, String traceId) {
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.headerTraceId()).isEqualTo(traceId);
        assertThat(response.body().at("/trace_id").asText()).isEqualTo(traceId);
        assertThat(response.body().at("/code").asText()).isEqualTo("0");
    }

    private record HttpJsonResponse(int status, String rawBody, JsonNode body, String headerTraceId) {
    }
}
