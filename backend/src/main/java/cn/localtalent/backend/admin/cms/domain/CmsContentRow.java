package cn.localtalent.backend.admin.cms.domain;

import java.time.LocalDateTime;

public record CmsContentRow(
        long contentId,
        String contentType,
        String title,
        String coverUrl,
        String summary,
        String bodyHtml,
        String cityCode,
        int status,
        LocalDateTime publishTime,
        LocalDateTime updatedAt
) {
}
