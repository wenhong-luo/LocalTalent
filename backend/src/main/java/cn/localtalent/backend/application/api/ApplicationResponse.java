package cn.localtalent.backend.application.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record ApplicationResponse(
        @JsonProperty("application_id") long applicationId,
        @JsonProperty("job_id") long jobId,
        @JsonProperty("company_id") long companyId,
        @JsonProperty("candidate_id") long candidateId,
        @JsonProperty("resume_id") Long resumeId,
        int status,
        @JsonProperty("apply_time") LocalDateTime applyTime,
        @JsonProperty("job_title") String jobTitle,
        @JsonProperty("company_name") String companyName
) {
}
