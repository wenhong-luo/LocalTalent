package cn.localtalent.backend.candidatecenter;

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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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
import cn.localtalent.backend.candidatecenter.infrastructure.CandidateResumeAttachmentStorageService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "localtalent.auth.jwt.secret=candidate-closure-flow-secret-change-me",
                "localtalent.auth.jwt.ttl-seconds=3600",
                "localtalent.phase3.candidate-closure=true",
                "localtalent.phase3.resume-attachment-upload=true",
                "localtalent.phase3.resume-ai-assist=true"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CandidateClosureFlowIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_candidate_closure_flow_test")
            .withUsername("localtalent")
            .withPassword("localtalent");

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CandidateResumeAttachmentStorageService attachmentStorageService;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @TestConfiguration
    static class AttachmentStorageTestConfiguration {
        @Bean
        @Primary
        CandidateResumeAttachmentStorageService candidateResumeAttachmentStorageService() {
            return mock(CandidateResumeAttachmentStorageService.class);
        }
    }

    @Test
    void candidateClosureShouldSupportPrivateFlowWithIdorAuditAndRevokePropagation() throws Exception {
        CandidateAccount candidate = registerAndLoginCandidate("main");
        CandidateAccount otherCandidate = registerAndLoginCandidate("other");
        long jobId = insertVisibleJob();

        HttpJsonResponse overview = getJson(
                "/api/candidate/center/overview",
                "trace-p3-closure-overview",
                "Bearer " + candidate.token());
        assertSuccess(overview, 200, "trace-p3-closure-overview");
        assertThat(overview.body().at("/data/features/candidate_closure_enabled").asBoolean()).isTrue();
        assertThat(overview.body().at("/data/features/resume_ai_assist_enabled").asBoolean()).isTrue();
        assertThat(overview.body().at("/data/stats/favorite_count").asLong()).isZero();
        assertThat(overview.body().at("/data/onboarding/onboarding_required").asBoolean()).isTrue();
        assertThat(overview.body().at("/data/onboarding/onboarding_step").asText()).isEqualTo("basic");

        String basicResumeBody = basicResumeBody("三期闭环简历", "候选人三期");
        HttpJsonResponse savedBasic = putJson(
                "/api/candidate/center/resume",
                basicResumeBody,
                "trace-p3-resume-basic",
                "Bearer " + candidate.token(),
                "idem-p3-resume-basic-001");
        assertSuccess(savedBasic, 200, "trace-p3-resume-basic");
        assertThat(savedBasic.body().at("/data/resume_status").asText()).isEqualTo("needs_completion");
        assertThat(onboardingValue(candidate.userId(), "onboarding_status")).isEqualTo("basic_saved");
        assertThat(onboardingValue(candidate.userId(), "current_step")).isEqualTo("detail");

        HttpJsonResponse basicOverview = getJson(
                "/api/candidate/center/overview",
                "trace-p3-closure-basic-overview",
                "Bearer " + candidate.token());
        assertSuccess(basicOverview, 200, "trace-p3-closure-basic-overview");
        assertThat(basicOverview.body().at("/data/onboarding/onboarding_required").asBoolean()).isTrue();
        assertThat(basicOverview.body().at("/data/onboarding/onboarding_step").asText()).isEqualTo("detail");

        String resumeBody = resumeBody("三期闭环简历", "候选人三期", "Java, Spring");
        HttpJsonResponse savedResume = putJson(
                "/api/candidate/center/resume",
                resumeBody,
                "trace-p3-resume-save",
                "Bearer " + candidate.token(),
                "idem-p3-resume-001");
        assertSuccess(savedResume, 200, "trace-p3-resume-save");
        assertThat(savedResume.body().at("/data/resume_status").asText()).isEqualTo("complete");
        assertThat(savedResume.body().at("/data/base_profile/display_name").asText()).isEqualTo("候选人三期");
        assertNoRawCandidateData(savedResume);
        assertThat(onboardingValue(candidate.userId(), "onboarding_status")).isEqualTo("completed");
        assertThat(onboardingValue(candidate.userId(), "current_step")).isEqualTo("done");

        HttpJsonResponse repeatedResume = putJson(
                "/api/candidate/center/resume",
                resumeBody,
                "trace-p3-resume-repeat",
                "Bearer " + candidate.token(),
                "idem-p3-resume-001");
        assertSuccess(repeatedResume, 200, "trace-p3-resume-repeat");
        assertThat(onboardingValue(candidate.userId(), "onboarding_status")).isEqualTo("completed");
        HttpJsonResponse conflictResume = putJson(
                "/api/candidate/center/resume",
                resumeBody("三期闭环简历 v2", "候选人三期", "Java"),
                "trace-p3-resume-conflict",
                "Bearer " + candidate.token(),
                "idem-p3-resume-001");
        assertError(conflictResume, 409, "IDEMPOTENCY_409", "trace-p3-resume-conflict");

        long resumeId = savedResume.body().at("/data/resume_id").asLong();
        insertApplication(candidate.userId(), jobId, resumeId);
        HttpJsonResponse applications = getJson(
                "/api/candidate/center/applications?page=1&size=20",
                "trace-p3-applications",
                "Bearer " + candidate.token());
        assertSuccess(applications, 200, "trace-p3-applications");
        assertThat(applications.body().at("/data/total").asLong()).isEqualTo(1);
        assertThat(applications.body().at("/data/application_list/0/job_title").asText()).isEqualTo("P3 Java 工程师");

        HttpJsonResponse favorites = postJson(
                "/api/candidate/center/favorites",
                """
                        {
                          "job_id": %d
                        }
                        """.formatted(jobId),
                "trace-p3-favorite-create",
                "Bearer " + candidate.token(),
                "idem-p3-favorite-001");
        assertSuccess(favorites, 200, "trace-p3-favorite-create");
        long favoriteId = favorites.body().at("/data/favorite_list/0/favorite_id").asLong();
        assertThat(favoriteId).isPositive();

        HttpJsonResponse subscription = postJson(
                "/api/candidate/center/subscriptions",
                """
                        {
                          "subscription_name": "后端职位订阅",
                          "keyword": "Java",
                          "city_code": "310000",
                          "category_code": "software",
                          "salary_min": 15000,
                          "salary_max": 30000
                        }
                        """,
                "trace-p3-subscription-create",
                "Bearer " + candidate.token(),
                "idem-p3-subscription-001");
        assertSuccess(subscription, 200, "trace-p3-subscription-create");
        long subscriptionId = subscription.body().at("/data/subscription_list/0/subscription_id").asLong();
        assertThat(subscriptionId).isPositive();

        HttpJsonResponse notifications = getJson(
                "/api/candidate/center/notifications?page=1&size=20",
                "trace-p3-notifications",
                "Bearer " + candidate.token());
        assertSuccess(notifications, 200, "trace-p3-notifications");
        long notificationId = notifications.body().at("/data/notification_list/0/notification_id").asLong();
        assertThat(notifications.body().at("/data/notification_list/0/read_status").asText()).isEqualTo("unread");

        HttpJsonResponse readNotification = postJson(
                "/api/candidate/center/notifications/" + notificationId + "/read",
                "{}",
                "trace-p3-notification-read",
                "Bearer " + candidate.token(),
                "idem-p3-notification-001");
        assertSuccess(readNotification, 200, "trace-p3-notification-read");
        assertThat(readNotification.body().at("/data/notification_list/0/read_status").asText()).isEqualTo("read");

        HttpJsonResponse otherCancelsSubscription = postJson(
                "/api/candidate/center/subscriptions/" + subscriptionId + "/cancel",
                "{}",
                "trace-p3-other-subscription-cancel",
                "Bearer " + otherCandidate.token(),
                "idem-p3-other-subscription-001");
        assertError(otherCancelsSubscription, 404, "NOT_FOUND_404", "trace-p3-other-subscription-cancel");

        HttpJsonResponse otherReadsNotification = postJson(
                "/api/candidate/center/notifications/" + notificationId + "/read",
                "{}",
                "trace-p3-other-notification-read",
                "Bearer " + otherCandidate.token(),
                "idem-p3-other-notification-001");
        assertError(otherReadsNotification, 404, "NOT_FOUND_404", "trace-p3-other-notification-read");

        HttpJsonResponse cancelFavorite = postJson(
                "/api/candidate/center/favorites/" + favoriteId + "/cancel",
                "{}",
                "trace-p3-favorite-cancel",
                "Bearer " + candidate.token(),
                "idem-p3-favorite-cancel-001");
        assertSuccess(cancelFavorite, 200, "trace-p3-favorite-cancel");
        assertThat(cancelFavorite.body().at("/data/total").asLong()).isZero();

        HttpJsonResponse companyDenied = getJson(
                "/api/candidate/center/resume",
                "trace-p3-company-denied",
                "Bearer " + registerAndLoginCompany());
        assertError(companyDenied, 403, "AUTHZ_403", "trace-p3-company-denied");

        String operatorToken = login("operator", "operator", "LocalTalent@123456", "trace-p3-operator-login");
        HttpJsonResponse operatorDenied = getJson(
                "/api/candidate/center/resume",
                "trace-p3-operator-denied",
                "Bearer " + operatorToken);
        assertError(operatorDenied, 403, "AUTHZ_403", "trace-p3-operator-denied");

        HttpJsonResponse consent = postJson(
                "/api/consents",
                """
                        {
                          "consent_scope": ["talent_service_area"],
                          "consent_version": "phase3-prompt27",
                          "realname_verified": true,
                          "second_confirmed": true
                        }
                        """,
                "trace-p3-consent",
                "Bearer " + candidate.token(),
                "idem-p3-consent-001");
        assertSuccess(consent, 200, "trace-p3-consent");
        long consentId = consent.body().at("/data/consent_id").asLong();
        HttpJsonResponse visible = getJson(
                "/api/portal/talent-snapshots?city_code=310000&category_code=software&page=1&size=10",
                "trace-p3-portal-visible",
                null);
        assertSuccess(visible, 200, "trace-p3-portal-visible");
        assertThat(visible.body().at("/data/total").asLong()).isEqualTo(1);
        assertNoRawCandidateData(visible);

        HttpJsonResponse revoke = postJson(
                "/api/consents/" + consentId + "/revoke",
                """
                        {
                          "reason": "phase3 closure regression"
                        }
                        """,
                "trace-p3-revoke",
                "Bearer " + candidate.token(),
                "idem-p3-revoke-001");
        assertSuccess(revoke, 200, "trace-p3-revoke");
        HttpJsonResponse hidden = getJson(
                "/api/portal/talent-snapshots?city_code=310000&category_code=software&page=1&size=10",
                "trace-p3-portal-hidden",
                null);
        assertSuccess(hidden, 200, "trace-p3-portal-hidden");
        assertThat(hidden.body().at("/data/total").asLong()).isZero();

        assertThat(countRows("audit_log", "action_type IN ('resume_save','favorite_create','favorite_cancel','subscription_create','notification_read')"))
                .isGreaterThanOrEqualTo(5);
        assertThat(countRows("field_access_log", "biz_type = 'candidate_resume' AND operator_id = " + candidate.userId()))
                .isGreaterThanOrEqualTo(4);
        assertThat(countRows("audit_log", "biz_type = 'candidate_resume_onboarding' AND operator_id = " + candidate.userId()))
                .isGreaterThanOrEqualTo(1);
    }

    @Test
    void overviewShouldFallbackToLegacyResumeWhenOnboardingStateIsMissing() throws Exception {
        CandidateAccount candidate = registerAndLoginCandidate("legacy");
        insertCompleteResume(candidate.userId());

        HttpJsonResponse overview = getJson(
                "/api/candidate/center/overview",
                "trace-p3-closure-legacy-overview",
                "Bearer " + candidate.token());

        assertSuccess(overview, 200, "trace-p3-closure-legacy-overview");
        assertThat(countRows("candidate_resume_onboarding", "candidate_id = " + candidate.userId())).isZero();
        assertThat(overview.body().at("/data/onboarding/onboarding_required").asBoolean()).isFalse();
        assertThat(overview.body().at("/data/onboarding/onboarding_step").asText()).isEqualTo("center");
    }

    @Test
    void candidateShouldUploadDownloadReplaceAndDeletePrivateResumeAttachment() throws Exception {
        CandidateAccount candidate = registerAndLoginCandidate("attachment");
        String resumeBody = resumeBody("附件简历", "附件候选人", "Java");
        HttpJsonResponse savedResume = putJson(
                "/api/candidate/center/resume",
                resumeBody,
                "trace-p3-attachment-resume",
                "Bearer " + candidate.token(),
                "idem-p3-attachment-resume");
        assertSuccess(savedResume, 200, "trace-p3-attachment-resume");

        HttpJsonResponse emptyAttachment = getJson(
                "/api/candidate/center/resume/attachment",
                "trace-p3-attachment-empty",
                "Bearer " + candidate.token());
        assertSuccess(emptyAttachment, 200, "trace-p3-attachment-empty");
        assertThat(emptyAttachment.body().at("/data/has_attachment").asBoolean()).isFalse();

        byte[] firstFile = "%PDF-1.4\nlocaltalent".getBytes();
        HttpJsonResponse uploaded = postMultipart(
                "/api/candidate/center/resume/attachment",
                "resume.pdf",
                "application/pdf",
                firstFile,
                "trace-p3-attachment-upload",
                "Bearer " + candidate.token(),
                "idem-p3-attachment-upload");
        assertSuccess(uploaded, 200, "trace-p3-attachment-upload");
        assertThat(uploaded.body().at("/data/has_attachment").asBoolean()).isTrue();
        assertThat(uploaded.body().at("/data/file_name").asText()).isEqualTo("resume.pdf");
        assertNoRawCandidateData(uploaded);
        assertThat(uploaded.body().toString()).doesNotContain("candidate-resume-attachments");

        byte[] replacementFile = "%PDF-1.4\nreplacement".getBytes();
        HttpJsonResponse replaced = postMultipart(
                "/api/candidate/center/resume/attachment",
                "resume-v2.pdf",
                "application/pdf",
                replacementFile,
                "trace-p3-attachment-replace",
                "Bearer " + candidate.token(),
                "idem-p3-attachment-replace");
        assertSuccess(replaced, 200, "trace-p3-attachment-replace");
        assertThat(replaced.body().at("/data/file_name").asText()).isEqualTo("resume-v2.pdf");

        when(attachmentStorageService.get(anyString())).thenReturn(replacementFile);
        HttpResponse<byte[]> download = httpClient.send(
                HttpRequest.newBuilder(uri("/api/candidate/center/resume/attachment/download"))
                        .header("X-Trace-Id", "trace-p3-attachment-download")
                        .header("Authorization", "Bearer " + candidate.token())
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertThat(download.statusCode()).isEqualTo(200);
        assertThat(download.body()).isEqualTo(replacementFile);
        assertThat(download.headers().firstValue("Content-Disposition")).hasValueSatisfying(value ->
                assertThat(value).contains("resume-v2.pdf"));

        HttpJsonResponse invalid = postMultipart(
                "/api/candidate/center/resume/attachment",
                "unsafe.txt",
                "text/plain",
                "raw".getBytes(),
                "trace-p3-attachment-invalid",
                "Bearer " + candidate.token(),
                "idem-p3-attachment-invalid");
        assertError(invalid, 400, "VALID_400", "trace-p3-attachment-invalid");

        HttpJsonResponse companyDenied = getJson(
                "/api/candidate/center/resume/attachment",
                "trace-p3-company-attachment-denied",
                "Bearer " + registerAndLoginCompany());
        assertError(companyDenied, 403, "AUTHZ_403", "trace-p3-company-attachment-denied");

        HttpJsonResponse deleted = deleteJson(
                "/api/candidate/center/resume/attachment",
                "trace-p3-attachment-delete",
                "Bearer " + candidate.token(),
                "idem-p3-attachment-delete");
        assertSuccess(deleted, 200, "trace-p3-attachment-delete");
        assertThat(deleted.body().at("/data/has_attachment").asBoolean()).isFalse();

        verify(attachmentStorageService).get(anyString());
        assertThat(countRows("audit_log", "action_type IN ('resume_attachment_upload','resume_attachment_delete')"))
                .isGreaterThanOrEqualTo(3);
        assertThat(countRows("field_access_log", "field_name = 'resume_attachment' AND operator_id = " + candidate.userId()))
                .isGreaterThanOrEqualTo(3);
    }

    @Test
    void candidateShouldGenerateApplyAndDismissPrivateRuleBasedResumeAiSuggestions() throws Exception {
        CandidateAccount candidate = registerAndLoginCandidate("ai");
        CandidateAccount otherCandidate = registerAndLoginCandidate("ai-other");
        HttpJsonResponse savedResume = putJson(
                "/api/candidate/center/resume",
                """
                        {
                          "resume_name": "AI 建议简历",
                          "base_profile": {
                            "display_name": "AI 候选人",
                            "city_code": "310000",
                            "category_code": "software",
                            "experience_years": 3,
                            "expected_positions": [],
                            "expected_cities": [],
                            "job_status": ""
                          },
                          "skills": ["Java"],
                          "work_experience": [
                            {
                              "company_name": "本地科技",
                              "position_name": "后端工程师",
                              "start_date": "2022-01",
                              "end_date": "",
                              "ongoing": true,
                              "responsibility": "开发"
                            }
                          ],
                          "education_experience": [],
                          "self_description": ""
                        }
                        """,
                "trace-p3-ai-resume",
                "Bearer " + candidate.token(),
                "idem-p3-ai-resume");
        assertSuccess(savedResume, 200, "trace-p3-ai-resume");

        HttpJsonResponse generated = postJson(
                "/api/candidate/center/resume/ai-suggestions",
                "{}",
                "trace-p3-ai-generate",
                "Bearer " + candidate.token(),
                "idem-p3-ai-generate");
        assertSuccess(generated, 200, "trace-p3-ai-generate");
        assertThat(generated.body().at("/data/suggestion_count").asInt()).isGreaterThanOrEqualTo(3);
        assertThat(generated.body().at("/data/items").isArray()).isTrue();
        assertNoRawCandidateData(generated);
        long firstApplicableId = 0;
        long guidanceId = 0;
        for (JsonNode item : generated.body().at("/data/items")) {
            if (firstApplicableId == 0 && item.at("/can_apply").asBoolean()) {
                firstApplicableId = item.at("/suggestion_id").asLong();
            }
            if (guidanceId == 0 && !item.at("/can_apply").asBoolean()) {
                guidanceId = item.at("/suggestion_id").asLong();
            }
        }
        assertThat(firstApplicableId).isPositive();
        assertThat(guidanceId).isPositive();

        HttpJsonResponse latest = getJson(
                "/api/candidate/center/resume/ai-suggestions/latest",
                "trace-p3-ai-latest",
                "Bearer " + candidate.token());
        assertSuccess(latest, 200, "trace-p3-ai-latest");
        assertThat(latest.body().at("/data/task_id").asLong()).isEqualTo(generated.body().at("/data/task_id").asLong());

        HttpJsonResponse otherApply = postJson(
                "/api/candidate/center/resume/ai-suggestions/" + firstApplicableId + "/apply",
                "{}",
                "trace-p3-ai-other-apply",
                "Bearer " + otherCandidate.token(),
                "idem-p3-ai-other-apply");
        assertError(otherApply, 404, "NOT_FOUND_404", "trace-p3-ai-other-apply");

        HttpJsonResponse applied = postJson(
                "/api/candidate/center/resume/ai-suggestions/" + firstApplicableId + "/apply",
                "{}",
                "trace-p3-ai-apply",
                "Bearer " + candidate.token(),
                "idem-p3-ai-apply");
        assertSuccess(applied, 200, "trace-p3-ai-apply");
        assertThat(applied.body().at("/data/applied_count").asInt()).isEqualTo(1);

        HttpJsonResponse dismissed = postJson(
                "/api/candidate/center/resume/ai-suggestions/" + guidanceId + "/dismiss",
                "{}",
                "trace-p3-ai-dismiss",
                "Bearer " + candidate.token(),
                "idem-p3-ai-dismiss");
        assertSuccess(dismissed, 200, "trace-p3-ai-dismiss");
        assertThat(dismissed.body().at("/data/dismissed_count").asInt()).isEqualTo(1);

        HttpJsonResponse resume = getJson(
                "/api/candidate/center/resume",
                "trace-p3-ai-resume-after",
                "Bearer " + candidate.token());
        assertSuccess(resume, 200, "trace-p3-ai-resume-after");
        assertNoRawCandidateData(resume);
        assertThat(resume.body().at("/data/self_description").asText()
                + resume.body().at("/data/base_profile/summary").asText()
                + resume.body().at("/data/skills").toString())
                .contains("稳定交付");

        HttpJsonResponse companyDenied = postJson(
                "/api/candidate/center/resume/ai-suggestions",
                "{}",
                "trace-p3-company-ai-denied",
                "Bearer " + registerAndLoginCompany(),
                "idem-p3-company-ai-denied");
        assertError(companyDenied, 403, "AUTHZ_403", "trace-p3-company-ai-denied");

        assertThat(countRows("audit_log", "action_type IN ('resume_ai_suggestions_generate','resume_ai_suggestion_apply','resume_ai_suggestion_dismiss')"))
                .isGreaterThanOrEqualTo(3);
        assertThat(countRows("field_access_log", "access_type = 'SELF_AI_ASSIST' AND operator_id = " + candidate.userId()))
                .isGreaterThanOrEqualTo(2);
    }

    private CandidateAccount registerAndLoginCandidate(String label) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "p3-candidate-" + label + "-" + suffix + "@example.com";
        String password = "Candidate@123456";
        HttpJsonResponse register = postJson(
                "/api/auth/register",
                """
                        {
                          "identity_type": "candidate",
                          "email": "%s",
                          "password": "%s",
                          "display_name": "三期求职者"
                        }
                        """.formatted(email, password),
                "trace-p3-register-" + label,
                null,
                null);
        assertSuccess(register, 200, "trace-p3-register-" + label);
        return new CandidateAccount(
                register.body().at("/data/identity/user_id").asLong(),
                login("candidate", email, password, "trace-p3-login-" + label));
    }

    private String registerAndLoginCompany() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "p3-company-" + suffix + "@example.com";
        String password = "Company@123456";
        postJson(
                "/api/auth/register",
                """
                        {
                          "identity_type": "company",
                          "company_name": "P3 企业",
                          "license_no": "P3-C-%s",
                          "user_name": "企业管理员",
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(suffix.substring(0, 16), email, password),
                "trace-p3-company-register",
                null,
                null);
        return login("company", email, password, "trace-p3-company-login");
    }

    private String login(String identityType, String account, String password, String traceId) throws Exception {
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
                null,
                null);
        assertSuccess(response, 200, traceId);
        return response.body().at("/data/access_token").asText();
    }

    private long insertVisibleJob() {
        long companyId = insertWithKey(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO company (company_name, license_no, auth_status, city_code, industry_code, source_system) "
                            + "VALUES ('P3 认证企业', ?, 2, '310000', 'software', 'portal')",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, "P3-J-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            return statement;
        });
        return insertWithKey(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO job_post "
                            + "(company_id, title, category_code, city_code, salary_min, salary_max, job_desc, status, audit_status) "
                            + "VALUES (?, 'P3 Java 工程师', 'software', '310000', 15000, 30000, 'P3 prompt27 public job', 2, 2)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, companyId);
            return statement;
        });
    }

    private void insertApplication(long candidateId, long jobId, long resumeId) {
        jdbcTemplate.update(
                "INSERT INTO job_application (job_id, candidate_id, resume_id, source_type, application_status) "
                        + "VALUES (?, ?, ?, 1, 2)",
                jobId,
                candidateId,
                resumeId);
    }

    private String resumeBody(String resumeName, String displayName, String skills) {
        return """
                {
                  "resume_name": "%s",
                  "base_profile": {
                    "display_name": "%s",
                    "city_code": "310000",
                    "category_code": "software",
                    "experience_years": 5,
                    "summary": "本人私有摘要"
                  },
                  "education": ["本科"],
                  "experience": ["后端服务建设"],
                  "skills": ["%s"]
                }
                """.formatted(resumeName, displayName, skills);
    }

    private String basicResumeBody(String resumeName, String displayName) {
        return """
                {
                  "resume_name": "%s",
                  "base_profile": {
                    "display_name": "%s",
                    "city_code": "310000",
                    "category_code": "software",
                    "contact_phone": "13900001234",
                    "expected_positions": ["Java工程师"],
                    "expected_cities": ["上海"]
                  },
                  "education": [],
                  "experience": [],
                  "skills": []
                }
                """.formatted(resumeName, displayName);
    }

    private void insertCompleteResume(long candidateId) {
        jdbcTemplate.update(
                "INSERT INTO candidate_resume "
                        + "(candidate_id, resume_name, base_profile_json, education_json, experience_json, "
                        + "skills_json, attachment_object_key, status) VALUES (?, 'legacy resume', ?, ?, ?, ?, ?, 1)",
                candidateId,
                "{\"display_name\":\"历史候选人\",\"city_code\":\"310000\",\"category_code\":\"software\"}",
                "[\"本科\"]",
                "[\"后端工程师\"]",
                "[\"Java\"]",
                "private/legacy.pdf");
    }

    private String onboardingValue(long candidateId, String columnName) {
        return jdbcTemplate.queryForObject(
                "SELECT " + columnName + " FROM candidate_resume_onboarding WHERE candidate_id = ?",
                String.class,
                candidateId);
    }

    private long insertWithKey(StatementFactory statementFactory) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> statementFactory.create(connection), keyHolder);
        Number key = keyHolder.getKey();
        assertThat(key).isNotNull();
        return key.longValue();
    }

    private int countRows(String tableName, String where) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName + " WHERE " + where, Integer.class);
        return count == null ? 0 : count;
    }

    private HttpJsonResponse postJson(
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

    private HttpJsonResponse putJson(
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

    private HttpJsonResponse getJson(String path, String traceId, String authorization) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("X-Trace-Id", traceId)
                .GET();
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        return send(builder.build());
    }

    private HttpJsonResponse deleteJson(
            String path,
            String traceId,
            String authorization,
            String idempotencyKey
    ) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("X-Trace-Id", traceId)
                .DELETE();
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        if (idempotencyKey != null) {
            builder.header("X-Idempotency-Key", idempotencyKey);
        }
        return send(builder.build());
    }

    private HttpJsonResponse postMultipart(
            String path,
            String fileName,
            String contentType,
            byte[] content,
            String traceId,
            String authorization,
            String idempotencyKey
    ) throws Exception {
        String boundary = "----localtalent-" + UUID.randomUUID();
        byte[] prefix = (
                "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                        + "Content-Type: " + contentType + "\r\n\r\n").getBytes();
        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes();
        byte[] body = new byte[prefix.length + content.length + suffix.length];
        System.arraycopy(prefix, 0, body, 0, prefix.length);
        System.arraycopy(content, 0, body, prefix.length, content.length);
        System.arraycopy(suffix, 0, body, prefix.length + content.length, suffix.length);
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("X-Trace-Id", traceId)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        if (idempotencyKey != null) {
            builder.header("X-Idempotency-Key", idempotencyKey);
        }
        return send(builder.build());
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

    private void assertSuccess(HttpJsonResponse response, int expectedStatus, String traceId) {
        assertThat(response.status())
                .withFailMessage("expected success response but got status %s and body %s", response.status(), response.body())
                .isEqualTo(expectedStatus);
        assertThat(response.traceId()).isEqualTo(traceId);
        assertThat(response.body().at("/code").asText()).isEqualTo("0");
        assertThat(response.body().at("/trace_id").asText()).isEqualTo(traceId);
    }

    private void assertError(HttpJsonResponse response, int expectedStatus, String code, String traceId) {
        assertThat(response.status()).isEqualTo(expectedStatus);
        assertThat(response.traceId()).isEqualTo(traceId);
        assertThat(response.body().at("/code").asText()).isEqualTo(code);
        assertThat(response.body().at("/trace_id").asText()).isEqualTo(traceId);
    }

    private void assertNoRawCandidateData(HttpJsonResponse response) {
        assertThat(response.body().toString())
                .doesNotContain("mobile")
                .doesNotContain("email")
                .doesNotContain("password_hash")
                .doesNotContain("resume_body")
                .doesNotContain("attachment_object_key")
                .doesNotContain("evidence")
                .doesNotContain("base_profile_json")
                .doesNotContain("education_json")
                .doesNotContain("experience_json")
                .doesNotContain("skills_json");
    }

    private interface StatementFactory {
        PreparedStatement create(java.sql.Connection connection) throws java.sql.SQLException;
    }

    private record CandidateAccount(long userId, String token) {
    }

    private record HttpJsonResponse(int status, String traceId, JsonNode body) {
    }
}
