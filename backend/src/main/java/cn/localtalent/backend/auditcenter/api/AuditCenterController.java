package cn.localtalent.backend.auditcenter.api;

import cn.localtalent.backend.auditcenter.service.AuditCenterService;
import cn.localtalent.backend.authz.RequirePermission;
import cn.localtalent.backend.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AuditCenterController {

    private final AuditCenterService auditCenterService;

    public AuditCenterController(AuditCenterService auditCenterService) {
        this.auditCenterService = auditCenterService;
    }

    @GetMapping("/audit-logs")
    @RequirePermission("admin.audit-log.read")
    public ApiResponse<AuditLogPageResponse> auditLogs(
            @RequestParam(value = "trace_id", required = false) String traceId,
            @RequestParam(value = "biz_type", required = false) String bizType,
            @RequestParam(value = "biz_id", required = false) Long bizId,
            @RequestParam(value = "action_type", required = false) String actionType,
            @RequestParam(value = "operator_id", required = false) Long operatorId,
            @RequestParam(value = "operator_role", required = false) String operatorRole,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ApiResponse.success(auditCenterService.listAuditLogs(
                traceId,
                bizType,
                bizId,
                actionType,
                operatorId,
                operatorRole,
                startTime,
                endTime,
                page,
                size));
    }

    @GetMapping("/field-access-logs")
    @RequirePermission("admin.field-access-log.read")
    public ApiResponse<FieldAccessLogPageResponse> fieldAccessLogs(
            @RequestParam(value = "trace_id", required = false) String traceId,
            @RequestParam(value = "biz_type", required = false) String bizType,
            @RequestParam(value = "biz_id", required = false) Long bizId,
            @RequestParam(value = "field_name", required = false) String fieldName,
            @RequestParam(value = "access_type", required = false) String accessType,
            @RequestParam(value = "operator_id", required = false) Long operatorId,
            @RequestParam(value = "operator_role", required = false) String operatorRole,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ApiResponse.success(auditCenterService.listFieldAccessLogs(
                traceId,
                bizType,
                bizId,
                fieldName,
                accessType,
                operatorId,
                operatorRole,
                startTime,
                endTime,
                page,
                size));
    }

    @GetMapping("/open-api-logs")
    @RequirePermission("admin.open-api-log.read")
    public ApiResponse<OpenApiLogPageResponse> openApiLogs(
            @RequestParam(value = "trace_id", required = false) String traceId,
            @RequestParam(value = "client_code", required = false) String clientCode,
            @RequestParam(value = "source_system", required = false) String sourceSystem,
            @RequestParam(value = "api_code", required = false) String apiCode,
            @RequestParam(value = "success_flag", required = false) Boolean successFlag,
            @RequestParam(value = "http_status", required = false) Integer httpStatus,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ApiResponse.success(auditCenterService.listOpenApiLogs(
                traceId,
                clientCode,
                sourceSystem,
                apiCode,
                successFlag,
                httpStatus,
                startTime,
                endTime,
                page,
                size));
    }

    @GetMapping("/audit-traces/{traceId}")
    @RequirePermission("admin.audit-trace.read")
    public ApiResponse<AuditTraceResponse> trace(@PathVariable("traceId") String traceId) {
        return ApiResponse.success(auditCenterService.trace(traceId));
    }
}
