package cn.localtalent.backend.openapi;

public record OpenApiIdempotentResult<T>(
        T response,
        String resourceType,
        long resourceId
) {
}
