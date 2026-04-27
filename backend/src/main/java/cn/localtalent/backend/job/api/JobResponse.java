package cn.localtalent.backend.job.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record JobResponse(
        @JsonProperty("job_id") long jobId,
        @JsonProperty("company_id") long companyId,
        String title,
        @JsonProperty("category_code") String categoryCode,
        @JsonProperty("city_code") String cityCode,
        @JsonProperty("salary_min") Integer salaryMin,
        @JsonProperty("salary_max") Integer salaryMax,
        @JsonProperty("job_desc") String jobDesc,
        int status,
        @JsonProperty("audit_status") int auditStatus,
        @JsonProperty("reject_reason") String rejectReason,
        @JsonProperty("updated_at") LocalDateTime updatedAt
) {
}
