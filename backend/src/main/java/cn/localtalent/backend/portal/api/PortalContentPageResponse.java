package cn.localtalent.backend.portal.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PortalContentPageResponse(
        @JsonProperty("content_list") List<PortalContentResponse> contentList,
        long total
) {
}
