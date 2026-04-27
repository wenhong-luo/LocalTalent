package cn.localtalent.backend.candidate.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConsentRevokeResponse(
        @JsonProperty("consent_id") long consentId,
        @JsonProperty("revoke_status") int revokeStatus,
        @JsonProperty("publishable_flag") int publishableFlag
) {
}
