package cn.localtalent.backend.candidatecenter.api;

import cn.localtalent.backend.authz.RequirePermission;
import cn.localtalent.backend.candidatecenter.application.CandidateCenterService;
import cn.localtalent.backend.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/candidate/center")
public class CandidateCenterController {

    private final CandidateCenterService candidateCenterService;

    public CandidateCenterController(CandidateCenterService candidateCenterService) {
        this.candidateCenterService = candidateCenterService;
    }

    @GetMapping("/overview")
    @RequirePermission("candidate.center.overview")
    public ApiResponse<CandidateCenterOverviewResponse> overview() {
        return ApiResponse.success(candidateCenterService.overview());
    }
}
