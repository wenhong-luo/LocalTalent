package cn.localtalent.backend.auditcenter.domain;

import java.time.LocalDateTime;

public record OpenApiLogRow(
        long id,
        String sourceSystem,
        String clientCode,
        String apiCode,
        String traceId,
        String requestUri,
        String requestMethod,
        String bizType,
        Long bizId,
        Integer httpStatus,
        Boolean successFlag,
        String requestHash,
        String idempotencyKey,
        Long durationMs,
        String requestSummaryJson,
        String responseSummaryJson,
        LocalDateTime requestTime,
        LocalDateTime responseTime,
        String errorMsg
) {
}
