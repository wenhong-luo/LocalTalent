package cn.localtalent.backend.application.api;

import cn.localtalent.backend.application.service.ApplicationService;
import cn.localtalent.backend.authz.RequirePermission;
import cn.localtalent.backend.common.api.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications")
public class CandidateApplicationController {

    private final ApplicationService applicationService;

    public CandidateApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    @RequirePermission("candidate.application.create")
    public ApiResponse<ApplicationResponse> create(@RequestBody ApplicationCreateRequest request) {
        return ApiResponse.success(applicationService.create(request));
    }
}
