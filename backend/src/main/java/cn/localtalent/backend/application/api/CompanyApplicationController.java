package cn.localtalent.backend.application.api;

import cn.localtalent.backend.application.service.ApplicationService;
import cn.localtalent.backend.authz.RequirePermission;
import cn.localtalent.backend.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/company/applications")
public class CompanyApplicationController {

    private final ApplicationService applicationService;

    public CompanyApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    @RequirePermission("company.application.list")
    public ApiResponse<ApplicationPageResponse> list(
            @RequestParam(value = "job_id", required = false) Long jobId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ApiResponse.success(applicationService.listCompanyApplications(jobId, status, page, size));
    }

    @GetMapping("/{id}")
    @RequirePermission("company.application.read")
    public ApiResponse<ApplicationResponse> get(@PathVariable long id) {
        return ApiResponse.success(applicationService.getCompanyApplication(id));
    }
}
