package cn.localtalent.backend.portal.domain;

import java.time.LocalDateTime;

public record PortalContentRow(
        long contentId,
        String contentType,
        String title,
        String coverUrl,
        String summary,
        String bodyHtml,
        String cityCode,
        LocalDateTime publishTime,
        LocalDateTime updatedAt
) {
}
