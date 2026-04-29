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
            @RequestParam(value = "salary_min", required = false) Integer salaryMin,
            @RequestParam(value = "salary_max", required = false) Integer salaryMax,
            @RequestParam(value = "industry_code", required = false) String industryCode,
            @RequestParam(value = "scale_code", required = false) String scaleCode,
            @RequestParam(value = "updated_within", required = false) Integer updatedWithin,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        PortalJobSearchCriteria criteria = new PortalJobSearchCriteria(
                keyword,
                cityCode,
                categoryCode,
                salaryMin,
                salaryMax,
                industryCode,
                scaleCode,
                updatedWithin,
                sort);
        return ApiResponse.success(portalJobService.list(criteria, page, size));
    }

    @GetMapping("/{id}")
    @PublicEndpoint
    public ApiResponse<PortalJobResponse> get(@PathVariable long id) {
        return ApiResponse.success(portalJobService.get(id));
    }
}
