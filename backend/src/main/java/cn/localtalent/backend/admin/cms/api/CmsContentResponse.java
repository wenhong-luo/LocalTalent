package cn.localtalent.backend.admin.cms.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record CmsContentResponse(
        @JsonProperty("content_id") long contentId,
        @JsonProperty("content_type") String contentType,
        String title,
        @JsonProperty("cover_url") String coverUrl,
        String summary,
        @JsonProperty("body_html") String bodyHtml,
        @JsonProperty("city_code") String cityCode,
        int status,
        @JsonProperty("publish_time") LocalDateTime publishTime,
        @JsonProperty("updated_at") LocalDateTime updatedAt
) {
}
