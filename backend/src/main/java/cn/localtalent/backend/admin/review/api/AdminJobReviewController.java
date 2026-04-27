package cn.localtalent.backend.admin.review.api;

import cn.localtalent.backend.authz.RequirePermission;
import cn.localtalent.backend.common.api.ApiResponse;
import cn.localtalent.backend.job.api.JobPageResponse;
import cn.localtalent.backend.job.api.JobResponse;
import cn.localtalent.backend.job.api.JobReviewRequest;
import cn.localtalent.backend.job.api.JobReviewResultResponse;
import cn.localtalent.backend.job.application.JobService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    @RequirePermission("admin.job.review.read")
    public ApiResponse<JobPageResponse> list(
            @RequestParam(value = "audit_status", required = false) Integer auditStatus,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ApiResponse.success(jobService.listForReview(auditStatus, page, size));
    }

    @GetMapping("/{jobId}")
    @RequirePermission("admin.job.review.read")
    public ApiResponse<JobResponse> detail(@PathVariable("jobId") long jobId) {
        return ApiResponse.success(jobService.getForReview(jobId));
    }

    @PostMapping
    @RequirePermission("admin.job.review.write")
    public ApiResponse<JobReviewResultResponse> review(@RequestBody JobReviewRequest request) {
        return ApiResponse.success(jobService.review(request));
    }
}
