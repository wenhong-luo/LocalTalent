package cn.localtalent.backend.company.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PortalCompanyPageResponse(
        @JsonProperty("company_list") List<PortalCompanyResponse> companyList,
        long total
) {
}
