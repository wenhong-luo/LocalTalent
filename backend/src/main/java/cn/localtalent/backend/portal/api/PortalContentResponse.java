package cn.localtalent.backend.portal.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record PortalContentResponse(
        @JsonProperty("content_id") long contentId,
        @JsonProperty("content_type") String contentType,
        String title,
        @JsonProperty("cover_url") String coverUrl,
        String summary,
        @JsonProperty("body_text") String bodyText,
        @JsonProperty("city_code") String cityCode,
        @JsonProperty("publish_time") LocalDateTime publishTime,
        @JsonProperty("updated_at") LocalDateTime updatedAt
) {
}
