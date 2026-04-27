package cn.localtalent.backend.admin.event.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AdminEventPageResponse(
        @JsonProperty("event_list") List<AdminEventResponse> eventList,
        long total
) {
}
