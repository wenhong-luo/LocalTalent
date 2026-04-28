package cn.localtalent.backend.auditcenter;

import static org.assertj.core.api.Assertions.assertThat;

import cn.localtalent.backend.auditcenter.service.AuditCenterMaskingService;
import org.junit.jupiter.api.Test;

class AuditMaskingTest {

    private final AuditCenterMaskingService maskingService = new AuditCenterMaskingService();

    @Test
    void jsonMaskingShouldRemoveSensitivePlaintextRecursively() {
        String sanitized = maskingService.sanitizeJson("""
                {
                  "mobile": "13900001234",
                  "email": "candidate@example.com",
                  "profile": {
                    "resume_body": "raw resume body text",
                    "attachment_object_key": "resume/private/object.pdf",
                    "evidence": {"page_snapshot": "consent evidence html"},
                    "password": "PlainPassword@123",
                    "client_secret": "LocalTalentOpen@123456",
                    "access_token": "token-value"
                  }
                }
                """);

        assertThat(sanitized)
                .doesNotContain("13900001234")
                .doesNotContain("candidate@example.com")
                .doesNotContain("raw resume body text")
                .doesNotContain("resume/private/object.pdf")
                .doesNotContain("consent evidence html")
                .doesNotContain("PlainPassword@123")
                .doesNotContain("LocalTalentOpen@123456")
                .doesNotContain("token-value")
                .contains("1**********")
                .contains("***@example.com")
                .contains("[REDACTED]");
    }

    @Test
    void uriTextAndOpenApiSummaryShouldBeMasked() {
        String uri = maskingService.sanitizeUri(
                "/api/admin/audit-logs?mobile=13900001234&email=candidate@example.com&trace_id=trace-p10");
        assertThat(uri)
                .doesNotContain("13900001234")
                .doesNotContain("candidate@example.com")
                .contains("mobile=[REDACTED]")
                .contains("email=[REDACTED]")
                .contains("trace_id=trace-p10");

        String message = maskingService.sanitizeText(
                "failed with password=Secret123 and token=abc.def for 13900001234 candidate@example.com");
        assertThat(message)
                .doesNotContain("Secret123")
                .doesNotContain("abc.def")
                .doesNotContain("13900001234")
                .doesNotContain("candidate@example.com")
                .contains("[REDACTED]")
                .contains("1**********")
                .contains("***@example.com");

        String summary = maskingService.sanitizeJson("""
                {
                  "request_body": {
                    "mobile": "13900001234",
                    "email": "candidate@example.com",
                    "attachment_object_key": "private-object",
                    "evidence": "raw evidence"
                  },
                  "response": "accepted"
                }
                """);
        assertThat(summary)
                .doesNotContain("13900001234")
                .doesNotContain("candidate@example.com")
                .doesNotContain("private-object")
                .doesNotContain("raw evidence")
                .contains("[REDACTED]");
    }
}
