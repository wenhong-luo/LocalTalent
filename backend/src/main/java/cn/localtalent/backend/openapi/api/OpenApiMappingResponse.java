package cn.localtalent.backend.openapi.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OpenApiMappingResponse(
        boolean found,
        boolean stub,
        @JsonProperty("source_system") String sourceSystem,
        @JsonProperty("local_biz_type") String localBizType,
        @JsonProperty("local_id") Long localId,
        @JsonProperty("external_id") String externalId,
        @JsonProperty("trace_id") String traceId
) {
}
