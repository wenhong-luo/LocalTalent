package cn.localtalent.backend.exporting.api;

import cn.localtalent.backend.authz.RequirePermission;
import cn.localtalent.backend.common.api.ApiResponse;
import cn.localtalent.backend.exporting.service.ExportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/company/exports")
public class CompanyExportController {

    private final ExportService exportService;

    public CompanyExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @PostMapping
    @RequirePermission("company.export.apply")
    public ApiResponse<ExportApplyResponse> apply(@RequestBody ExportApplyRequest request) {
        return ApiResponse.success(exportService.apply(request));
    }

    @GetMapping("/{exportId}")
    @RequirePermission("company.export.read")
    public ApiResponse<ExportApplyResponse> detail(@PathVariable("exportId") long exportId) {
        return ApiResponse.success(exportService.companyDetail(exportId));
    }

    @GetMapping("/{exportId}/download-url")
    @RequirePermission("company.export.download")
    public ApiResponse<ExportDownloadUrlResponse> downloadUrl(@PathVariable("exportId") long exportId) {
        return ApiResponse.success(exportService.issueDownloadUrl(exportId));
    }
}
