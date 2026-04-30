package cn.localtalent.backend.portal.domain;

import java.time.LocalDateTime;

public record PortalEventRow(
        long eventId,
        String title,
        String typeCode,
        String cityCode,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String location,
        int status,
        LocalDateTime updatedAt
) {
}
