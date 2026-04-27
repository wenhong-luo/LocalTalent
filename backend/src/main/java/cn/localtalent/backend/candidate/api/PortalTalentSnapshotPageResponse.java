package cn.localtalent.backend.candidate.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PortalTalentSnapshotPageResponse(
        @JsonProperty("snapshot_list") List<PortalTalentSnapshotItemResponse> snapshotList,
        long total,
        int page,
        int size
) {
}
