package cn.localtalent.backend.openapi.api;

import cn.localtalent.backend.common.api.ApiResponse;
import cn.localtalent.backend.common.trace.TraceIdContext;
import cn.localtalent.backend.openapi.OpenApiEndpoint;
import cn.localtalent.backend.openapi.OpenApiStubService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@OpenApiEndpoint
@RestController
@RequestMapping("/api/open/v1")
public class OpenApiStubController {

    private final OpenApiStubService stubService;

    public OpenApiStubController(OpenApiStubService stubService) {
        this.stubService = stubService;
    }

    @PostMapping("/jobs/sync")
    public ResponseEntity<ApiResponse<OpenApiStubResponse>> syncJobs(@RequestBody Map<String, Object> payload) {
        return writeResponse(stubService.handleWrite(payload));
    }

    @PostMapping("/applications/sync")
    public ResponseEntity<ApiResponse<OpenApiStubResponse>> syncApplications(@RequestBody Map<String, Object> payload) {
        return writeResponse(stubService.handleWrite(payload));
    }

    @PostMapping("/consents/callback")
    public ResponseEntity<ApiResponse<OpenApiStubResponse>> consentCallback(@RequestBody Map<String, Object> payload) {
        return writeResponse(stubService.handleWrite(payload));
    }

    @PostMapping("/candidates/publishable-sync")
    public ResponseEntity<ApiResponse<OpenApiStubResponse>> syncPublishableCandidates(
            @RequestBody Map<String, Object> payload
    ) {
        return writeResponse(stubService.handleWrite(payload));
    }

    @GetMapping("/mappings/query")
    public ApiResponse<OpenApiMappingResponse> queryMapping(
            @RequestParam("source_system") String sourceSystem,
            @RequestParam("local_biz_type") String localBizType,
            @RequestParam("local_id") Long localId
    ) {
        return ApiResponse.success(stubService.queryMapping(sourceSystem, localBizType, localId));
    }

    private ResponseEntity<ApiResponse<OpenApiStubResponse>> writeResponse(OpenApiStubResponse response) {
        if ("retry_queued".equals(response.syncStatus())) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(new ApiResponse<>(
                            "OPEN_RETRY_202",
                            "accepted for retry",
                            TraceIdContext.getCurrentTraceId(),
                            response));
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
