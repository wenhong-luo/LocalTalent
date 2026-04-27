package cn.localtalent.backend.job.api;

import cn.localtalent.backend.authz.RequirePermission;
import cn.localtalent.backend.common.api.ApiResponse;
import cn.localtalent.backend.job.application.JobService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/company/jobs")
public class CompanyJobController {

    private final JobService jobService;

    public CompanyJobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    @RequirePermission("company.job.list")
    public ApiResponse<JobPageResponse> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ApiResponse.success(jobService.listCompanyJobs(page, size));
    }

    @PostMapping
    @RequirePermission("company.job.create")
    public ApiResponse<JobResponse> create(@RequestBody JobCreateRequest request) {
        return ApiResponse.success(jobService.create(request));
    }

    @GetMapping("/{id}")
    @RequirePermission("company.job.read")
    public ApiResponse<JobResponse> get(@PathVariable long id) {
        return ApiResponse.success(jobService.getCompanyJob(id));
    }

    @PutMapping("/{id}")
    @RequirePermission("company.job.update")
    public ApiResponse<JobResponse> update(@PathVariable long id, @RequestBody JobUpdateRequest request) {
        return ApiResponse.success(jobService.update(id, request));
    }

    @PostMapping("/{id}/status")
    @RequirePermission("company.job.status")
    public ApiResponse<JobReviewResultResponse> changeStatus(
            @PathVariable long id,
            @RequestBody JobStatusRequest request
    ) {
        return ApiResponse.success(jobService.changeCompanyStatus(id, request));
    }
}
