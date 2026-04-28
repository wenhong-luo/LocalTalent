package cn.localtalent.backend.auditcenter.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record FieldAccessLogPageResponse(
        @JsonProperty("access_log_list") List<FieldAccessLogResponse> accessLogList,
        long total
) {
}
