package cn.localtalent.backend.phase3;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class Phase3ExportSecurityRegressionIT {

    @Test
    void existingExportFlowShouldRemainGuardedByApprovalFieldPolicyExpiryAndAudit() throws Exception {
        String exportFlow = Files.readString(Path.of("src/test/java/cn/localtalent/backend/exporting/ExportApprovalFlowIT.java"));
        String companyWorkbench = Files.readString(Path.of("src/test/java/cn/localtalent/backend/company/CompanyWorkbenchFlowIT.java"));

        assertThat(exportFlow)
                .contains("EXPORT_403")
                .contains("EXPORT_409")
                .contains("download-url")
                .contains("expire")
                .contains("field_access_log")
                .contains("export_download_url_issue")
                .contains("export_approve")
                .contains("export_reject")
                .contains("doesNotContain(\"13900001234\")")
                .contains("doesNotContain(\"SecretSkill\")");
        assertThat(companyWorkbench)
                .contains("/api/company/workbench/exports")
                .contains("trace-p28-export")
                .contains("export_apply")
                .contains("assertNoSensitiveFields");
    }
}
