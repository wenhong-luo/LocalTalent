package cn.localtalent.backend.exporting.service;

import cn.localtalent.backend.authz.AuthzPrincipal;
import cn.localtalent.backend.authz.FieldPolicyEngine;
import cn.localtalent.backend.authz.FieldSensitivity;
import cn.localtalent.backend.authz.ProtectedField;
import cn.localtalent.backend.exporting.domain.ExportCandidateRow;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ExportCsvGenerator {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final FieldPolicyEngine fieldPolicyEngine;

    public ExportCsvGenerator(FieldPolicyEngine fieldPolicyEngine) {
        this.fieldPolicyEngine = fieldPolicyEngine;
    }

    public byte[] generate(AuthzPrincipal principal, List<ExportCandidateRow> rows) {
        StringBuilder csv = new StringBuilder();
        csv.append("application_id,job_title,application_status,apply_time,display_name,mobile,email,skills_summary\n");
        for (ExportCandidateRow row : rows) {
            Map<String, Object> fields = fieldPolicyEngine.apply(principal, "export_application", row.applicationId(), List.of(
                    new ProtectedField("application_id", row.applicationId(), FieldSensitivity.S2),
                    new ProtectedField("job_title", row.jobTitle(), FieldSensitivity.S1),
                    new ProtectedField("application_status", row.applicationStatus(), FieldSensitivity.S2),
                    new ProtectedField("apply_time", row.applyTime().format(DATE_TIME_FORMAT), FieldSensitivity.S2),
                    new ProtectedField("display_name", row.displayName(), FieldSensitivity.S3),
                    new ProtectedField("mobile", row.contactMobile(), FieldSensitivity.S3),
                    new ProtectedField("email", row.contactEmail(), FieldSensitivity.S3),
                    new ProtectedField("skills_summary", row.skillsSummary(), FieldSensitivity.S3)));
            csv.append(csv(fields.get("application_id"))).append(',')
                    .append(csv(fields.get("job_title"))).append(',')
                    .append(csv(fields.get("application_status"))).append(',')
                    .append(csv(fields.get("apply_time"))).append(',')
                    .append(csv(fields.get("display_name"))).append(',')
                    .append(csv(fields.get("mobile"))).append(',')
                    .append(csv(fields.get("email"))).append(',')
                    .append(csv(fields.get("skills_summary"))).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.contains("\"") || text.contains(",") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
