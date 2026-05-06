package cn.localtalent.backend.portal.recommendation.application;

import cn.localtalent.backend.admin.ops.infrastructure.AdminPortalOpsJdbcRepository;
import cn.localtalent.backend.phase3.Phase3FeatureProperties;
import cn.localtalent.backend.portal.recommendation.api.PortalRecommendationDtos.PortalRecommendationItem;
import cn.localtalent.backend.portal.recommendation.api.PortalRecommendationDtos.PortalRecommendationResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PortalRecommendationService {

    private static final int MAX_LIMIT = 24;

    private final AdminPortalOpsJdbcRepository repository;
    private final Phase3FeatureProperties phase3Features;

    public PortalRecommendationService(AdminPortalOpsJdbcRepository repository, Phase3FeatureProperties phase3Features) {
        this.repository = repository;
        this.phase3Features = phase3Features;
    }

    public PortalRecommendationResponse list(String slotCode, int limit) {
        if (!phase3Features.isOperatorPortalOps()) {
            return new PortalRecommendationResponse(List.of(), 0);
        }
        String normalizedSlot = slotCode == null || slotCode.isBlank() ? "home_hot_jobs" : slotCode.trim();
        int normalizedLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        List<PortalRecommendationItem> items = repository.activeRecommendations(normalizedSlot, normalizedLimit)
                .stream()
                .flatMap(recommendation -> repository.resolveTargetCard(recommendation).stream())
                .toList();
        return new PortalRecommendationResponse(items, items.size());
    }
}
