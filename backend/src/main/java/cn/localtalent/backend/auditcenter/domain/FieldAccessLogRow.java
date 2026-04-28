package cn.localtalent.backend.auditcenter.domain;

import java.time.LocalDateTime;

public record FieldAccessLogRow(
        long id,
        Long operatorId,
        String operatorRole,
        String bizType,
        Long bizId,
        String fieldName,
        String accessType,
        String traceId,
        LocalDateTime createdAt
) {
}
