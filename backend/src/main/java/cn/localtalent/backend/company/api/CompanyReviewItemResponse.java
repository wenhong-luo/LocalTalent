package cn.localtalent.backend.company.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record CompanyReviewItemResponse(
        @JsonProperty("company_id") long companyId,
        @JsonProperty("company_name") String companyName,
        @JsonProperty("license_no") String licenseNo,
        @JsonProperty("industry_code") String industryCode,
        @JsonProperty("city_code") String cityCode,
        @JsonProperty("auth_status") int authStatus,
        @JsonProperty("reject_reason") String rejectReason,
        @JsonProperty("submitted_at") LocalDateTime submittedAt
) {
}
