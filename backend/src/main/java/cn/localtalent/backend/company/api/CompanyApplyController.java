package cn.localtalent.backend.company.api;

import cn.localtalent.backend.authz.RequirePermission;
import cn.localtalent.backend.common.api.ApiResponse;
import cn.localtalent.backend.company.application.CompanyService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/company")
public class CompanyApplyController {

    private final CompanyService companyService;

    public CompanyApplyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping("/apply")
    @RequirePermission("company.apply")
    public ApiResponse<CompanyStatusResponse> apply(@RequestBody CompanyApplyRequest request) {
        return ApiResponse.success(companyService.apply(request));
    }
}
