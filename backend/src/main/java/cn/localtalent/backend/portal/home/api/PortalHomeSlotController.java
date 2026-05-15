package cn.localtalent.backend.portal.home.api;

import cn.localtalent.backend.authz.PublicEndpoint;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.HomeSlotImageDownload;
import cn.localtalent.backend.common.api.ApiResponse;
import cn.localtalent.backend.portal.home.api.PortalHomeSlotDtos.HomeSlotListResponse;
import cn.localtalent.backend.portal.home.application.PortalHomeSlotService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PortalHomeSlotController {

    private final PortalHomeSlotService service;

    public PortalHomeSlotController(PortalHomeSlotService service) {
        this.service = service;
    }

    @GetMapping("/api/portal/home-slots")
    @PublicEndpoint
    public ApiResponse<HomeSlotListResponse> homeSlots(
            @RequestParam(value = "slot_codes", required = false) String slotCodes,
            @RequestParam(value = "limit", defaultValue = "30") int limit
    ) {
        return ApiResponse.success(service.list(slotCodes, limit));
    }

    @GetMapping("/api/portal/home-slots/{id}/image")
    @PublicEndpoint
    public ResponseEntity<ByteArrayResource> homeSlotImage(@PathVariable long id) {
        HomeSlotImageDownload download = service.publicImage(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(download.fileName())
                        .build()
                        .toString())
                .body(new ByteArrayResource(download.content()));
    }
}
