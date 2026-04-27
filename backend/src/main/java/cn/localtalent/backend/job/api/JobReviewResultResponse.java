package cn.localtalent.backend.job.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JobReviewResultResponse(
        @JsonProperty("job_id") long jobId,
        int status,
        @JsonProperty("audit_status") int auditStatus,
        @JsonProperty("reject_reason") String rejectReason
) {
}
