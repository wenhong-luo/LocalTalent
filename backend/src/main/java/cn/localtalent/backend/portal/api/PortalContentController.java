package cn.localtalent.backend.portal.api;

import cn.localtalent.backend.authz.PublicEndpoint;
import cn.localtalent.backend.common.api.ApiResponse;
import cn.localtalent.backend.portal.application.PortalContentEventService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portal/contents")
public class PortalContentController {

    private final PortalContentEventService service;

    public PortalContentController(PortalContentEventService service) {
        this.service = service;
    }

    @GetMapping
    @PublicEndpoint
    public ApiResponse<PortalContentPageResponse> list(
            @RequestParam(value = "content_type", required = false) String contentType,
            @RequestParam(value = "city_code", required = false) String cityCode,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ApiResponse.success(service.listContents(contentType, cityCode, page, size));
    }

    @GetMapping("/{id}")
    @PublicEndpoint
    public ApiResponse<PortalContentResponse> get(@PathVariable long id) {
        return ApiResponse.success(service.getContent(id));
    }
}
