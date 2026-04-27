package cn.localtalent.backend.job.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PortalJobPageResponse(
        @JsonProperty("job_list") List<PortalJobResponse> jobList,
        long total
) {
}
