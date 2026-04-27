package cn.localtalent.backend.job.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record PortalJobResponse(
        @JsonProperty("job_id") long jobId,
        String title,
        @JsonProperty("company_name") String companyName,
        @JsonProperty("category_code") String categoryCode,
        @JsonProperty("city_code") String cityCode,
        @JsonProperty("salary_min") Integer salaryMin,
        @JsonProperty("salary_max") Integer salaryMax,
        @JsonProperty("job_desc") String jobDesc,
        @JsonProperty("updated_at") LocalDateTime updatedAt
) {
}
