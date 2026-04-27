package cn.localtalent.backend.job.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JobReviewRequest(
        @JsonProperty("job_id") Long jobId,
        @JsonProperty("audit_status") Integer auditStatus,
        String memo
) {
}
