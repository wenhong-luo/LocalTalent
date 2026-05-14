package cn.localtalent.backend.candidate.application;

import cn.localtalent.backend.audit.FieldAccessRecorder;
import cn.localtalent.backend.authz.AuthzPrincipal;
import cn.localtalent.backend.candidate.domain.CandidateProfileRaw;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
        JsonNode education = readJson(profile.educationJson());
        JsonNode skills = readJson(profile.skillsJson());
        ObjectNode snapshot = objectMapper.createObjectNode();
        snapshot.put("display_name_masked", maskName(profile.realName()));
        putText(snapshot, "city_code", text(baseProfile, "city_code"));
        putText(snapshot, "category_code", text(baseProfile, "category_code"));
        putText(snapshot, "gender", text(baseProfile, "gender"));
        putText(snapshot, "education_code", firstText(baseProfile, "education_code", "highest_education"));
        putText(snapshot, "highest_education", text(baseProfile, "highest_education"));
        putText(snapshot, "age_band", ageBand(text(baseProfile, "birth_date")));
        putText(snapshot, "expected_salary", text(baseProfile, "expected_salary"));
        putText(snapshot, "expected_salary_code", firstText(baseProfile, "expected_salary_code", "expected_salary"));
        putText(snapshot, "industry_code", text(baseProfile, "industry_code"));
        putText(snapshot, "work_nature", text(baseProfile, "work_nature"));
        String majorName = firstText(baseProfile, "major_name");
        if (majorName == null) {
            majorName = firstMajorName(education);
        }
        putText(snapshot, "major_name", majorName);
        List<String> expectedPositions = textList(baseProfile.path("expected_positions"), 6, 80);
        putTextArray(snapshot, "expected_positions", expectedPositions);
        putText(snapshot, "expected_positions_text", joinText(expectedPositions));
        List<String> expectedCities = textList(baseProfile.path("expected_cities"), 6, 80);
        putTextArray(snapshot, "expected_cities", expectedCities);
        putText(snapshot, "expected_cities_text", joinText(expectedCities));
        Integer experienceYears = integer(baseProfile, "experience_years");
        if (experienceYears != null) {
            snapshot.put("experience_years", experienceYears);
        }
        String skillsSummary = skillsSummary(skills);
        putText(snapshot, "skills_summary", skillsSummary);
        List<String> resumeTags = resumeTags(baseProfile, skills);
        putTextArray(snapshot, "resume_tags", resumeTags);
        putText(snapshot, "resume_tags_text", joinText(resumeTags));
        return snapshot.toString();
    }

    private void recordSourceRead(AuthzPrincipal principal, CandidateProfileRaw profile) {
        fieldAccessRecorder.record(principal, "candidate_profile", profile.candidateId(), "real_name", "SNAPSHOT_BUILD");
        if (profile.baseProfileJson() != null) {
            fieldAccessRecorder.record(principal, "candidate_profile", profile.candidateId(), "base_profile_json", "SNAPSHOT_BUILD");
        }
        if (profile.educationJson() != null) {
            fieldAccessRecorder.record(principal, "candidate_profile", profile.candidateId(), "education_json", "SNAPSHOT_BUILD");
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

    private void putTextArray(ObjectNode node, String fieldName, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        ArrayNode array = node.putArray(fieldName);
        values.forEach(array::add);
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text.isBlank() ? null : text;
    }

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = text(node, fieldName);
            if (value != null) {
                return value;
            }
        }
        return null;
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

    private List<String> resumeTags(JsonNode baseProfile, JsonNode skills) {
        List<String> values = textList(baseProfile.path("resume_tags"), 8, 32);
        if (!values.isEmpty()) {
            return values;
        }
        return textList(skills.isObject() ? skills.path("skills") : skills, 8, 32);
    }

    private List<String> textList(JsonNode node, int limit, int valueLimit) {
        List<String> values = new ArrayList<>();
        if (node == null || node.isMissingNode() || node.isNull()) {
            return values;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                addText(values, item.asText(), limit, valueLimit);
            }
            return values;
        }
        if (node.isTextual()) {
            String[] parts = node.asText().split("[,，/、]");
            for (String part : parts) {
                addText(values, part, limit, valueLimit);
            }
        }
        return values;
    }

    private void addText(List<String> values, String raw, int limit, int valueLimit) {
        if (values.size() >= limit || raw == null) {
            return;
        }
        String value = raw.trim();
        if (!value.isBlank() && !values.contains(value)) {
            values.add(value.length() > valueLimit ? value.substring(0, valueLimit) : value);
        }
    }

    private String joinText(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join(" / ", values);
    }

    private String firstMajorName(JsonNode education) {
        if (!education.isArray()) {
            return null;
        }
        for (JsonNode item : education) {
            String majorName = text(item, "major_name");
            if (majorName != null) {
                return majorName;
            }
        }
        return null;
    }

    private String ageBand(String birthDate) {
        if (birthDate == null || birthDate.isBlank()) {
            return null;
        }
        try {
            int age = LocalDate.now().getYear() - LocalDate.parse(birthDate).getYear();
            if (age < 16 || age > 80) {
                return null;
            }
            int lower = age / 5 * 5;
            int upper = lower + 4;
            return lower + "-" + upper;
        } catch (DateTimeParseException exception) {
            return null;
        }
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
