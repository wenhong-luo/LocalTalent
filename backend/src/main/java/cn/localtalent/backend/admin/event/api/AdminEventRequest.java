package cn.localtalent.backend.admin.event.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record AdminEventRequest(
        String title,
        @JsonProperty("type_code") String typeCode,
        @JsonProperty("city_code") String cityCode,
        @JsonProperty("start_time") LocalDateTime startTime,
        @JsonProperty("end_time") LocalDateTime endTime,
        String location,
        @JsonProperty("organizer_company_id") Long organizerCompanyId,
        Integer status
) {
}
