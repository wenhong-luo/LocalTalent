package cn.localtalent.backend.company.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CompanyStatusResponse(
        @JsonProperty("company_id") long companyId,
        @JsonProperty("auth_status") int authStatus,
        @JsonProperty("reject_reason") String rejectReason
) {
}
