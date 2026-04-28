package cn.localtalent.backend.auditcenter.domain;

import java.time.LocalDateTime;

public record FieldAccessLogQuery(
        String traceId,
        String bizType,
        Long bizId,
        String fieldName,
        String accessType,
        Long operatorId,
        String operatorRole,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
