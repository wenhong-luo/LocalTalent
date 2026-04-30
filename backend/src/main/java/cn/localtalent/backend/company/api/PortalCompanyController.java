package cn.localtalent.backend.company.api;

import cn.localtalent.backend.authz.PublicEndpoint;
import cn.localtalent.backend.common.api.ApiResponse;
import cn.localtalent.backend.company.application.PortalCompanyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portal/companies")
public class PortalCompanyController {

    private final PortalCompanyService portalCompanyService;

    public PortalCompanyController(PortalCompanyService portalCompanyService) {
        this.portalCompanyService = portalCompanyService;
    }

    @GetMapping
    @PublicEndpoint
    public ApiResponse<PortalCompanyPageResponse> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "city_code", required = false) String cityCode,
            @RequestParam(value = "industry_code", required = false) String industryCode,
            @RequestParam(value = "nature_code", required = false) String natureCode,
            @RequestParam(value = "scale_code", required = false) String scaleCode,
            @RequestParam(value = "verified", required = false) String verified,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        PortalCompanySearchCriteria criteria = new PortalCompanySearchCriteria(
                keyword,
                cityCode,
                industryCode,
                natureCode,
                scaleCode,
                verified);
        return ApiResponse.success(portalCompanyService.list(criteria, page, size));
    }

    @GetMapping("/{id}")
    @PublicEndpoint
    public ApiResponse<PortalCompanyResponse> get(@PathVariable long id) {
        return ApiResponse.success(portalCompanyService.get(id));
    }
}
