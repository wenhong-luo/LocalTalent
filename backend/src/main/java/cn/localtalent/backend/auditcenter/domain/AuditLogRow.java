package cn.localtalent.backend.auditcenter.domain;

import java.time.LocalDateTime;

public record AuditLogRow(
        long id,
        Long operatorId,
        String operatorRole,
        String bizType,
        Long bizId,
        String actionType,
        String beforeJson,
        String afterJson,
        String traceId,
        LocalDateTime createdAt
) {
}
