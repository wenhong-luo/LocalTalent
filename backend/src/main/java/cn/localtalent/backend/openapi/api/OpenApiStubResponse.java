package cn.localtalent.backend.openapi.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OpenApiStubResponse(
        boolean accepted,
        boolean stub,
        @JsonProperty("api_code") String apiCode,
        @JsonProperty("sync_status") String syncStatus,
        @JsonProperty("task_id") Long taskId,
        @JsonProperty("trace_id") String traceId
) {
}
