package cn.localtalent.backend.exporting.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExportReviewRequest(
        @JsonProperty("export_id") Long exportId,
        @JsonProperty("approve_status") Integer approveStatus,
        String memo
) {
}
