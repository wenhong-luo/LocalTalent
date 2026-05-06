package cn.localtalent.backend.phase3;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class Phase3IdorBolaMatrixIT {

    @Test
    void roleAndObjectBoundaryRegressionTestsShouldCoverPhase3CriticalDomains() throws Exception {
        String candidate = Files.readString(Path.of("src/test/java/cn/localtalent/backend/candidatecenter/CandidateClosureFlowIT.java"));
        String company = Files.readString(Path.of("src/test/java/cn/localtalent/backend/company/CompanyWorkbenchFlowIT.java"));
        String admin = Files.readString(Path.of("src/test/java/cn/localtalent/backend/admin/AdminPortalOpsFlowIT.java"));
        String auditor = Files.readString(Path.of("src/test/java/cn/localtalent/backend/admin/AuditorWriteDeniedIT.java"));
        String openApi = Files.readString(Path.of("src/test/java/cn/localtalent/backend/openapi/OpenApiContractTest.java"));

        assertThat(candidate)
                .contains("trace-p3-other-subscription-cancel")
                .contains("trace-p3-other-notification-read")
                .contains("trace-p3-company-denied")
                .contains("trace-p3-operator-denied");
        assertThat(company)
                .contains("trace-p28-cross-company-detail")
                .contains("trace-p28-candidate-denied")
                .contains("field_access_log")
                .contains("COMPANY_APPLICATION_DETAIL");
        assertThat(admin)
                .contains("trace-p29-auditor-rec-denied")
                .contains("trace-p29-risk-auditor-denied")
                .contains("target_valid")
                .contains("trace-p29-public-hidden");
        assertThat(auditor)
                .contains("auditorShouldReadButNotWriteAdminResources")
                .contains("trace-p7-auditor-company-write-denied")
                .contains("trace-p7-auditor-job-write-denied");
        assertThat(openApi)
                .contains("invalidSignature")
                .contains("nonceReplay")
                .contains("OPEN_SENSITIVE_400")
                .contains("IDEMPOTENCY_409");
    }
}
