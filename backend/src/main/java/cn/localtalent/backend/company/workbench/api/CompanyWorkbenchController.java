package cn.localtalent.backend.company.workbench.api;

import cn.localtalent.backend.authz.RequirePermission;
import cn.localtalent.backend.common.api.ApiResponse;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.ApplicationPageResponse;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.ApplicationStageRequest;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.CertificationSubmitRequest;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.CompanyProfileResponse;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.CompanyProfileSaveRequest;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.InterviewSessionPageResponse;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.OverviewResponse;
import cn.localtalent.backend.company.workbench.application.CompanyWorkbenchService;
import cn.localtalent.backend.exporting.api.ExportApplyRequest;
import cn.localtalent.backend.exporting.api.ExportApplyResponse;
import cn.localtalent.backend.interview.api.InterviewSessionCreateRequest;
import cn.localtalent.backend.interview.api.InterviewSessionResponse;
import cn.localtalent.backend.job.api.JobCreateRequest;
import cn.localtalent.backend.job.api.JobPageResponse;
import cn.localtalent.backend.job.api.JobResponse;
import cn.localtalent.backend.job.api.JobStatusRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/company/workbench")
public class CompanyWorkbenchController {

    private final CompanyWorkbenchService service;

    public CompanyWorkbenchController(CompanyWorkbenchService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    @RequirePermission("company.workbench.read")
    public ApiResponse<OverviewResponse> overview() {
        return ApiResponse.success(service.overview());
    }

    @GetMapping("/profile")
    @RequirePermission("company.profile.read")
    public ApiResponse<CompanyProfileResponse> profile() {
        return ApiResponse.success(service.profile());
    }

    @PutMapping("/profile")
    @RequirePermission("company.profile.write")
    public ApiResponse<CompanyProfileResponse> saveProfile(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CompanyProfileSaveRequest request
    ) {
        return ApiResponse.success(service.saveProfile(request, idempotencyKey));
    }

    @PostMapping("/certification")
    @RequirePermission("company.certification.submit")
    public ApiResponse<CompanyProfileResponse> submitCertification(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CertificationSubmitRequest request
    ) {
        return ApiResponse.success(service.submitCertification(request, idempotencyKey));
    }

    @GetMapping("/jobs")
    @RequirePermission("company.workbench.read")
    public ApiResponse<JobPageResponse> jobs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(service.jobs(page, size));
    }

    @PostMapping("/jobs")
    @RequirePermission("company.workbench.write")
    public ApiResponse<JobResponse> createJob(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody JobCreateRequest request
    ) {
        return ApiResponse.success(service.createJob(request, idempotencyKey));
    }

    @GetMapping("/jobs/{id}")
    @RequirePermission("company.workbench.read")
    public ApiResponse<JobResponse> getJob(@PathVariable long id) {
        return ApiResponse.success(service.getJob(id));
    }

    @PutMapping("/jobs/{id}")
    @RequirePermission("company.workbench.write")
    public ApiResponse<JobResponse> updateJob(
            @PathVariable long id,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody JobCreateRequest request
    ) {
        return ApiResponse.success(service.updateJob(
                id,
                new cn.localtalent.backend.job.api.JobUpdateRequest(
                        request.title(),
                        request.categoryCode(),
                        request.cityCode(),
                        request.salaryMin(),
                        request.salaryMax(),
                        request.jobDesc()),
                idempotencyKey));
    }

    @PostMapping("/jobs/{id}/submit-review")
    @RequirePermission("company.workbench.write")
    public ApiResponse<Object> submitReview(
            @PathVariable long id,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ApiResponse.success(service.submitReview(id, idempotencyKey));
    }

    @PostMapping("/jobs/{id}/offline")
    @RequirePermission("company.workbench.write")
    public ApiResponse<Object> offlineJob(
            @PathVariable long id,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) JobStatusRequest request
    ) {
        return ApiResponse.success(service.offlineJob(id, request, idempotencyKey));
    }

    @GetMapping("/applications")
    @RequirePermission("company.application.read")
    public ApiResponse<ApplicationPageResponse> applications(
            @RequestParam(value = "job_id", required = false) Long jobId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(service.applications(jobId, status, page, size));
    }

    @GetMapping("/applications/{id}")
    @RequirePermission("company.application.read")
    public ApiResponse<Object> applicationDetail(@PathVariable long id) {
        return ApiResponse.success(service.applicationDetail(id));
    }

    @PostMapping("/applications/{id}/stage")
    @RequirePermission("company.application.stage")
    public ApiResponse<Object> changeStage(
            @PathVariable long id,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ApplicationStageRequest request
    ) {
        return ApiResponse.success(service.changeStage(id, request, idempotencyKey));
    }

    @GetMapping("/interview-sessions")
    @RequirePermission("company.interview.list")
    public ApiResponse<InterviewSessionPageResponse> interviewSessions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(service.interviewSessions(page, size));
    }

    @PostMapping("/applications/{id}/interview-sessions")
    @RequirePermission("company.interview.session.create")
    public ApiResponse<InterviewSessionResponse> createInterview(
            @PathVariable long id,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody InterviewSessionCreateRequest request
    ) {
        return ApiResponse.success(service.createInterview(id, request, idempotencyKey));
    }

    @PostMapping("/exports")
    @RequirePermission("company.export.apply")
    public ApiResponse<ExportApplyResponse> applyExport(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ExportApplyRequest request
    ) {
        return ApiResponse.success(service.applyExport(request, idempotencyKey));
    }

    @GetMapping("/exports/{id}")
    @RequirePermission("company.export.read")
    public ApiResponse<ExportApplyResponse> exportDetail(@PathVariable long id) {
        return ApiResponse.success(service.exportDetail(id));
    }
}
