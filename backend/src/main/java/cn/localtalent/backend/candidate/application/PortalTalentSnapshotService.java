package cn.localtalent.backend.candidate.application;

import cn.localtalent.backend.candidate.api.PortalTalentSnapshotItemResponse;
import cn.localtalent.backend.candidate.api.PortalTalentSnapshotPageResponse;
import cn.localtalent.backend.candidate.domain.PortalSnapshotRow;
import cn.localtalent.backend.candidate.domain.PortalSnapshotRows;
import cn.localtalent.backend.candidate.infrastructure.CandidateJdbcRepository;
import cn.localtalent.backend.common.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PortalTalentSnapshotService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final List<Integer> ALLOWED_UPDATED_WITHIN_DAYS = List.of(3, 7, 30);

    private final CandidateJdbcRepository candidateRepository;
    private final ObjectMapper objectMapper;

    public PortalTalentSnapshotService(CandidateJdbcRepository candidateRepository) {
        this.candidateRepository = candidateRepository;
        this.objectMapper = new ObjectMapper();
    }

    public PortalTalentSnapshotPageResponse list(
            String cityCode,
            String categoryCode,
            Integer experienceMin,
            Integer experienceMax,
            String updatedWithin,
            String sort,
            int page,
            int size
    ) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 20 : Math.min(size, MAX_PAGE_SIZE);
        PortalSnapshotRows rows = candidateRepository.findVisibleSnapshots(
                cityCode,
                categoryCode,
                nonNegative(experienceMin),
                nonNegative(experienceMax),
                allowedUpdatedWithin(updatedWithin),
                allowedSort(sort),
                safePage,
                safeSize);
        List<PortalTalentSnapshotItemResponse> snapshots = rows.rows()
                .stream()
                .map(this::toResponse)
                .toList();
        return new PortalTalentSnapshotPageResponse(snapshots, rows.total(), safePage, safeSize);
    }

    private PortalTalentSnapshotItemResponse toResponse(PortalSnapshotRow row) {
        JsonNode snapshot = readSnapshot(row.snapshotJson());
        return new PortalTalentSnapshotItemResponse(
                row.snapshotId(),
                text(snapshot, "display_name_masked"),
                text(snapshot, "city_code"),
                text(snapshot, "category_code"),
                text(snapshot, "skills_summary"),
                integer(snapshot, "experience_years"),
                row.updatedAt());
    }

    private JsonNode readSnapshot(String snapshotJson) {
        try {
            return objectMapper.readTree(snapshotJson);
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SYS_500", "invalid snapshot payload");
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text.isBlank() ? null : text;
    }

    private Integer integer(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.isInt() || value.isLong()) {
            return value.asInt();
        }
        if (value.isTextual()) {
            try {
                return Integer.parseInt(value.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Integer nonNegative(Integer value) {
        if (value == null || value < 0) {
            return null;
        }
        return value;
    }

    private Integer allowedUpdatedWithin(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return ALLOWED_UPDATED_WITHIN_DAYS.contains(parsed) ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String allowedSort(String sort) {
        if ("experience_desc".equals(sort) || "experience_asc".equals(sort)) {
            return sort;
        }
        return "updated_desc";
    }
}
