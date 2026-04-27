package cn.localtalent.backend.company.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CompanyReviewRequest(
        @JsonProperty("company_id") Long companyId,
        @JsonProperty("audit_status") Integer auditStatus,
        String memo
) {
}
