package cn.localtalent.backend.admin.cms.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CmsContentRequest(
        @JsonProperty("content_type") String contentType,
        String title,
        @JsonProperty("cover_url") String coverUrl,
        String summary,
        @JsonProperty("body_html") String bodyHtml,
        @JsonProperty("city_code") String cityCode,
        Integer status
) {
}
