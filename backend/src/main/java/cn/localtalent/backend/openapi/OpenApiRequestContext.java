package cn.localtalent.backend.openapi;

public record OpenApiRequestContext(
        OpenApiClient client,
        String apiCode,
        String requestHash,
        String idempotencyKey,
        long openApiLogId
) {
}
