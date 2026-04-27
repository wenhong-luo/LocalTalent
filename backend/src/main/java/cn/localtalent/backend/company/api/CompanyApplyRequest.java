package cn.localtalent.backend.company.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CompanyApplyRequest(
        @JsonProperty("company_name") String companyName,
        @JsonProperty("license_no") String licenseNo,
        @JsonProperty("industry_code") String industryCode,
        @JsonProperty("scale_code") String scaleCode,
        @JsonProperty("city_code") String cityCode,
        String address,
        @JsonProperty("company_profile") String companyProfile
) {
}
