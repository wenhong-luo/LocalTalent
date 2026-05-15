package cn.localtalent.backend.admin.ops.api;

import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.OpsOverviewResponse;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.HomeSlotImageDownload;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.HomeSlotPageResponse;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.HomeSlotRequest;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.HomeSlotResponse;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.RecommendationPageResponse;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.RecommendationRequest;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.RecommendationResponse;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.RiskHandleRequest;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.RiskReviewPageResponse;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.RiskReviewResponse;
import cn.localtalent.backend.admin.ops.application.AdminPortalOpsService;
import cn.localtalent.backend.authz.RequirePermission;
import cn.localtalent.backend.common.api.ApiResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AdminPortalOpsController {

    private final AdminPortalOpsService service;

    public AdminPortalOpsController(AdminPortalOpsService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/ops/overview")
    @RequirePermission("admin.ops.read")
    public ApiResponse<OpsOverviewResponse> overview() {
        return ApiResponse.success(service.overview());
    }

    @GetMapping("/api/admin/recommendations")
    @RequirePermission("admin.recommendation.read")
    public ApiResponse<RecommendationPageResponse> recommendations(
            @RequestParam(value = "slot_code", required = false) String slotCode,
            @RequestParam(value = "target_type", required = false) String targetType,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(service.recommendations(slotCode, targetType, status, page, size));
    }

    @PostMapping("/api/admin/recommendations")
    @RequirePermission("admin.recommendation.write")
    public ApiResponse<RecommendationResponse> createRecommendation(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody RecommendationRequest request
    ) {
        return ApiResponse.success(service.createRecommendation(request, idempotencyKey));
    }

    @PutMapping("/api/admin/recommendations/{id}")
    @RequirePermission("admin.recommendation.write")
    public ApiResponse<RecommendationResponse> updateRecommendation(
            @PathVariable long id,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody RecommendationRequest request
    ) {
        return ApiResponse.success(service.updateRecommendation(id, request, idempotencyKey));
    }

    @PostMapping("/api/admin/recommendations/{id}/offline")
    @RequirePermission("admin.recommendation.write")
    public ApiResponse<RecommendationResponse> offlineRecommendation(
            @PathVariable long id,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ApiResponse.success(service.offlineRecommendation(id, idempotencyKey));
    }

    @GetMapping("/api/admin/home-slots")
    @RequirePermission("admin.portal-config.read")
    public ApiResponse<HomeSlotPageResponse> homeSlots(
            @RequestParam(value = "slot_code", required = false) String slotCode,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(service.homeSlots(slotCode, status, page, size));
    }

    @PostMapping("/api/admin/home-slots")
    @RequirePermission("admin.portal-config.write")
    public ApiResponse<HomeSlotResponse> createHomeSlot(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody HomeSlotRequest request
    ) {
        return ApiResponse.success(service.createHomeSlot(request, idempotencyKey));
    }

    @PutMapping("/api/admin/home-slots/{id}")
    @RequirePermission("admin.portal-config.write")
    public ApiResponse<HomeSlotResponse> updateHomeSlot(
            @PathVariable long id,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody HomeSlotRequest request
    ) {
        return ApiResponse.success(service.updateHomeSlot(id, request, idempotencyKey));
    }

    @PostMapping("/api/admin/home-slots/{id}/offline")
    @RequirePermission("admin.portal-config.write")
    public ApiResponse<HomeSlotResponse> offlineHomeSlot(
            @PathVariable long id,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ApiResponse.success(service.offlineHomeSlot(id, idempotencyKey));
    }

    @PostMapping(value = "/api/admin/home-slots/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("admin.portal-config.write")
    public ApiResponse<HomeSlotResponse> uploadHomeSlotImage(
            @PathVariable long id,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.success(service.uploadHomeSlotImage(id, file, idempotencyKey));
    }

    @GetMapping("/api/admin/home-slots/{id}/image/content")
    @RequirePermission("admin.portal-config.read")
    public ResponseEntity<ByteArrayResource> homeSlotImageContent(@PathVariable long id) {
        HomeSlotImageDownload download = service.homeSlotImageContent(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(download.fileName())
                        .build()
                        .toString())
                .body(new ByteArrayResource(download.content()));
    }

    @DeleteMapping("/api/admin/home-slots/{id}/image")
    @RequirePermission("admin.portal-config.write")
    public ApiResponse<HomeSlotResponse> deleteHomeSlotImage(
            @PathVariable long id,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ApiResponse.success(service.deleteHomeSlotImage(id, idempotencyKey));
    }

    @GetMapping("/api/admin/risk-reviews")
    @RequirePermission("admin.risk-review.read")
    public ApiResponse<RiskReviewPageResponse> riskReviews(
            @RequestParam(value = "risk_type", required = false) String riskType,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "severity", required = false) String severity,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(service.riskReviews(riskType, status, severity, page, size));
    }

    @GetMapping("/api/admin/risk-reviews/{id}")
    @RequirePermission("admin.risk-review.read")
    public ApiResponse<RiskReviewResponse> riskReview(@PathVariable long id) {
        return ApiResponse.success(service.riskReview(id));
    }

    @PostMapping("/api/admin/risk-reviews/{id}/handle")
    @RequirePermission("admin.risk-review.write")
    public ApiResponse<RiskReviewResponse> handleRiskReview(
            @PathVariable long id,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody RiskHandleRequest request
    ) {
        return ApiResponse.success(service.handleRiskReview(id, request, idempotencyKey));
    }
}
