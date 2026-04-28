package cn.localtalent.backend.auditcenter.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record FieldAccessLogResponse(
        long id,
        @JsonProperty("operator_id") Long operatorId,
        @JsonProperty("operator_role") String operatorRole,
        @JsonProperty("biz_type") String bizType,
        @JsonProperty("biz_id") Long bizId,
        @JsonProperty("field_name") String fieldName,
        @JsonProperty("access_type") String accessType,
        @JsonProperty("trace_id") String traceId,
        @JsonProperty("created_at") LocalDateTime createdAt
) {
}
