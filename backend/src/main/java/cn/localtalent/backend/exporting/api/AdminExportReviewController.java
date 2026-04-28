package cn.localtalent.backend.exporting.api;

import cn.localtalent.backend.authz.RequirePermission;
import cn.localtalent.backend.common.api.ApiResponse;
import cn.localtalent.backend.exporting.service.ExportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/exports/review")
public class AdminExportReviewController {

    private final ExportService exportService;

    public AdminExportReviewController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping
    @RequirePermission("admin.export.review.read")
    public ApiResponse<ExportApplyPageResponse> list(
            @RequestParam(value = "approve_status", required = false) Integer approveStatus,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ApiResponse.success(exportService.reviewList(approveStatus, page, size));
    }

    @GetMapping("/{exportId}")
    @RequirePermission("admin.export.review.read")
    public ApiResponse<ExportApplyResponse> detail(@PathVariable("exportId") long exportId) {
        return ApiResponse.success(exportService.reviewDetail(exportId));
    }

    @PostMapping
    @RequirePermission("admin.export.review.write")
    public ApiResponse<ExportApplyResponse> review(@RequestBody ExportReviewRequest request) {
        return ApiResponse.success(exportService.review(request));
    }
}
