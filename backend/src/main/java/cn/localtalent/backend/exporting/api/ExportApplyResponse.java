package cn.localtalent.backend.exporting.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record ExportApplyResponse(
        @JsonProperty("export_id") long exportId,
        @JsonProperty("biz_type") String bizType,
        @JsonProperty("approve_status") int approveStatus,
        @JsonProperty("generate_status") int generateStatus,
        String reason,
        @JsonProperty("reject_reason") String rejectReason,
        @JsonProperty("approve_time") LocalDateTime approveTime,
        @JsonProperty("generated_at") LocalDateTime generatedAt,
        @JsonProperty("expire_time") LocalDateTime expireTime,
        @JsonProperty("file_size_bytes") Long fileSizeBytes,
        @JsonProperty("download_issued_at") LocalDateTime downloadIssuedAt,
        @JsonProperty("download_count") int downloadCount,
        @JsonProperty("error_msg") String errorMsg,
        @JsonProperty("created_at") LocalDateTime createdAt
) {
}
