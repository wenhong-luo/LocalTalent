package cn.localtalent.backend.candidatecenter.infrastructure;

import cn.localtalent.backend.candidatecenter.domain.CandidateApplicationSummary;
import cn.localtalent.backend.candidatecenter.domain.CandidateConsentSummary;
import cn.localtalent.backend.candidatecenter.domain.CandidateResumeSummary;
import cn.localtalent.backend.candidatecenter.domain.CandidateSigninSummary;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CandidateCenterJdbcRepository {

    private static final int MAX_SKILL_COUNT = 5;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CandidateCenterJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CandidateResumeSummary resumeSummary(long candidateId) {
        return jdbcTemplate.query(
                        "SELECT base_profile_json, education_json, experience_json, skills_json, "
                                + "attachment_object_key, "
                                + "DATE_FORMAT(updated_at, '%Y-%m-%dT%H:%i:%s') AS updated_at_text "
                                + "FROM candidate_resume "
                                + "WHERE candidate_id = ? AND status = 1 "
                                + "ORDER BY id DESC LIMIT 1",
                        (rs, rowNum) -> resumeSummary(rs),
                        candidateId)
                .stream()
                .findFirst()
                .orElseGet(CandidateResumeSummary::empty);
    }

    public CandidateApplicationSummary applicationSummary(long candidateId) {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM job_application WHERE candidate_id = ?",
                Long.class,
                candidateId);
        Optional<LatestApplicationRow> latest = jdbcTemplate.query(
                        "SELECT a.application_status, j.title AS job_title "
                                + "FROM job_application a "
                                + "JOIN job_post j ON j.id = a.job_id "
                                + "WHERE a.candidate_id = ? "
                                + "ORDER BY a.apply_time DESC, a.id DESC LIMIT 1",
                        (rs, rowNum) -> new LatestApplicationRow(
                                rs.getObject("application_status", Integer.class),
                                rs.getString("job_title")),
                        candidateId)
                .stream()
                .findFirst();
        if (latest.isEmpty()) {
            return CandidateApplicationSummary.empty();
        }
        return new CandidateApplicationSummary(
                total == null ? 0 : total,
                latest.get().status(),
                blankToDefault(latest.get().jobTitle(), "暂无"));
    }

    public CandidateSigninSummary signinSummary(long candidateId) {
        return jdbcTemplate.query(
                        "SELECT DATE_FORMAT(sign_time, '%Y-%m-%dT%H:%i:%s') AS sign_time_text "
                                + "FROM interview_signin "
                                + "WHERE candidate_id = ? "
                                + "ORDER BY sign_time DESC, id DESC LIMIT 1",
                        (rs, rowNum) -> new CandidateSigninSummary(
                                "已签到",
                                blankToDefault(rs.getString("sign_time_text"), "")),
                        candidateId)
                .stream()
                .findFirst()
                .orElseGet(CandidateSigninSummary::empty);
    }

    public CandidateConsentSummary consentSummary(long candidateId) {
        Optional<ControlProfileRow> profile = findControlProfile(candidateId);
        Optional<LatestConsentRow> latestConsent = findLatestConsent(candidateId);

        if (isRevoked(profile, latestConsent)) {
            return new CandidateConsentSummary(
                    consentId(profile, latestConsent),
                    "revoked",
                    0,
                    "服务端确认已撤回",
                    "已撤回同意",
                    updatedAt(profile, latestConsent));
        }

        if (profile.isPresent()
                && profile.get().consentStatus() == 1
                && profile.get().publishableFlag() == 1
                && profile.get().activeSnapshotId() != null) {
            return new CandidateConsentSummary(
                    consentId(profile, latestConsent),
                    "consented",
                    1,
                    "服务端确认已同意",
                    "",
                    updatedAt(profile, latestConsent));
        }

        return new CandidateConsentSummary(
                consentId(profile, latestConsent),
                "not_publishable",
                profile.map(ControlProfileRow::publishableFlag).orElse(0),
                "暂不可发布",
                notPublishableReason(profile, latestConsent),
                updatedAt(profile, latestConsent));
    }

    private CandidateResumeSummary resumeSummary(ResultSet rs) throws SQLException {
        String baseProfileJson = rs.getString("base_profile_json");
        String educationJson = rs.getString("education_json");
        String experienceJson = rs.getString("experience_json");
        String skillsJson = rs.getString("skills_json");
        String attachmentObjectKey = rs.getString("attachment_object_key");
        int completed = 0;
        completed += hasJsonContent(baseProfileJson) ? 1 : 0;
        completed += hasJsonContent(educationJson) ? 1 : 0;
        completed += hasJsonContent(experienceJson) ? 1 : 0;
        completed += hasJsonContent(skillsJson) ? 1 : 0;
        completed += isPresent(attachmentObjectKey) ? 1 : 0;
        return new CandidateResumeSummary(
                completed * 20,
                blankToDefault(rs.getString("updated_at_text"), ""),
                skillsSummary(skillsJson));
    }

    private Optional<ControlProfileRow> findControlProfile(long candidateId) {
        return jdbcTemplate.query(
                        "SELECT p.current_consent_id, p.consent_status, p.publishable_flag, "
                                + "s.id AS active_snapshot_id, "
                                + "DATE_FORMAT(p.updated_at, '%Y-%m-%dT%H:%i:%s') AS updated_at_text "
                                + "FROM candidate_control_profile p "
                                + "LEFT JOIN candidate_publish_snapshot s ON s.id = p.current_snapshot_id "
                                + "  AND s.candidate_id = p.candidate_id "
                                + "  AND s.status = 1 "
                                + "  AND s.publishable_flag = 1 "
                                + "  AND s.consent_status = 1 "
                                + "WHERE p.candidate_id = ? LIMIT 1",
                        (rs, rowNum) -> new ControlProfileRow(
                                rs.getObject("current_consent_id", Long.class),
                                rs.getInt("consent_status"),
                                rs.getInt("publishable_flag"),
                                rs.getObject("active_snapshot_id", Long.class),
                                blankToDefault(rs.getString("updated_at_text"), "")),
                        candidateId)
                .stream()
                .findFirst();
    }

    private Optional<LatestConsentRow> findLatestConsent(long candidateId) {
        return jdbcTemplate.query(
                        "SELECT id, consent_status, revoke_status, "
                                + "DATE_FORMAT(COALESCE(revoke_time, consent_time, updated_at), '%Y-%m-%dT%H:%i:%s') "
                                + "AS consent_time_text "
                                + "FROM candidate_consent "
                                + "WHERE candidate_id = ? "
                                + "ORDER BY id DESC LIMIT 1",
                        (rs, rowNum) -> new LatestConsentRow(
                                rs.getLong("id"),
                                rs.getInt("consent_status"),
                                rs.getInt("revoke_status"),
                                blankToDefault(rs.getString("consent_time_text"), "")),
                        candidateId)
                .stream()
                .findFirst();
    }

    private boolean isRevoked(
            Optional<ControlProfileRow> profile,
            Optional<LatestConsentRow> latestConsent
    ) {
        if (profile.isPresent() && profile.get().consentStatus() == 2) {
            return true;
        }
        return latestConsent
                .map(consent -> consent.consentStatus() == 2 || consent.revokeStatus() == 1)
                .orElse(false);
    }

    private Long consentId(
            Optional<ControlProfileRow> profile,
            Optional<LatestConsentRow> latestConsent
    ) {
        return profile
                .map(ControlProfileRow::currentConsentId)
                .orElseGet(() -> latestConsent.map(LatestConsentRow::consentId).orElse(null));
    }

    private String updatedAt(
            Optional<ControlProfileRow> profile,
            Optional<LatestConsentRow> latestConsent
    ) {
        return profile
                .map(ControlProfileRow::updatedAt)
                .filter(this::isPresent)
                .orElseGet(() -> latestConsent.map(LatestConsentRow::updatedAt).orElse(""));
    }

    private String notPublishableReason(
            Optional<ControlProfileRow> profile,
            Optional<LatestConsentRow> latestConsent
    ) {
        if (profile.isEmpty() && latestConsent.isEmpty()) {
            return "尚未提交同意";
        }
        if (profile.isEmpty()) {
            return "发布控制记录暂不可用";
        }
        ControlProfileRow row = profile.get();
        if (row.consentStatus() != 1) {
            return "尚未处于有效同意状态";
        }
        if (row.publishableFlag() != 1) {
            return "当前不可发布";
        }
        return "发布快照未生效";
    }

    private boolean hasJsonContent(String json) {
        if (!isPresent(json)) {
            return false;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.isObject() || node.isArray()) {
                return node.size() > 0;
            }
            if (node.isTextual()) {
                return isPresent(node.asText());
            }
            return !node.isNull() && !node.isMissingNode();
        } catch (Exception exception) {
            return true;
        }
    }

    private String skillsSummary(String skillsJson) {
        if (!isPresent(skillsJson)) {
            return "";
        }
        try {
            JsonNode skills = objectMapper.readTree(skillsJson);
            if (skills.isArray()) {
                return joinSkills(skills);
            }
            if (skills.isObject()) {
                JsonNode summary = skills.path("summary");
                if (summary.isTextual() && isPresent(summary.asText())) {
                    return limit(summary.asText());
                }
                JsonNode skillList = skills.path("skills");
                if (skillList.isArray()) {
                    return joinSkills(skillList);
                }
            }
            if (skills.isTextual()) {
                return limit(skills.asText());
            }
            return "";
        } catch (Exception exception) {
            return "";
        }
    }

    private String joinSkills(JsonNode skills) {
        List<String> values = new ArrayList<>();
        for (JsonNode skill : skills) {
            if (values.size() >= MAX_SKILL_COUNT) {
                break;
            }
            String value = skill.asText();
            if (isPresent(value)) {
                values.add(value.trim());
            }
        }
        return limit(String.join(" / ", values));
    }

    private String limit(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.length() <= 200) {
            return normalized;
        }
        return normalized.substring(0, 200);
    }

    private String blankToDefault(String value, String defaultValue) {
        return isPresent(value) ? value.trim() : defaultValue;
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private record LatestApplicationRow(Integer status, String jobTitle) {
    }

    private record ControlProfileRow(
            Long currentConsentId,
            int consentStatus,
            int publishableFlag,
            Long activeSnapshotId,
            String updatedAt
    ) {
    }

    private record LatestConsentRow(
            long consentId,
            int consentStatus,
            int revokeStatus,
            String updatedAt
    ) {
    }
}
