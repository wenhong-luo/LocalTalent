package cn.localtalent.backend.admin.cms.api;

import cn.localtalent.backend.admin.cms.service.CmsContentService;
import cn.localtalent.backend.authz.RequirePermission;
import cn.localtalent.backend.common.api.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/cms/contents")
public class AdminCmsContentController {

    private final CmsContentService cmsContentService;

    public AdminCmsContentController(CmsContentService cmsContentService) {
        this.cmsContentService = cmsContentService;
    }

    @GetMapping
    @RequirePermission("admin.cms.read")
    public ApiResponse<CmsContentPageResponse> list(
            @RequestParam(value = "content_type", required = false) String contentType,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ApiResponse.success(cmsContentService.list(contentType, status, page, size));
    }

    @GetMapping("/{contentId}")
    @RequirePermission("admin.cms.read")
    public ApiResponse<CmsContentResponse> detail(@PathVariable("contentId") long contentId) {
        return ApiResponse.success(cmsContentService.detail(contentId));
    }

    @PostMapping
    @RequirePermission("admin.cms.write")
    public ApiResponse<CmsContentResponse> create(@RequestBody CmsContentRequest request) {
        return ApiResponse.success(cmsContentService.create(request));
    }

    @PutMapping("/{contentId}")
    @RequirePermission("admin.cms.write")
    public ApiResponse<CmsContentResponse> update(
            @PathVariable("contentId") long contentId,
            @RequestBody CmsContentRequest request
    ) {
        return ApiResponse.success(cmsContentService.update(contentId, request));
    }

    @DeleteMapping("/{contentId}")
    @RequirePermission("admin.cms.write")
    public ApiResponse<CmsContentResponse> softDelete(@PathVariable("contentId") long contentId) {
        return ApiResponse.success(cmsContentService.softDelete(contentId));
    }
}
