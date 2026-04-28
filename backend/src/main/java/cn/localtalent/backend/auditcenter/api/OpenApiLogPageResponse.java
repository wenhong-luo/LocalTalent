package cn.localtalent.backend.auditcenter.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record OpenApiLogPageResponse(
        @JsonProperty("open_api_log_list") List<OpenApiLogResponse> openApiLogList,
        long total
) {
}
