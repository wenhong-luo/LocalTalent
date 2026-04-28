package cn.localtalent.backend.auditcenter.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record AuditLogResponse(
        long id,
        @JsonProperty("operator_id") Long operatorId,
        @JsonProperty("operator_role") String operatorRole,
        @JsonProperty("biz_type") String bizType,
        @JsonProperty("biz_id") Long bizId,
        @JsonProperty("action_type") String actionType,
        @JsonProperty("before_json") String beforeJson,
        @JsonProperty("after_json") String afterJson,
        @JsonProperty("trace_id") String traceId,
        @JsonProperty("created_at") LocalDateTime createdAt
) {
}
