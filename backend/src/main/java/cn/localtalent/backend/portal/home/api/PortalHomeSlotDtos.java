package cn.localtalent.backend.portal.home.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public final class PortalHomeSlotDtos {

    private PortalHomeSlotDtos() {
    }

    public record HomeSlotItem(
            @JsonProperty("slot_id") long slotId,
            @JsonProperty("slot_code") String slotCode,
            String title,
            String subtitle,
            @JsonProperty("image_url") String imageUrl,
            @JsonProperty("image_alt") String imageAlt,
            @JsonProperty("link_type") String linkType,
            @JsonProperty("link_url") String linkUrl,
            @JsonProperty("display_order") int displayOrder,
            @JsonProperty("updated_at") LocalDateTime updatedAt
    ) {
    }

    public record HomeSlotListResponse(
            @JsonProperty("slot_list") List<HomeSlotItem> slotList,
            long total
    ) {
    }
}
