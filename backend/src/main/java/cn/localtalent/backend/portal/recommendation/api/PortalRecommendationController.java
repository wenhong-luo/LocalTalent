package cn.localtalent.backend.portal.recommendation.api;

import cn.localtalent.backend.authz.PublicEndpoint;
import cn.localtalent.backend.common.api.ApiResponse;
import cn.localtalent.backend.portal.recommendation.api.PortalRecommendationDtos.PortalRecommendationResponse;
import cn.localtalent.backend.portal.recommendation.application.PortalRecommendationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PortalRecommendationController {

    private final PortalRecommendationService service;

    public PortalRecommendationController(PortalRecommendationService service) {
        this.service = service;
    }

    @GetMapping("/api/portal/recommendations")
    @PublicEndpoint
    public ApiResponse<PortalRecommendationResponse> recommendations(
            @RequestParam(value = "slot_code", required = false) String slotCode,
            @RequestParam(value = "limit", defaultValue = "8") int limit
    ) {
        return ApiResponse.success(service.list(slotCode, limit));
    }
}
