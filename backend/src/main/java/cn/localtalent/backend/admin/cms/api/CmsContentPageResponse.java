package cn.localtalent.backend.admin.cms.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CmsContentPageResponse(
        @JsonProperty("content_list") List<CmsContentResponse> contentList,
        long total
) {
}
