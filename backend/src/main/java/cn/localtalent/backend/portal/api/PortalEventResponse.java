package cn.localtalent.backend.portal.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record PortalEventResponse(
        @JsonProperty("event_id") long eventId,
        String title,
        @JsonProperty("type_code") String typeCode,
        @JsonProperty("city_code") String cityCode,
        @JsonProperty("start_time") LocalDateTime startTime,
        @JsonProperty("end_time") LocalDateTime endTime,
        String location,
        int status,
        @JsonProperty("updated_at") LocalDateTime updatedAt
) {
}
