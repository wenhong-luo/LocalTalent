package cn.localtalent.backend.portal.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PortalEventPageResponse(
        @JsonProperty("event_list") List<PortalEventResponse> eventList,
        long total
) {
}
