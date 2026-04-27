package cn.localtalent.backend.job.api;

import cn.localtalent.backend.authz.RequirePermission;
import cn.localtalent.backend.common.api.ApiResponse;
import cn.localtalent.backend.job.application.JobService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/jobs/review")
public class AdminJobReviewController {

    private final JobService jobService;

    public AdminJobReviewController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    @RequirePermission("admin.job.review")
    public ApiResponse<JobPageResponse> list(
            @RequestParam(value = "audit_status", required = false) Integer auditStatus,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ApiResponse.success(jobService.listForReview(auditStatus, page, size));
    }

    @PostMapping
    @RequirePermission("admin.job.review")
    public ApiResponse<JobReviewResultResponse> review(@RequestBody JobReviewRequest request) {
        return ApiResponse.success(jobService.review(request));
    }
}
