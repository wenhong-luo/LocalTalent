package cn.localtalent.backend.candidate.api;

import cn.localtalent.backend.authz.RequirePermission;
import cn.localtalent.backend.candidate.application.CandidateConsentService;
import cn.localtalent.backend.common.api.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consents")
public class CandidateConsentController {

    private final CandidateConsentService consentService;

    public CandidateConsentController(CandidateConsentService consentService) {
        this.consentService = consentService;
    }

    @PostMapping
    @RequirePermission("candidate.consent.create")
    public ApiResponse<ConsentCreateResponse> create(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ConsentCreateRequest request
    ) {
        return ApiResponse.success(consentService.createConsent(request, idempotencyKey));
    }

    @PostMapping("/{id}/revoke")
    @RequirePermission("candidate.consent.revoke")
    public ApiResponse<ConsentRevokeResponse> revoke(
            @PathVariable long id,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) ConsentRevokeRequest request
    ) {
        return ApiResponse.success(consentService.revokeConsent(id, request, idempotencyKey));
    }
}
