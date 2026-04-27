package cn.localtalent.backend.application.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ApplicationCreateRequest(
        @JsonProperty("job_id") Long jobId,
        @JsonProperty("resume_id") Long resumeId
) {
}
