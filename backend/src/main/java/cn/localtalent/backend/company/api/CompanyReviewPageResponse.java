package cn.localtalent.backend.company.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CompanyReviewPageResponse(
        @JsonProperty("company_list") List<CompanyReviewItemResponse> companyList,
        long total
) {
}
