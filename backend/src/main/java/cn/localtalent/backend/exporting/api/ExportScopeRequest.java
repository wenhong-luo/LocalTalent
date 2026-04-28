package cn.localtalent.backend.exporting.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExportScopeRequest(
        @JsonProperty("job_id") Long jobId,
        Integer status
) {
}
