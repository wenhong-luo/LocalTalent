package cn.localtalent.backend.phase3;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class Phase3SensitiveFieldBlacklistTest {

    private static final String[] BLACKLIST = {
            "mobile",
            "email",
            "resume_body",
            "attachment_object_key",
            "evidence",
            "password_hash",
            "base_profile_json",
            "education_json",
            "experience_json",
            "skills_json",
            "审核材料",
            "营业执照附件",
            "报名名单",
            "签到证据"
    };

    @Test
    void publicPortalRegressionTestsShouldGuardSensitiveFieldBlacklist() throws Exception {
        String portalSnapshots = Files.readString(Path.of("src/test/java/cn/localtalent/backend/candidate/PortalFieldBlacklistTest.java"));
        String companies = Files.readString(Path.of("src/test/java/cn/localtalent/backend/company/PortalCompanyVisibilityIT.java"));
        String jobs = Files.readString(Path.of("src/test/java/cn/localtalent/backend/job/JobVisibilityIT.java"));
        String contents = Files.readString(Path.of("src/test/java/cn/localtalent/backend/portal/PortalContentEventVisibilityIT.java"));
        String recommendations = Files.readString(Path.of("src/test/java/cn/localtalent/backend/admin/AdminPortalOpsFlowIT.java"));

        String publicPortalRegressionSources = portalSnapshots + companies + jobs + contents + recommendations;
        for (String field : BLACKLIST) {
            assertThat(publicPortalRegressionSources).contains(field);
        }
        assertThat(portalSnapshots)
                .contains("candidate_publish_snapshot")
                .contains("display_name_masked")
                .contains("skills_summary")
                .contains("experience_years")
                .contains("doesNotContain(\"mobile\")")
                .contains("doesNotContain(\"email\")")
                .contains("doesNotContain(\"resume_body\")")
                .contains("doesNotContain(\"attachment_object_key\")")
                .contains("doesNotContain(\"base_profile_json\")");
        assertThat(companies)
                .contains("license_no")
                .contains("mobile")
                .contains("email")
                .contains("营业执照附件");
        assertThat(jobs)
                .contains("未认证")
                .contains("audit_status")
                .contains("status");
        assertThat(contents)
                .contains("activity_registration")
                .contains("签到证据")
                .contains("script");
        assertThat(recommendations)
                .contains("assertPublicBodySafe")
                .contains("报名名单")
                .contains("签到证据");
    }
}
