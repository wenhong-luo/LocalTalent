package cn.localtalent.backend.auditcenter.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record OpenApiLogResponse(
        long id,
        @JsonProperty("source_system") String sourceSystem,
        @JsonProperty("client_code") String clientCode,
        @JsonProperty("api_code") String apiCode,
        @JsonProperty("trace_id") String traceId,
        @JsonProperty("request_uri") String requestUri,
        @JsonProperty("request_method") String requestMethod,
        @JsonProperty("biz_type") String bizType,
        @JsonProperty("biz_id") Long bizId,
        @JsonProperty("http_status") Integer httpStatus,
        @JsonProperty("success_flag") Boolean successFlag,
        @JsonProperty("request_hash") String requestHash,
        @JsonProperty("idempotency_key") String idempotencyKey,
        @JsonProperty("duration_ms") Long durationMs,
        @JsonProperty("request_summary_json") String requestSummaryJson,
        @JsonProperty("response_summary_json") String responseSummaryJson,
        @JsonProperty("request_time") LocalDateTime requestTime,
        @JsonProperty("response_time") LocalDateTime responseTime,
        @JsonProperty("error_msg") String errorMsg
) {
}
