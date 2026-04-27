package cn.localtalent.backend.interview.api;

import cn.localtalent.backend.authz.RequirePermission;
import cn.localtalent.backend.common.api.ApiResponse;
import cn.localtalent.backend.interview.service.InterviewService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interview")
public class CandidateInterviewController {

    private final InterviewService interviewService;

    public CandidateInterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PostMapping("/signin")
    @RequirePermission("candidate.interview.signin")
    public ApiResponse<InterviewSigninResponse> signin(@RequestBody InterviewSigninRequest request) {
        return ApiResponse.success(interviewService.signin(request));
    }
}
