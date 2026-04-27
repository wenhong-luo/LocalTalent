package cn.localtalent.backend.admin;

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
                "localtalent.auth.jwt.secret=prompt7-review-secret-change-me-with-enough-length",
                "localtalent.auth.jwt.ttl-seconds=3600"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReviewAffectsVisibilityIT extends AdminIntegrationTestSupport {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_admin_review_test")
            .withUsername("localtalent")
            .withPassword("localtalent");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    void operatorReviewShouldAffectPortalVisibilityAndWriteAudit() throws Exception {
        Account company = registerAndLoginCompany("review");
        String operatorToken = login("operator", "operator", "LocalTalent@123456", "trace-p7-review-operator-login");
        long jobId = createJob(company.token(), "Prompt7 审核职位", "trace-p7-review-job-create");

        HttpJsonResponse companyQueue = getJson(
                "/api/admin/companies/review?auth_status=1&page=1&size=10",
                "trace-p7-company-review-queue",
                "Bearer " + operatorToken);
        assertSuccess(companyQueue, 200, "trace-p7-company-review-queue");
        assertThat(companyQueue.body().at("/data/total").asLong()).isGreaterThanOrEqualTo(1);

        HttpJsonResponse companyDetail = getJson(
                "/api/admin/companies/review/" + company.companyId(),
                "trace-p7-company-review-detail",
                "Bearer " + operatorToken);
        assertSuccess(companyDetail, 200, "trace-p7-company-review-detail");
        assertThat(companyDetail.body().at("/data/company_id").asLong()).isEqualTo(company.companyId());

        HttpJsonResponse rejectCompany = postJson(
                "/api/admin/companies/review",
                """
                        {
                          "company_id": %d,
                          "audit_status": 3,
                          "memo": "prompt7 company reject reason"
                        }
                        """.formatted(company.companyId()),
                "trace-p7-company-review-reject",
                "Bearer " + operatorToken);
        assertSuccess(rejectCompany, 200, "trace-p7-company-review-reject");
        assertThat(rejectCompany.body().at("/data/auth_status").asInt()).isEqualTo(3);
        assertThat(queryString(
                "SELECT after_json FROM audit_log WHERE trace_id = ? AND action_type = ?",
                "trace-p7-company-review-reject",
                "company_review_reject")).contains("prompt7 company reject reason");

        applyCompany(company, "trace-p7-company-reapply");

        HttpJsonResponse approveCompany = postJson(
                "/api/admin/companies/review",
                """
                        {
                          "company_id": %d,
                          "audit_status": 2,
                          "memo": "prompt7 company pass"
                        }
                        """.formatted(company.companyId()),
                "trace-p7-company-review-pass",
                "Bearer " + operatorToken);
        assertSuccess(approveCompany, 200, "trace-p7-company-review-pass");
        assertThat(approveCompany.body().at("/data/auth_status").asInt()).isEqualTo(2);

        HttpJsonResponse jobQueue = getJson(
                "/api/admin/jobs/review?audit_status=1&page=1&size=10",
                "trace-p7-job-review-queue",
                "Bearer " + operatorToken);
        assertSuccess(jobQueue, 200, "trace-p7-job-review-queue");
        assertThat(jobQueue.body().at("/data/total").asLong()).isGreaterThanOrEqualTo(1);

        HttpJsonResponse jobDetail = getJson(
                "/api/admin/jobs/review/" + jobId,
                "trace-p7-job-review-detail",
                "Bearer " + operatorToken);
        assertSuccess(jobDetail, 200, "trace-p7-job-review-detail");
        assertThat(jobDetail.body().at("/data/job_id").asLong()).isEqualTo(jobId);

        HttpJsonResponse rejectJob = postJson(
                "/api/admin/jobs/review",
                """
                        {
                          "job_id": %d,
                          "audit_status": 3,
                          "memo": "prompt7 job reject reason"
                        }
                        """.formatted(jobId),
                "trace-p7-job-review-reject",
                "Bearer " + operatorToken);
        assertSuccess(rejectJob, 200, "trace-p7-job-review-reject");
        assertThat(rejectJob.body().at("/data/audit_status").asInt()).isEqualTo(3);
        assertThat(queryString(
                "SELECT after_json FROM audit_log WHERE trace_id = ? AND action_type = ?",
                "trace-p7-job-review-reject",
                "job_review_reject")).contains("prompt7 job reject reason");

        HttpJsonResponse invisibleAfterReject = getJson(
                "/api/portal/jobs?city_code=310000&category_code=software&page=1&size=10",
                "trace-p7-portal-job-rejected",
                null);
        assertSuccess(invisibleAfterReject, 200, "trace-p7-portal-job-rejected");
        assertThat(invisibleAfterReject.body().at("/data/total").asLong()).isZero();

        submitJobReview(company.token(), jobId, "trace-p7-job-resubmit");

        HttpJsonResponse approveJob = postJson(
                "/api/admin/jobs/review",
                """
                        {
                          "job_id": %d,
                          "audit_status": 2,
                          "memo": "prompt7 job pass"
                        }
                        """.formatted(jobId),
                "trace-p7-job-review-pass",
                "Bearer " + operatorToken);
        assertSuccess(approveJob, 200, "trace-p7-job-review-pass");
        assertThat(approveJob.body().at("/data/status").asInt()).isEqualTo(2);

        HttpJsonResponse visibleAfterPass = getJson(
                "/api/portal/jobs?city_code=310000&category_code=software&page=1&size=10",
                "trace-p7-portal-job-visible",
                null);
        assertSuccess(visibleAfterPass, 200, "trace-p7-portal-job-visible");
        assertThat(visibleAfterPass.body().at("/data/total").asLong()).isEqualTo(1);
        assertThat(visibleAfterPass.body().at("/data/job_list/0/job_id").asLong()).isEqualTo(jobId);

        assertThat(countAudit("trace-p7-company-review-reject", "company", "company_review_reject")).isEqualTo(1);
        assertThat(countAudit("trace-p7-company-review-pass", "company", "company_review_pass")).isEqualTo(1);
        assertThat(countAudit("trace-p7-job-review-reject", "job_post", "job_review_reject")).isEqualTo(1);
        assertThat(countAudit("trace-p7-job-review-pass", "job_post", "job_review_pass")).isEqualTo(1);
    }
}
