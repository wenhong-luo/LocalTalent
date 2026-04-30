package cn.localtalent.backend.portal.application;

import cn.localtalent.backend.common.exception.ApiException;
import cn.localtalent.backend.portal.api.PortalContentPageResponse;
import cn.localtalent.backend.portal.api.PortalContentResponse;
import cn.localtalent.backend.portal.api.PortalEventPageResponse;
import cn.localtalent.backend.portal.api.PortalEventResponse;
import cn.localtalent.backend.portal.domain.PortalContentRow;
import cn.localtalent.backend.portal.domain.PortalEventRow;
import cn.localtalent.backend.portal.infrastructure.PortalContentEventJdbcRepository;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PortalContentEventService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Pattern SCRIPT_OR_STYLE = Pattern.compile("(?is)<(script|style)[^>]*>.*?</\\1>");
    private static final Pattern HTML_TAG = Pattern.compile("(?s)<[^>]+>");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final PortalContentEventJdbcRepository repository;

    public PortalContentEventService(PortalContentEventJdbcRepository repository) {
        this.repository = repository;
    }

    public PortalContentPageResponse listContents(String contentType, String cityCode, int page, int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = (normalizedPage - 1) * normalizedSize;
        List<PortalContentResponse> items = repository
                .listContents(contentType, cityCode, normalizedSize, offset)
                .stream()
                .map(this::contentResponse)
                .toList();
        return new PortalContentPageResponse(items, repository.countContents(contentType, cityCode));
    }

    public PortalContentResponse getContent(long contentId) {
        return repository.findContentById(contentId)
                .map(this::contentResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "content not found"));
    }

    public PortalEventPageResponse listEvents(String typeCode, String cityCode, int page, int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = (normalizedPage - 1) * normalizedSize;
        List<PortalEventResponse> items = repository
                .listEvents(typeCode, cityCode, normalizedSize, offset)
                .stream()
                .map(this::eventResponse)
                .toList();
        return new PortalEventPageResponse(items, repository.countEvents(typeCode, cityCode));
    }

    public PortalEventResponse getEvent(long eventId) {
        return repository.findEventById(eventId)
                .map(this::eventResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_404", "event not found"));
    }

    private PortalContentResponse contentResponse(PortalContentRow row) {
        return new PortalContentResponse(
                row.contentId(),
                row.contentType(),
                row.title(),
                row.coverUrl(),
                row.summary(),
                safeBodyText(row.bodyHtml()),
                row.cityCode(),
                row.publishTime(),
                row.updatedAt());
    }

    private PortalEventResponse eventResponse(PortalEventRow row) {
        return new PortalEventResponse(
                row.eventId(),
                row.title(),
                row.typeCode(),
                row.cityCode(),
                row.startTime(),
                row.endTime(),
                row.location(),
                row.status(),
                row.updatedAt());
    }

    private String safeBodyText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String withoutExecutableBlocks = SCRIPT_OR_STYLE.matcher(html).replaceAll(" ");
        String withoutTags = HTML_TAG.matcher(withoutExecutableBlocks).replaceAll(" ");
        return WHITESPACE.matcher(withoutTags)
                .replaceAll(" ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .trim();
    }
}
