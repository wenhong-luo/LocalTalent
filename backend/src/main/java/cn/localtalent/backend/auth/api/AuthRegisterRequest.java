package cn.localtalent.backend.auth.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthRegisterRequest(
        @JsonProperty("identity_type") String identityType,
        String mobile,
        String email,
        String password,
        @JsonProperty("display_name") String displayName,
        @JsonProperty("company_name") String companyName,
        @JsonProperty("license_no") String licenseNo,
        @JsonProperty("user_name") String userName
) {

    @Override
    public String toString() {
        return "AuthRegisterRequest[identityType=%s, mobile=%s, email=%s, password=***, displayName=%s, companyName=%s, licenseNo=%s, userName=%s]"
                .formatted(identityType, mobile, email, displayName, companyName, licenseNo, userName);
    }
}
