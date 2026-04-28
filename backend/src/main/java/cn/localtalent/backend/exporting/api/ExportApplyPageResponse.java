package cn.localtalent.backend.exporting.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ExportApplyPageResponse(
        @JsonProperty("export_list") List<ExportApplyResponse> exportList,
        long total
) {
}
