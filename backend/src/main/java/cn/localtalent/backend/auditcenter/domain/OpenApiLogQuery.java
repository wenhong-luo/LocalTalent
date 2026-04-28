package cn.localtalent.backend.auditcenter.domain;

import java.time.LocalDateTime;

public record OpenApiLogQuery(
        String traceId,
        String clientCode,
        String sourceSystem,
        String apiCode,
        Boolean successFlag,
        Integer httpStatus,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
