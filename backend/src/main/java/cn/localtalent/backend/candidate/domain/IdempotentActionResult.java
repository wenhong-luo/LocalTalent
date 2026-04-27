package cn.localtalent.backend.candidate.domain;

public record IdempotentActionResult<T>(
        T response,
        String resourceType,
        long resourceId
) {
}
