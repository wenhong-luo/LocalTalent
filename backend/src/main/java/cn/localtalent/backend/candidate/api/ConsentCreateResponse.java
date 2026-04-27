package cn.localtalent.backend.candidate.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConsentCreateResponse(
        @JsonProperty("consent_id") long consentId,
        @JsonProperty("consent_status") int consentStatus,
        @JsonProperty("snapshot_id") long snapshotId,
        @JsonProperty("publishable_flag") int publishableFlag
) {
}
