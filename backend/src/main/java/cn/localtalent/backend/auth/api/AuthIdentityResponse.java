package cn.localtalent.backend.auth.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthIdentityResponse(
        @JsonProperty("identity_type") String identityType,
        @JsonProperty("user_id") long userId,
        @JsonProperty("company_id") Long companyId,
        @JsonProperty("display_name") String displayName,
        Integer status
) {
}
