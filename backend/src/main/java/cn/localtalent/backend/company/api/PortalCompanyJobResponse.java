package cn.localtalent.backend.company.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record PortalCompanyJobResponse(
        @JsonProperty("job_id") long jobId,
        String title,
        @JsonProperty("category_code") String categoryCode,
        @JsonProperty("city_code") String cityCode,
        @JsonProperty("salary_min") Integer salaryMin,
        @JsonProperty("salary_max") Integer salaryMax,
        @JsonProperty("updated_at") LocalDateTime updatedAt
) {
}
