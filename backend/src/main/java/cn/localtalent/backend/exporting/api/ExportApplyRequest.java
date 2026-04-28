package cn.localtalent.backend.exporting.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExportApplyRequest(
        @JsonProperty("biz_type") String bizType,
        ExportScopeRequest scope,
        String reason
) {
}
