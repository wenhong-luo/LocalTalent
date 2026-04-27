package cn.localtalent.backend.candidate.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PortalTalentSnapshotItemResponse(
        @JsonProperty("snapshot_id") long snapshotId,
        @JsonProperty("display_name_masked") String displayNameMasked,
        @JsonProperty("city_code") String cityCode,
        @JsonProperty("category_code") String categoryCode,
        @JsonProperty("skills_summary") String skillsSummary,
        @JsonProperty("experience_years") Integer experienceYears,
        @JsonProperty("updated_at") String updatedAt
) {
}
