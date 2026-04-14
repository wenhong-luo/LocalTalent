package cn.localtalent.backend.common.api;

import cn.localtalent.backend.common.trace.TraceIdContext;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        String code,
        String message,
        @JsonProperty("trace_id") String traceId,
        T data
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("0", "success", TraceIdContext.getCurrentTraceId(), data);
    }

    public static <T> ApiResponse<T> error(String code, String message, T data) {
        return new ApiResponse<>(code, message, TraceIdContext.getCurrentTraceId(), data);
    }
}
