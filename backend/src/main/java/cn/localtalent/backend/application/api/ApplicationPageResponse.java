package cn.localtalent.backend.application.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ApplicationPageResponse(
        @JsonProperty("application_list") List<ApplicationResponse> applicationList,
        long total
) {
}
