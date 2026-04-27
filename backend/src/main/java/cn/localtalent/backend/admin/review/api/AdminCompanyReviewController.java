package cn.localtalent.backend.admin.review.api;

import cn.localtalent.backend.authz.RequirePermission;
import cn.localtalent.backend.common.api.ApiResponse;
import cn.localtalent.backend.company.api.CompanyReviewItemResponse;
import cn.localtalent.backend.company.api.CompanyReviewPageResponse;
import cn.localtalent.backend.company.api.CompanyReviewRequest;
import cn.localtalent.backend.company.api.CompanyStatusResponse;
import cn.localtalent.backend.company.application.CompanyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/companies/review")
public class AdminCompanyReviewController {

    private final CompanyService companyService;

    public AdminCompanyReviewController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    @RequirePermission("admin.company.review.read")
    public ApiResponse<CompanyReviewPageResponse> list(
            @RequestParam(value = "auth_status", required = false) Integer authStatus,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ApiResponse.success(companyService.listForReview(authStatus, page, size));
    }

    @GetMapping("/{companyId}")
    @RequirePermission("admin.company.review.read")
    public ApiResponse<CompanyReviewItemResponse> detail(@PathVariable("companyId") long companyId) {
        return ApiResponse.success(companyService.getForReview(companyId));
    }

    @PostMapping
    @RequirePermission("admin.company.review.write")
    public ApiResponse<CompanyStatusResponse> review(@RequestBody CompanyReviewRequest request) {
        return ApiResponse.success(companyService.review(request));
    }
}
