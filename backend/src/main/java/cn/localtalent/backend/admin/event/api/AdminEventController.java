package cn.localtalent.backend.admin.event.api;

import cn.localtalent.backend.admin.event.service.AdminEventService;
import cn.localtalent.backend.authz.RequirePermission;
import cn.localtalent.backend.common.api.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/events")
public class AdminEventController {

    private final AdminEventService eventService;

    public AdminEventController(AdminEventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    @RequirePermission("admin.event.read")
    public ApiResponse<AdminEventPageResponse> list(
            @RequestParam(value = "type_code", required = false) String typeCode,
            @RequestParam(value = "city_code", required = false) String cityCode,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ApiResponse.success(eventService.list(typeCode, cityCode, status, page, size));
    }

    @GetMapping("/{eventId}")
    @RequirePermission("admin.event.read")
    public ApiResponse<AdminEventResponse> detail(@PathVariable("eventId") long eventId) {
        return ApiResponse.success(eventService.detail(eventId));
    }

    @PostMapping
    @RequirePermission("admin.event.write")
    public ApiResponse<AdminEventResponse> create(@RequestBody AdminEventRequest request) {
        return ApiResponse.success(eventService.create(request));
    }

    @PutMapping("/{eventId}")
    @RequirePermission("admin.event.write")
    public ApiResponse<AdminEventResponse> update(
            @PathVariable("eventId") long eventId,
            @RequestBody AdminEventRequest request
    ) {
        return ApiResponse.success(eventService.update(eventId, request));
    }

    @DeleteMapping("/{eventId}")
    @RequirePermission("admin.event.write")
    public ApiResponse<AdminEventResponse> softDelete(@PathVariable("eventId") long eventId) {
        return ApiResponse.success(eventService.softDelete(eventId));
    }
}
