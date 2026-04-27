package cn.localtalent.backend.interview.api;

import cn.localtalent.backend.authz.RequirePermission;
import cn.localtalent.backend.common.api.ApiResponse;
import cn.localtalent.backend.interview.service.InterviewService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/company")
public class CompanyInterviewController {

    private final InterviewService interviewService;

    public CompanyInterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PostMapping("/applications/{id}/interview-sessions")
    @RequirePermission("company.interview.session.create")
    public ApiResponse<InterviewSessionResponse> createSession(
            @PathVariable long id,
            @RequestBody InterviewSessionCreateRequest request
    ) {
        return ApiResponse.success(interviewService.createSession(id, request));
    }

    @PostMapping("/interview-sessions/{id}/qrcode")
    @RequirePermission("company.interview.qrcode.generate")
    public ApiResponse<SigninCodeResponse> generateCode(
            @PathVariable long id,
            @RequestBody(required = false) SigninCodeRequest request
    ) {
        return ApiResponse.success(interviewService.generateCode(id, request));
    }
}
