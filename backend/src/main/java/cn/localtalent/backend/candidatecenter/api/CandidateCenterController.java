package cn.localtalent.backend.candidatecenter.api;

import cn.localtalent.backend.authz.RequirePermission;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.ApplicationPageResponse;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.AiSuggestionTaskResponse;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.AttachmentDownload;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.AttachmentResponse;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.FavoriteCreateRequest;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.FavoritePageResponse;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.NotificationPageResponse;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.ResumeResponse;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.ResumeSaveRequest;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.SubscriptionCreateRequest;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.SubscriptionPageResponse;
import cn.localtalent.backend.candidatecenter.application.CandidateClosureService;
import cn.localtalent.backend.candidatecenter.application.CandidateCenterService;
import cn.localtalent.backend.common.api.ApiResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/candidate/center")
public class CandidateCenterController {

    private final CandidateCenterService candidateCenterService;
    private final CandidateClosureService candidateClosureService;

    public CandidateCenterController(
            CandidateCenterService candidateCenterService,
            CandidateClosureService candidateClosureService
    ) {
        this.candidateCenterService = candidateCenterService;
        this.candidateClosureService = candidateClosureService;
    }

    @GetMapping("/overview")
    @RequirePermission("candidate.center.overview")
    public ApiResponse<CandidateCenterOverviewResponse> overview() {
        return ApiResponse.success(candidateCenterService.overview());
    }

    @GetMapping("/resume")
    @RequirePermission("candidate.center.resume.read")
    public ApiResponse<ResumeResponse> resume() {
        return ApiResponse.success(candidateClosureService.resume());
    }

    @PutMapping("/resume")
    @RequirePermission("candidate.center.resume.write")
    public ApiResponse<ResumeResponse> saveResume(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ResumeSaveRequest request
    ) {
        return ApiResponse.success(candidateClosureService.saveResume(request, idempotencyKey));
    }

    @GetMapping("/resume/preview")
    @RequirePermission("candidate.center.resume.read")
    public ApiResponse<ResumeResponse> preview() {
        return ApiResponse.success(candidateClosureService.preview());
    }

    @GetMapping("/resume/attachment")
    @RequirePermission("candidate.center.resume.read")
    public ApiResponse<AttachmentResponse> attachment() {
        return ApiResponse.success(candidateClosureService.attachment());
    }

    @PostMapping(value = "/resume/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("candidate.center.resume.write")
    public ApiResponse<AttachmentResponse> uploadAttachment(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.success(candidateClosureService.uploadAttachment(file, idempotencyKey));
    }

    @GetMapping("/resume/attachment/download")
    @RequirePermission("candidate.center.resume.read")
    public ResponseEntity<ByteArrayResource> downloadAttachment() {
        AttachmentDownload download = candidateClosureService.downloadAttachment();
        ByteArrayResource resource = new ByteArrayResource(download.content());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.fileName())
                        .build()
                        .toString())
                .body(resource);
    }

    @DeleteMapping("/resume/attachment")
    @RequirePermission("candidate.center.resume.write")
    public ApiResponse<AttachmentResponse> deleteAttachment(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ApiResponse.success(candidateClosureService.deleteAttachment(idempotencyKey));
    }

    @PostMapping("/resume/ai-suggestions")
    @RequirePermission("candidate.center.resume.write")
    public ApiResponse<AiSuggestionTaskResponse> generateAiSuggestions(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ApiResponse.success(candidateClosureService.generateAiSuggestions(idempotencyKey));
    }

    @GetMapping("/resume/ai-suggestions/latest")
    @RequirePermission("candidate.center.resume.read")
    public ApiResponse<AiSuggestionTaskResponse> latestAiSuggestions() {
        return ApiResponse.success(candidateClosureService.latestAiSuggestions());
    }

    @PostMapping("/resume/ai-suggestions/{itemId}/apply")
    @RequirePermission("candidate.center.resume.write")
    public ApiResponse<AiSuggestionTaskResponse> applyAiSuggestion(
            @PathVariable long itemId,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ApiResponse.success(candidateClosureService.applyAiSuggestion(itemId, idempotencyKey));
    }

    @PostMapping("/resume/ai-suggestions/{itemId}/dismiss")
    @RequirePermission("candidate.center.resume.write")
    public ApiResponse<AiSuggestionTaskResponse> dismissAiSuggestion(
            @PathVariable long itemId,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ApiResponse.success(candidateClosureService.dismissAiSuggestion(itemId, idempotencyKey));
    }

    @GetMapping("/applications")
    @RequirePermission("candidate.center.application.list")
    public ApiResponse<ApplicationPageResponse> applications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(candidateClosureService.applications(page, size));
    }

    @GetMapping("/favorites")
    @RequirePermission("candidate.center.favorite.read")
    public ApiResponse<FavoritePageResponse> favorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(candidateClosureService.favorites(page, size));
    }

    @PostMapping("/favorites")
    @RequirePermission("candidate.center.favorite.write")
    public ApiResponse<FavoritePageResponse> createFavorite(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody FavoriteCreateRequest request
    ) {
        return ApiResponse.success(candidateClosureService.createFavorite(request, idempotencyKey));
    }

    @PostMapping("/favorites/{id}/cancel")
    @RequirePermission("candidate.center.favorite.write")
    public ApiResponse<FavoritePageResponse> cancelFavorite(
            @PathVariable long id,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ApiResponse.success(candidateClosureService.cancelFavorite(id, idempotencyKey));
    }

    @GetMapping("/subscriptions")
    @RequirePermission("candidate.center.subscription.read")
    public ApiResponse<SubscriptionPageResponse> subscriptions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(candidateClosureService.subscriptions(page, size));
    }

    @PostMapping("/subscriptions")
    @RequirePermission("candidate.center.subscription.write")
    public ApiResponse<SubscriptionPageResponse> createSubscription(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody SubscriptionCreateRequest request
    ) {
        return ApiResponse.success(candidateClosureService.createSubscription(request, idempotencyKey));
    }

    @PostMapping("/subscriptions/{id}/cancel")
    @RequirePermission("candidate.center.subscription.write")
    public ApiResponse<SubscriptionPageResponse> cancelSubscription(
            @PathVariable long id,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ApiResponse.success(candidateClosureService.cancelSubscription(id, idempotencyKey));
    }

    @GetMapping("/notifications")
    @RequirePermission("candidate.center.notification.read")
    public ApiResponse<NotificationPageResponse> notifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(candidateClosureService.notifications(page, size));
    }

    @PostMapping("/notifications/{id}/read")
    @RequirePermission("candidate.center.notification.write")
    public ApiResponse<NotificationPageResponse> markNotificationRead(
            @PathVariable long id,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ApiResponse.success(candidateClosureService.markNotificationRead(id, idempotencyKey));
    }
}
