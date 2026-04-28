package cn.localtalent.backend.exporting.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record ExportDownloadUrlResponse(
        @JsonProperty("download_url") String downloadUrl,
        @JsonProperty("expire_time") LocalDateTime expireTime
) {
}
