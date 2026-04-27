package cn.localtalent.backend.admin.event.service;

import cn.localtalent.backend.admin.event.api.AdminEventPageResponse;
import cn.localtalent.backend.admin.event.api.AdminEventRequest;
import cn.localtalent.backend.admin.event.api.AdminEventResponse;
import cn.localtalent.backend.admin.event.domain.AdminEventRow;
import cn.localtalent.backend.admin.event.infrastructure.AdminEventJdbcRepository;
import cn.localtalent.backend.audit.AuditService;
import cn.localtalent.backend.authz.AuthzContext;
import cn.localtalent.backend.authz.AuthzPrincipal;
import cn.localtalent.backend.common.exception.ApiException;
import cn.localtalent.backend.common.json.AuditJsonMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminEventService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminEventJdbcRepository eventRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper = AuditJsonMapper.create();

    public AdminEventService(AdminEventJdbcRepository eventRepository, AuditService auditService) {
        this.eventRepository = eventRepository;
        this.auditService = auditService;
    }

    public AdminEventPageResponse list(String typeCode, String cityCode, Integer status, int page, int size) {
        requireAdminReader();
        validateStatusFilter(status);
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = (normalizedPage - 1) * normalizedSize;
        String normalizedTypeCode = normalizeNullable(typeCode, 64);
        String normalizedCityCode = normalizeNullable(cityCode, 32);
        List<AdminEventResponse> items = eventRepository
                .list(normalizedTypeCode, normalizedCityCode, status, normalizedSize, offset)
                .stream()
                .map(this::response)
                .toList();
        return new AdminEventPageResponse(items, eventRepository.count(normalizedTypeCode, normalizedCityCode, status));
    }

    public AdminEventResponse detail(long eventId) {
        requireAdminReader();
        return response(requireEvent(eventId));
    }

    @Transactional
    public AdminEventResponse create(AdminEventRequest request) {
        AuthzPrincipal principal = requireOperator();
        AdminEventRequest normalized = normalize(request);
        long eventId = eventRepository.create(normalized);
        AdminEventRow after = requireEvent(eventId);
        auditService.record(principal, "activity_event", eventId, "event_create", null, json(after));
        return response(after);
    }

    @Transactional
    public AdminEventResponse update(long eventId, AdminEventRequest request) {
        AuthzPrincipal principal = requireOperator();
        AdminEventRow before = requireEvent(eventId);
        AdminEventRequest normalized = normalize(request);
        eventRepository.update(eventId, normalized);
        AdminEventRow after = requireEvent(eventId);
        auditService.record(principal, "activity_event", eventId, "event_update", json(before), json(after));
        return response(after);
    }

    @Transactional
    public AdminEventResponse softDelete(long eventId) {
        AuthzPrincipal principal = requireOperator();
        AdminEventRow before = requireEvent(eventId);
        eventRepository.softDelete(eventId);
        AdminEventRow after = requireEvent(eventId);
        auditService.record(principal, "activity_event", eventId, "event_soft_delete", json(before), json(after));
        return response(after);
    }

    private AdminEventRequest normalize(AdminEventRequest request) {
        if (request == null) {
            throw validation("request body is required");
        }
        int status = request.status() == null ? 1 : request.status();
        if (status != 0 && status != 1) {
            throw validation("status must be 0 or 1");
        }
        if (request.startTime() == null) {
            throw validation("start_time is required");
        }
        if (request.endTime() == null) {
            throw validation("end_time is required");
        }
        if (!request.endTime().isAfter(request.startTime())) {
            throw validation("end_time must be after start_time");
        }
        return new AdminEventRequest(
                required(request.title(), "title", 200),
                required(request.typeCode(), "type_code", 64),
                normalizeNullable(request.cityCode(), 32),
                request.startTime(),
                request.endTime(),
                normalizeNullable(request.location(), 255),
                request.organizerCompanyId(),
                status);
    }

    private AdminEventRow requireEvent(long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "activity event not found"));
    }

    private AdminEventResponse response(AdminEventRow row) {
        return new AdminEventResponse(
                row.eventId(),
                row.title(),
                row.typeCode(),
                row.cityCode(),
                row.startTime(),
                row.endTime(),
                row.location(),
                row.organizerCompanyId(),
                row.status(),
                row.updatedAt());
    }

    private void requireAdminReader() {
        AuthzPrincipal principal = AuthzContext.requireCurrentPrincipal();
        if (!principal.hasRole("operator") && !principal.hasRole("auditor")) {
            throw forbidden("admin event reader role required");
        }
    }

    private AuthzPrincipal requireOperator() {
        AuthzPrincipal principal = AuthzContext.requireCurrentPrincipal();
        if (!principal.hasRole("operator")) {
            throw forbidden("operator role required");
        }
        return principal;
    }

    private void validateStatusFilter(Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw validation("status must be 0 or 1");
        }
    }

    private String required(String value, String fieldName, int maxLength) {
        String normalized = normalizeNullable(value, maxLength);
        if (normalized == null) {
            throw validation(fieldName + " is required");
        }
        return normalized;
    }

    private String normalizeNullable(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return objectMapper.valueToTree(Map.of("serialization", "failed")).toString();
        }
    }

    private ApiException validation(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALID_400", message);
    }

    private ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "AUTHZ_403", message);
    }
}
