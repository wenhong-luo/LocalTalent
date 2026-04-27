package cn.localtalent.backend.job.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JobUpdateRequest(
        String title,
        @JsonProperty("category_code") String categoryCode,
        @JsonProperty("city_code") String cityCode,
        @JsonProperty("salary_min") Integer salaryMin,
        @JsonProperty("salary_max") Integer salaryMax,
        @JsonProperty("job_desc") String jobDesc
) {
}
