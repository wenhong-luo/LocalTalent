package cn.localtalent.backend.company.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record PortalCompanyResponse(
        @JsonProperty("company_id") long companyId,
        @JsonProperty("company_name") String companyName,
        @JsonProperty("city_code") String cityCode,
        @JsonProperty("industry_code") String industryCode,
        @JsonProperty("nature_code") String natureCode,
        @JsonProperty("scale_code") String scaleCode,
        @JsonProperty("company_verified") boolean companyVerified,
        @JsonProperty("company_profile") String companyProfile,
        @JsonProperty("open_job_count") long openJobCount,
        @JsonProperty("updated_at") LocalDateTime updatedAt,
        @JsonProperty("open_jobs") List<PortalCompanyJobResponse> openJobs
) {
}
