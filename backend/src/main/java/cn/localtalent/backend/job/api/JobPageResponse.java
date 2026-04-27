package cn.localtalent.backend.job.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record JobPageResponse(
        @JsonProperty("job_list") List<JobResponse> jobList,
        long total
) {
}
