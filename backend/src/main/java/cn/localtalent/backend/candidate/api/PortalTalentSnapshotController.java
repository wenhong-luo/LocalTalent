package cn.localtalent.backend.candidate.api;

import cn.localtalent.backend.authz.PublicEndpoint;
import cn.localtalent.backend.candidate.application.PortalTalentSnapshotService;
import cn.localtalent.backend.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portal")
public class PortalTalentSnapshotController {

    private final PortalTalentSnapshotService snapshotService;

    public PortalTalentSnapshotController(PortalTalentSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @PublicEndpoint
    @GetMapping("/talent-snapshots")
    public ApiResponse<PortalTalentSnapshotPageResponse> list(
            @RequestParam(value = "city_code", required = false) String cityCode,
            @RequestParam(value = "category_code", required = false) String categoryCode,
            @RequestParam(value = "experience_min", required = false) Integer experienceMin,
            @RequestParam(value = "experience_max", required = false) Integer experienceMax,
            @RequestParam(value = "updated_within", required = false) String updatedWithin,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ApiResponse.success(snapshotService.list(
                cityCode,
                categoryCode,
                experienceMin,
                experienceMax,
                updatedWithin,
                sort,
                page,
                size));
    }
}
