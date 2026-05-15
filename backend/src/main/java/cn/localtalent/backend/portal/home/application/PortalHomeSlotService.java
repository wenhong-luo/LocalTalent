package cn.localtalent.backend.portal.home.application;

import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.HomeSlotResponse;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.HomeSlotImageDownload;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.RecommendationResponse;
import cn.localtalent.backend.admin.ops.infrastructure.AdminPortalOpsJdbcRepository;
import cn.localtalent.backend.common.exception.ApiException;
import cn.localtalent.backend.company.workbench.infrastructure.CompanyStyleImageStorageService;
import cn.localtalent.backend.phase3.Phase3FeatureProperties;
import cn.localtalent.backend.portal.home.api.PortalHomeSlotDtos.HomeSlotListResponse;
import cn.localtalent.backend.portal.home.api.PortalHomeSlotDtos.HomeSlotItem;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PortalHomeSlotService {

    private static final int MAX_LIMIT = 60;

    private final AdminPortalOpsJdbcRepository repository;
    private final Phase3FeatureProperties phase3Features;
    private final CompanyStyleImageStorageService imageStorageService;

    public PortalHomeSlotService(
            AdminPortalOpsJdbcRepository repository,
            Phase3FeatureProperties phase3Features,
            CompanyStyleImageStorageService imageStorageService
    ) {
        this.repository = repository;
        this.phase3Features = phase3Features;
        this.imageStorageService = imageStorageService;
    }

    public HomeSlotListResponse list(String slotCodes, int limit) {
        if (!phase3Features.isOperatorPortalOps()) {
            return new HomeSlotListResponse(List.of(), 0);
        }
        int normalizedLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        List<String> normalizedSlots = normalizeSlotCodes(slotCodes);
        List<HomeSlotItem> items = repository.activeHomeSlots(normalizedSlots, normalizedLimit)
                .stream()
                .flatMap(slot -> toPublicSlot(slot).stream())
                .toList();
        return new HomeSlotListResponse(items, items.size());
    }

    public HomeSlotImageDownload publicImage(long id) {
        if (!phase3Features.isOperatorPortalOps()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "home slot image not found");
        }
        HomeSlotResponse slot = repository.findActiveHomeSlot(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "home slot image not found"));
        if (!slot.hasImage() || slot.imageObjectKey() == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "home slot image not found");
        }
        return new HomeSlotImageDownload(slot.imageFileName(), slot.imageContentType(), imageStorageService.get(slot.imageObjectKey()));
    }

    private List<String> normalizeSlotCodes(String slotCodes) {
        if (slotCodes == null || slotCodes.isBlank()) {
            return List.of();
        }
        return Arrays.stream(slotCodes.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .limit(20)
                .toList();
    }

    private Optional<HomeSlotItem> toPublicSlot(HomeSlotResponse slot) {
        String linkUrl = slot.linkUrl();
        if ("target".equals(slot.linkType())) {
            if (slot.targetType() == null || slot.targetId() == null) {
                return Optional.empty();
            }
            linkUrl = repository.resolveTargetCard(new RecommendationResponse(
                    0,
                    slot.slotCode(),
                    slot.targetType(),
                    slot.targetId(),
                    slot.title(),
                    slot.subtitle(),
                    slot.displayOrder(),
                    1,
                    null,
                    null,
                    true,
                    null,
                    slot.updatedAt()))
                    .map(card -> card.url())
                    .orElse(null);
            if (linkUrl == null) {
                return Optional.empty();
            }
        }
        String imageUrl = slot.hasImage() ? "/api/portal/home-slots/" + slot.slotId() + "/image" : slot.imageUrl();
        return Optional.of(new HomeSlotItem(
                slot.slotId(),
                slot.slotCode(),
                slot.title(),
                slot.subtitle(),
                imageUrl,
                slot.imageAlt(),
                slot.linkType(),
                linkUrl,
                slot.displayOrder(),
                slot.updatedAt()));
    }
}
