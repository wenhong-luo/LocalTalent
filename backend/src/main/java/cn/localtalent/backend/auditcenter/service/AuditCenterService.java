package cn.localtalent.backend.auditcenter.service;

import cn.localtalent.backend.auditcenter.api.AuditLogPageResponse;
import cn.localtalent.backend.auditcenter.api.AuditLogResponse;
import cn.localtalent.backend.auditcenter.api.AuditTraceResponse;
import cn.localtalent.backend.auditcenter.api.FieldAccessLogPageResponse;
import cn.localtalent.backend.auditcenter.api.FieldAccessLogResponse;
import cn.localtalent.backend.auditcenter.api.OpenApiLogPageResponse;
import cn.localtalent.backend.auditcenter.api.OpenApiLogResponse;
import cn.localtalent.backend.auditcenter.domain.AuditLogQuery;
import cn.localtalent.backend.auditcenter.domain.AuditLogRow;
import cn.localtalent.backend.auditcenter.domain.FieldAccessLogQuery;
import cn.localtalent.backend.auditcenter.domain.FieldAccessLogRow;
import cn.localtalent.backend.auditcenter.domain.OpenApiLogQuery;
import cn.localtalent.backend.auditcenter.domain.OpenApiLogRow;
import cn.localtalent.backend.auditcenter.infrastructure.AuditCenterJdbcRepository;
import cn.localtalent.backend.common.exception.ApiException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AuditCenterService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int TRACE_LIST_LIMIT = 100;

    private final AuditCenterJdbcRepository repository;
    private final AuditCenterMaskingService maskingService;

    public AuditCenterService(AuditCenterJdbcRepository repository, AuditCenterMaskingService maskingService) {
        this.repository = repository;
        this.maskingService = maskingService;
    }

    public AuditLogPageResponse listAuditLogs(
            String traceId,
            String bizType,
            Long bizId,
            String actionType,
            Long operatorId,
            String operatorRole,
            String startTime,
            String endTime,
            int page,
            int size
    ) {
        PageBounds bounds = bounds(page, size);
        AuditLogQuery query = new AuditLogQuery(
                normalize(traceId),
                normalize(bizType),
                bizId,
                normalize(actionType),
                operatorId,
                normalize(operatorRole),
                parseTime(startTime, "start_time"),
                parseTime(endTime, "end_time"));
        return new AuditLogPageResponse(
                repository.listAuditLogs(query, bounds.limit(), bounds.offset()).stream()
                        .map(this::auditResponse)
                        .toList(),
                repository.countAuditLogs(query));
    }

    public FieldAccessLogPageResponse listFieldAccessLogs(
            String traceId,
            String bizType,
            Long bizId,
            String fieldName,
            String accessType,
            Long operatorId,
            String operatorRole,
            String startTime,
            String endTime,
            int page,
            int size
    ) {
        PageBounds bounds = bounds(page, size);
        FieldAccessLogQuery query = new FieldAccessLogQuery(
                normalize(traceId),
                normalize(bizType),
                bizId,
                normalize(fieldName),
                normalize(accessType),
                operatorId,
                normalize(operatorRole),
                parseTime(startTime, "start_time"),
                parseTime(endTime, "end_time"));
        return new FieldAccessLogPageResponse(
                repository.listFieldAccessLogs(query, bounds.limit(), bounds.offset()).stream()
                        .map(this::fieldAccessResponse)
                        .toList(),
                repository.countFieldAccessLogs(query));
    }

    public OpenApiLogPageResponse listOpenApiLogs(
            String traceId,
            String clientCode,
            String sourceSystem,
            String apiCode,
            Boolean successFlag,
            Integer httpStatus,
            String startTime,
            String endTime,
            int page,
            int size
    ) {
        PageBounds bounds = bounds(page, size);
        OpenApiLogQuery query = new OpenApiLogQuery(
                normalize(traceId),
                normalize(clientCode),
                normalize(sourceSystem),
                normalize(apiCode),
                successFlag,
                httpStatus,
                parseTime(startTime, "start_time"),
                parseTime(endTime, "end_time"));
        return new OpenApiLogPageResponse(
                repository.listOpenApiLogs(query, bounds.limit(), bounds.offset()).stream()
                        .map(this::openApiResponse)
                        .toList(),
                repository.countOpenApiLogs(query));
    }

    public AuditTraceResponse trace(String traceId) {
        String normalizedTraceId = required(traceId, "trace_id");
        AuditLogQuery auditQuery = new AuditLogQuery(
                normalizedTraceId, null, null, null, null, null, null, null);
        FieldAccessLogQuery fieldQuery = new FieldAccessLogQuery(
                normalizedTraceId, null, null, null, null, null, null, null, null);
        OpenApiLogQuery openQuery = new OpenApiLogQuery(
                normalizedTraceId, null, null, null, null, null, null, null);
        return new AuditTraceResponse(
                normalizedTraceId,
                repository.listAuditLogs(auditQuery, TRACE_LIST_LIMIT, 0).stream()
                        .map(this::auditResponse)
                        .toList(),
                repository.listFieldAccessLogs(fieldQuery, TRACE_LIST_LIMIT, 0).stream()
                        .map(this::fieldAccessResponse)
                        .toList(),
                repository.listOpenApiLogs(openQuery, TRACE_LIST_LIMIT, 0).stream()
                        .map(this::openApiResponse)
                        .toList());
    }

    private AuditLogResponse auditResponse(AuditLogRow row) {
        return new AuditLogResponse(
                row.id(),
                row.operatorId(),
                row.operatorRole(),
                row.bizType(),
                row.bizId(),
                row.actionType(),
                maskingService.sanitizeJson(row.beforeJson()),
                maskingService.sanitizeJson(row.afterJson()),
                row.traceId(),
                row.createdAt());
    }

    private FieldAccessLogResponse fieldAccessResponse(FieldAccessLogRow row) {
        return new FieldAccessLogResponse(
                row.id(),
                row.operatorId(),
                row.operatorRole(),
                row.bizType(),
                row.bizId(),
                row.fieldName(),
                row.accessType(),
                row.traceId(),
                row.createdAt());
    }

    private OpenApiLogResponse openApiResponse(OpenApiLogRow row) {
        return new OpenApiLogResponse(
                row.id(),
                row.sourceSystem(),
                row.clientCode(),
                row.apiCode(),
                row.traceId(),
                maskingService.sanitizeUri(row.requestUri()),
                row.requestMethod(),
                row.bizType(),
                row.bizId(),
                row.httpStatus(),
                row.successFlag(),
                row.requestHash(),
                maskingService.sanitizeToken(row.idempotencyKey()),
                row.durationMs(),
                maskingService.sanitizeJson(row.requestSummaryJson()),
                maskingService.sanitizeJson(row.responseSummaryJson()),
                row.requestTime(),
                row.responseTime(),
                maskingService.sanitizeText(row.errorMsg()));
    }

    private PageBounds bounds(int page, int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return new PageBounds(normalizedSize, (normalizedPage - 1) * normalizedSize);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String required(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALID_400", fieldName + " is required");
        }
        return normalized;
    }

    private LocalDateTime parseTime(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(normalized);
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(normalized).toLocalDateTime();
            } catch (DateTimeParseException exception) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALID_400", fieldName + " must be ISO date time");
            }
        }
    }

    private record PageBounds(int limit, int offset) {
    }
}
