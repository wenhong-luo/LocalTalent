package cn.localtalent.backend.interview.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record InterviewSessionResponse(
        @JsonProperty("session_id") long sessionId,
        @JsonProperty("application_id") long applicationId,
        @JsonProperty("job_id") long jobId,
        @JsonProperty("company_id") long companyId,
        int status,
        @JsonProperty("session_name") String sessionName,
        @JsonProperty("session_time") LocalDateTime sessionTime,
        String location
) {
}
