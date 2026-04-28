package cn.localtalent.backend.openapi;

import cn.localtalent.backend.common.trace.TraceIdContext;
import cn.localtalent.backend.openapi.api.OpenApiMappingResponse;
import cn.localtalent.backend.openapi.api.OpenApiStubResponse;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenApiStubService {

    private final OpenApiIdempotencyService idempotencyService;
    private final OpenApiJdbcRepository repository;

    public OpenApiStubService(OpenApiIdempotencyService idempotencyService, OpenApiJdbcRepository repository) {
        this.idempotencyService = idempotencyService;
        this.repository = repository;
    }

    public OpenApiStubResponse handleWrite(Map<String, Object> payload) {
        return idempotencyService.execute(OpenApiStubResponse.class, () -> {
            OpenApiRequestContext context = OpenApiContext.current();
            if ("retry".equals(String.valueOf(payload.get("stub_mode")))) {
                long taskId = repository.createRetryTask(
                        TraceIdContext.getCurrentTraceId(),
                        context.apiCode(),
                        context.openApiLogId(),
                        context.client(),
                        context.requestHash(),
                        "stub downstream failure queued for retry");
                OpenApiStubResponse response = new OpenApiStubResponse(
                        true,
                        true,
                        context.apiCode(),
                        "retry_queued",
                        taskId,
                        TraceIdContext.getCurrentTraceId());
                return new OpenApiIdempotentResult<>(response, "integration_sync_task", taskId);
            }

            OpenApiStubResponse response = new OpenApiStubResponse(
                    true,
                    true,
                    context.apiCode(),
                    "accepted",
                    null,
                    TraceIdContext.getCurrentTraceId());
            return new OpenApiIdempotentResult<>(response, "open_api_log", context.openApiLogId());
        });
    }

    public OpenApiMappingResponse queryMapping(String sourceSystem, String localBizType, Long localId) {
        return new OpenApiMappingResponse(
                false,
                true,
                sourceSystem,
                localBizType,
                localId,
                null,
                TraceIdContext.getCurrentTraceId());
    }
}
