package cn.localtalent.backend.job.api;

import cn.localtalent.backend.authz.PublicEndpoint;
import cn.localtalent.backend.common.api.ApiResponse;
import cn.localtalent.backend.job.application.PortalJobService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portal/jobs")
public class PortalJobController {

    private final PortalJobService portalJobService;

    public PortalJobController(PortalJobService portalJobService) {
        this.portalJobService = portalJobService;
    }

    @GetMapping
    @PublicEndpoint
    public ApiResponse<PortalJobPageResponse> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "city_code", required = false) String cityCode,
            @RequestParam(value = "category_code", required = false) String categoryCode,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ApiResponse.success(portalJobService.list(keyword, cityCode, categoryCode, page, size));
    }

    @GetMapping("/{id}")
    @PublicEndpoint
    public ApiResponse<PortalJobResponse> get(@PathVariable long id) {
        return ApiResponse.success(portalJobService.get(id));
    }
}
