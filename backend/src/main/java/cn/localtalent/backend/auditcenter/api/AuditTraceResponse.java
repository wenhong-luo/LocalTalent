package cn.localtalent.backend.auditcenter.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AuditTraceResponse(
        @JsonProperty("trace_id") String traceId,
        @JsonProperty("audit_log_list") List<AuditLogResponse> auditLogList,
        @JsonProperty("access_log_list") List<FieldAccessLogResponse> accessLogList,
        @JsonProperty("open_api_log_list") List<OpenApiLogResponse> openApiLogList
) {
}
