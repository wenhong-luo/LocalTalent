package cn.localtalent.backend.auditcenter.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AuditLogPageResponse(
        @JsonProperty("audit_log_list") List<AuditLogResponse> auditLogList,
        long total
) {
}
