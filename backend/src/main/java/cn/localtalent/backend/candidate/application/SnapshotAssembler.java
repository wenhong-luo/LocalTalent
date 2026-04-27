package cn.localtalent.backend.candidate.application;

import cn.localtalent.backend.audit.FieldAccessRecorder;
import cn.localtalent.backend.authz.AuthzPrincipal;
import cn.localtalent.backend.candidate.domain.CandidateProfileRaw;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SnapshotAssembler {

    private static final int MAX_SKILL_COUNT = 5;
    private static final int MAX_SUMMARY_LENGTH = 200;

    private final ObjectMapper objectMapper;
    private final FieldAccessRecorder fieldAccessRecorder;

    public SnapshotAssembler(FieldAccessRecorder fieldAccessRecorder) {
        this.objectMapper = new ObjectMapper();
        this.fieldAccessRecorder = fieldAccessRecorder;
    }

    public String build(AuthzPrincipal principal, CandidateProfileRaw profile) {
        recordSourceRead(principal, profile);

        JsonNode baseProfile = readJson(profile.baseProfileJson());
        JsonNode skills = readJson(profile.skillsJson());
        ObjectNode snapshot = objectMapper.createObjectNode();
        snapshot.put("display_name_masked", maskName(profile.realName()));
        putText(snapshot, "city_code", text(baseProfile, "city_code"));
        putText(snapshot, "category_code", text(baseProfile, "category_code"));
        Integer experienceYears = integer(baseProfile, "experience_years");
        if (experienceYears != null) {
            snapshot.put("experience_years", experienceYears);
        }
        putText(snapshot, "skills_summary", skillsSummary(skills));
        return snapshot.toString();
    }

    private void recordSourceRead(AuthzPrincipal principal, CandidateProfileRaw profile) {
        fieldAccessRecorder.record(principal, "candidate_profile", profile.candidateId(), "real_name", "SNAPSHOT_BUILD");
        if (profile.baseProfileJson() != null) {
            fieldAccessRecorder.record(principal, "candidate_profile", profile.candidateId(), "base_profile_json", "SNAPSHOT_BUILD");
        }
        if (profile.skillsJson() != null) {
            fieldAccessRecorder.record(principal, "candidate_profile", profile.candidateId(), "skills_json", "SNAPSHOT_BUILD");
        }
    }

    private JsonNode readJson(String json) {
        if (json == null || json.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }

    private void putText(ObjectNode node, String fieldName, String value) {
        if (value != null && !value.isBlank()) {
            node.put(fieldName, value);
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

    private String skillsSummary(JsonNode skills) {
        if (skills.isArray()) {
            return joinSkills(skills);
        }
        if (skills.isObject()) {
            String summary = text(skills, "summary");
            if (summary != null) {
                return limit(summary);
            }
            JsonNode skillList = skills.path("skills");
            if (skillList.isArray()) {
                return joinSkills(skillList);
            }
        }
        if (skills.isTextual()) {
            return limit(skills.asText());
        }
        return null;
    }

    private String joinSkills(JsonNode skills) {
        List<String> values = new ArrayList<>();
        for (JsonNode skill : skills) {
            if (values.size() >= MAX_SKILL_COUNT) {
                break;
            }
            String value = skill.asText();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        if (values.isEmpty()) {
            return null;
        }
        return limit(String.join(" / ", values));
    }

    private String limit(String value) {
        if (value.length() <= MAX_SUMMARY_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_SUMMARY_LENGTH);
    }

    private String maskName(String realName) {
        if (realName == null || realName.isBlank()) {
            return "求职者";
        }
        String normalized = realName.trim();
        if (normalized.length() == 1) {
            return "*";
        }
        return normalized.charAt(0) + "*";
    }
}
