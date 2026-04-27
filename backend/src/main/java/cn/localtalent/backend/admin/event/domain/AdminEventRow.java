package cn.localtalent.backend.admin.event.domain;

import java.time.LocalDateTime;

public record AdminEventRow(
        long eventId,
        String title,
        String typeCode,
        String cityCode,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String location,
        Long organizerCompanyId,
        int status,
        LocalDateTime updatedAt
) {
}
