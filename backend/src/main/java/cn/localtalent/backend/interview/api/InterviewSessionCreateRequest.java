package cn.localtalent.backend.interview.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record InterviewSessionCreateRequest(
        @JsonProperty("session_name") String sessionName,
        @JsonProperty("session_time") LocalDateTime sessionTime,
        String location
) {
}
