package cn.localtalent.backend.portal.recommendation.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public final class PortalRecommendationDtos {

    private PortalRecommendationDtos() {
    }

    public record PortalRecommendationItem(
            @JsonProperty("target_type") String targetType,
            @JsonProperty("target_id") long targetId,
            String title,
            String summary,
            List<String> tags,
            String url,
            @JsonProperty("city_code") String cityCode,
            @JsonProperty("updated_at") LocalDateTime updatedAt,
            @JsonProperty("display_order") int displayOrder
    ) {
    }

    public record PortalRecommendationResponse(
            @JsonProperty("recommendation_list") List<PortalRecommendationItem> recommendationList,
            long total
    ) {
    }
}
