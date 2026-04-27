package cn.localtalent.backend.admin.cms.service;

import cn.localtalent.backend.admin.cms.api.CmsContentPageResponse;
import cn.localtalent.backend.admin.cms.api.CmsContentRequest;
import cn.localtalent.backend.admin.cms.api.CmsContentResponse;
import cn.localtalent.backend.admin.cms.domain.CmsContentRow;
import cn.localtalent.backend.admin.cms.infrastructure.CmsContentJdbcRepository;
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
public class CmsContentService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CmsContentJdbcRepository cmsContentRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper = AuditJsonMapper.create();

    public CmsContentService(CmsContentJdbcRepository cmsContentRepository, AuditService auditService) {
        this.cmsContentRepository = cmsContentRepository;
        this.auditService = auditService;
    }

    public CmsContentPageResponse list(String contentType, Integer status, int page, int size) {
        requireAdminReader();
        validateStatusFilter(status);
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = (normalizedPage - 1) * normalizedSize;
        List<CmsContentResponse> items = cmsContentRepository
                .list(normalizeNullable(contentType, 32), status, normalizedSize, offset)
                .stream()
                .map(this::response)
                .toList();
        return new CmsContentPageResponse(items, cmsContentRepository.count(normalizeNullable(contentType, 32), status));
    }

    public CmsContentResponse detail(long contentId) {
        requireAdminReader();
        return response(requireContent(contentId));
    }

    @Transactional
    public CmsContentResponse create(CmsContentRequest request) {
        AuthzPrincipal principal = requireOperator();
        CmsContentRequest normalized = normalize(request);
        long contentId = cmsContentRepository.create(normalized);
        CmsContentRow after = requireContent(contentId);
        auditService.record(principal, "cms_content", contentId, "cms_create", null, json(after));
        return response(after);
    }

    @Transactional
    public CmsContentResponse update(long contentId, CmsContentRequest request) {
        AuthzPrincipal principal = requireOperator();
        CmsContentRow before = requireContent(contentId);
        CmsContentRequest normalized = normalize(request);
        cmsContentRepository.update(contentId, normalized);
        CmsContentRow after = requireContent(contentId);
        auditService.record(principal, "cms_content", contentId, "cms_update", json(before), json(after));
        return response(after);
    }

    @Transactional
    public CmsContentResponse softDelete(long contentId) {
        AuthzPrincipal principal = requireOperator();
        CmsContentRow before = requireContent(contentId);
        cmsContentRepository.softDelete(contentId);
        CmsContentRow after = requireContent(contentId);
        auditService.record(principal, "cms_content", contentId, "cms_soft_delete", json(before), json(after));
        return response(after);
    }

    private CmsContentRequest normalize(CmsContentRequest request) {
        if (request == null) {
            throw validation("request body is required");
        }
        int status = request.status() == null ? 1 : request.status();
        if (status != 0 && status != 1) {
            throw validation("status must be 0 or 1");
        }
        return new CmsContentRequest(
                required(request.contentType(), "content_type", 32),
                required(request.title(), "title", 200),
                normalizeNullable(request.coverUrl(), 500),
                normalizeNullable(request.summary(), 500),
                normalizeNullable(request.bodyHtml(), 65535),
                normalizeNullable(request.cityCode(), 32),
                status);
    }

    private CmsContentRow requireContent(long contentId) {
        return cmsContentRepository.findById(contentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "cms content not found"));
    }

    private CmsContentResponse response(CmsContentRow row) {
        return new CmsContentResponse(
                row.contentId(),
                row.contentType(),
                row.title(),
                row.coverUrl(),
                row.summary(),
                row.bodyHtml(),
                row.cityCode(),
                row.status(),
                row.publishTime(),
                row.updatedAt());
    }

    private void requireAdminReader() {
        AuthzPrincipal principal = AuthzContext.requireCurrentPrincipal();
        if (!principal.hasRole("operator") && !principal.hasRole("auditor")) {
            throw forbidden("admin cms reader role required");
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
