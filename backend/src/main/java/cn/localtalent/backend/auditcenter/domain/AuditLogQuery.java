package cn.localtalent.backend.auditcenter.domain;

import java.time.LocalDateTime;

public record AuditLogQuery(
        String traceId,
        String bizType,
        Long bizId,
        String actionType,
        Long operatorId,
        String operatorRole,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
