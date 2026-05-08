package cn.localtalent.backend.candidatecenter.infrastructure;

import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.ApplicationItemResponse;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.FavoriteItemResponse;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.NotificationItemResponse;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.ResumeResponse;
import cn.localtalent.backend.candidatecenter.api.CandidateCenterDtos.SubscriptionItemResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class CandidateClosureJdbcRepository {

    private static final int MAX_LIST_SIZE = 100;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CandidateClosureJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countActiveFavorites(long candidateId) {
        return count("SELECT COUNT(*) FROM candidate_job_favorite WHERE candidate_id = ? AND status = 1", candidateId);
    }

    public long countActiveSubscriptions(long candidateId) {
        return count("SELECT COUNT(*) FROM candidate_search_subscription WHERE candidate_id = ? AND status = 1", candidateId);
    }

    public long countUnreadNotifications(long candidateId) {
        return count("SELECT COUNT(*) FROM candidate_notification WHERE candidate_id = ? AND read_status = 0", candidateId);
    }

    public Optional<OnboardingState> findOnboardingState(long candidateId) {
        return jdbcTemplate.query(
                        "SELECT resume_id, onboarding_status, current_step, completion_score, "
                                + "DATE_FORMAT(completed_at, '%Y-%m-%dT%H:%i:%s') AS completed_at_text "
                                + "FROM candidate_resume_onboarding WHERE candidate_id = ? LIMIT 1",
                        (rs, rowNum) -> new OnboardingState(
                                rs.getObject("resume_id", Long.class),
                                rs.getString("onboarding_status"),
                                rs.getString("current_step"),
                                rs.getInt("completion_score"),
                                blankToDefault(rs.getString("completed_at_text"), "")),
                        candidateId)
                .stream()
                .findFirst();
    }

    public ResumeResponse latestResume(long candidateId) {
        return jdbcTemplate.query(
                        "SELECT id, resume_name, base_profile_json, education_json, experience_json, skills_json, "
                                + "attachment_object_key, DATE_FORMAT(updated_at, '%Y-%m-%dT%H:%i:%s') AS updated_at_text "
                                + "FROM candidate_resume WHERE candidate_id = ? AND status = 1 "
                                + "ORDER BY id DESC LIMIT 1",
                        (rs, rowNum) -> toResume(
                                rs.getLong("id"),
                                rs.getString("resume_name"),
                                rs.getString("base_profile_json"),
                                rs.getString("education_json"),
                                rs.getString("experience_json"),
                                rs.getString("skills_json"),
                                rs.getString("attachment_object_key"),
                                rs.getString("updated_at_text")),
                        candidateId)
                .stream()
                .findFirst()
                .orElse(emptyResume());
    }

    public long saveResume(
            long candidateId,
            String resumeName,
            String baseProfileJson,
            String educationJson,
            String experienceJson,
            String skillsJson
    ) {
        Long resumeId = latestResumeId(candidateId).orElse(null);
        if (resumeId == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO candidate_resume "
                                + "(candidate_id, resume_name, base_profile_json, education_json, experience_json, "
                                + "skills_json, status) VALUES (?, ?, ?, ?, ?, ?, 1)",
                        Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, candidateId);
                statement.setString(2, resumeName);
                statement.setString(3, baseProfileJson);
                statement.setString(4, educationJson);
                statement.setString(5, experienceJson);
                statement.setString(6, skillsJson);
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("resume id was not generated");
            }
            return key.longValue();
        }

        jdbcTemplate.update(
                "UPDATE candidate_resume SET resume_name = ?, base_profile_json = ?, education_json = ?, "
                        + "experience_json = ?, skills_json = ?, status = 1, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE id = ? AND candidate_id = ?",
                resumeName,
                baseProfileJson,
                educationJson,
                experienceJson,
                skillsJson,
                resumeId,
                candidateId);
        return resumeId;
    }

    public long upsertOnboardingState(
            long candidateId,
            long resumeId,
            String onboardingStatus,
            String currentStep,
            int completionScore
    ) {
        jdbcTemplate.update(
                "INSERT INTO candidate_resume_onboarding "
                        + "(candidate_id, resume_id, onboarding_status, current_step, completion_score, completed_at) "
                        + "VALUES (?, ?, ?, ?, ?, CASE WHEN ? = 'completed' THEN CURRENT_TIMESTAMP ELSE NULL END) "
                        + "ON DUPLICATE KEY UPDATE resume_id = ?, onboarding_status = ?, current_step = ?, "
                        + "completion_score = ?, completed_at = CASE WHEN ? = 'completed' "
                        + "THEN COALESCE(completed_at, CURRENT_TIMESTAMP) ELSE NULL END, "
                        + "updated_at = CURRENT_TIMESTAMP, version = version + 1",
                candidateId,
                resumeId,
                onboardingStatus,
                currentStep,
                completionScore,
                onboardingStatus,
                resumeId,
                onboardingStatus,
                currentStep,
                completionScore,
                onboardingStatus);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM candidate_resume_onboarding WHERE candidate_id = ?",
                Long.class,
                candidateId);
        if (id == null) {
            throw new IllegalStateException("candidate resume onboarding id was not found");
        }
        return id;
    }

    public List<ApplicationItemResponse> listApplications(long candidateId, int page, int size) {
        int normalizedSize = normalizeSize(size);
        return jdbcTemplate.query(
                "SELECT a.id, a.job_id, j.title AS job_title, c.company_name, a.application_status, "
                        + "DATE_FORMAT(a.apply_time, '%Y-%m-%dT%H:%i:%s') AS apply_time_text "
                        + "FROM job_application a "
                        + "JOIN job_post j ON j.id = a.job_id "
                        + "JOIN company c ON c.id = j.company_id "
                        + "WHERE a.candidate_id = ? "
                        + "ORDER BY a.apply_time DESC, a.id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new ApplicationItemResponse(
                        rs.getLong("id"),
                        rs.getLong("job_id"),
                        rs.getString("job_title"),
                        rs.getString("company_name"),
                        rs.getInt("application_status"),
                        applicationStatusLabel(rs.getInt("application_status")),
                        blankToDefault(rs.getString("apply_time_text"), "")),
                candidateId,
                normalizedSize,
                offset(page, normalizedSize));
    }

    public long countApplications(long candidateId) {
        return count("SELECT COUNT(*) FROM job_application WHERE candidate_id = ?", candidateId);
    }

    public boolean isVisibleJob(long jobId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM job_post j JOIN company c ON c.id = j.company_id "
                        + "WHERE j.id = ? AND j.status = 2 AND j.audit_status = 2 AND c.auth_status = 2",
                Integer.class,
                jobId);
        return count != null && count > 0;
    }

    public long upsertFavorite(long candidateId, long jobId) {
        jdbcTemplate.update(
                "INSERT INTO candidate_job_favorite (candidate_id, job_id, status) VALUES (?, ?, 1) "
                        + "ON DUPLICATE KEY UPDATE status = 1, updated_at = CURRENT_TIMESTAMP",
                candidateId,
                jobId);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM candidate_job_favorite WHERE candidate_id = ? AND job_id = ?",
                Long.class,
                candidateId,
                jobId);
    }

    public boolean cancelFavorite(long candidateId, long favoriteId) {
        return jdbcTemplate.update(
                "UPDATE candidate_job_favorite SET status = 0, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE id = ? AND candidate_id = ?",
                favoriteId,
                candidateId) > 0;
    }

    public List<FavoriteItemResponse> listFavorites(long candidateId, int page, int size) {
        int normalizedSize = normalizeSize(size);
        return jdbcTemplate.query(
                "SELECT f.id, f.job_id, j.title AS job_title, c.company_name, j.city_code, j.category_code, "
                        + "DATE_FORMAT(f.created_at, '%Y-%m-%dT%H:%i:%s') AS created_at_text "
                        + "FROM candidate_job_favorite f "
                        + "JOIN job_post j ON j.id = f.job_id "
                        + "JOIN company c ON c.id = j.company_id "
                        + "WHERE f.candidate_id = ? AND f.status = 1 "
                        + "ORDER BY f.updated_at DESC, f.id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new FavoriteItemResponse(
                        rs.getLong("id"),
                        rs.getLong("job_id"),
                        rs.getString("job_title"),
                        rs.getString("company_name"),
                        blankToDefault(rs.getString("city_code"), ""),
                        blankToDefault(rs.getString("category_code"), ""),
                        "active",
                        blankToDefault(rs.getString("created_at_text"), "")),
                candidateId,
                normalizedSize,
                offset(page, normalizedSize));
    }

    public long createSubscription(
            long candidateId,
            String subscriptionName,
            String keyword,
            String cityCode,
            String categoryCode,
            Integer salaryMin,
            Integer salaryMax
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO candidate_search_subscription "
                            + "(candidate_id, subscription_name, keyword, city_code, category_code, salary_min, salary_max, status) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, 1)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, candidateId);
            statement.setString(2, subscriptionName);
            statement.setString(3, keyword);
            statement.setString(4, cityCode);
            statement.setString(5, categoryCode);
            statement.setObject(6, salaryMin);
            statement.setObject(7, salaryMax);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("subscription id was not generated");
        }
        return key.longValue();
    }

    public boolean cancelSubscription(long candidateId, long subscriptionId) {
        return jdbcTemplate.update(
                "UPDATE candidate_search_subscription SET status = 0, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE id = ? AND candidate_id = ?",
                subscriptionId,
                candidateId) > 0;
    }

    public List<SubscriptionItemResponse> listSubscriptions(long candidateId, int page, int size) {
        int normalizedSize = normalizeSize(size);
        return jdbcTemplate.query(
                "SELECT id, subscription_name, keyword, city_code, category_code, salary_min, salary_max, status, "
                        + "DATE_FORMAT(updated_at, '%Y-%m-%dT%H:%i:%s') AS updated_at_text "
                        + "FROM candidate_search_subscription WHERE candidate_id = ? AND status = 1 "
                        + "ORDER BY updated_at DESC, id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new SubscriptionItemResponse(
                        rs.getLong("id"),
                        rs.getString("subscription_name"),
                        blankToDefault(rs.getString("keyword"), ""),
                        blankToDefault(rs.getString("city_code"), ""),
                        blankToDefault(rs.getString("category_code"), ""),
                        rs.getObject("salary_min", Integer.class),
                        rs.getObject("salary_max", Integer.class),
                        rs.getInt("status") == 1 ? "active" : "cancelled",
                        blankToDefault(rs.getString("updated_at_text"), "")),
                candidateId,
                normalizedSize,
                offset(page, normalizedSize));
    }

    public long createNotification(
            long candidateId,
            String notificationType,
            String title,
            String contentSummary,
            String bizType,
            long bizId
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO candidate_notification "
                            + "(candidate_id, notification_type, title, content_summary, biz_type, biz_id, read_status) "
                            + "VALUES (?, ?, ?, ?, ?, ?, 0)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, candidateId);
            statement.setString(2, notificationType);
            statement.setString(3, title);
            statement.setString(4, contentSummary);
            statement.setString(5, bizType);
            statement.setLong(6, bizId);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("notification id was not generated");
        }
        return key.longValue();
    }

    public boolean markNotificationRead(long candidateId, long notificationId) {
        return jdbcTemplate.update(
                "UPDATE candidate_notification SET read_status = 1, read_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE id = ? AND candidate_id = ?",
                notificationId,
                candidateId) > 0;
    }

    public List<NotificationItemResponse> listNotifications(long candidateId, int page, int size) {
        int normalizedSize = normalizeSize(size);
        return jdbcTemplate.query(
                "SELECT id, notification_type, title, content_summary, read_status, "
                        + "DATE_FORMAT(created_at, '%Y-%m-%dT%H:%i:%s') AS created_at_text "
                        + "FROM candidate_notification WHERE candidate_id = ? "
                        + "ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new NotificationItemResponse(
                        rs.getLong("id"),
                        rs.getString("notification_type"),
                        rs.getString("title"),
                        blankToDefault(rs.getString("content_summary"), ""),
                        rs.getInt("read_status") == 1 ? "read" : "unread",
                        blankToDefault(rs.getString("created_at_text"), "")),
                candidateId,
                normalizedSize,
                offset(page, normalizedSize));
    }

    private Optional<Long> latestResumeId(long candidateId) {
        return jdbcTemplate.query(
                        "SELECT id FROM candidate_resume WHERE candidate_id = ? AND status = 1 ORDER BY id DESC LIMIT 1",
                        (rs, rowNum) -> rs.getLong("id"),
                        candidateId)
                .stream()
                .findFirst();
    }

    private ResumeResponse toResume(
            Long resumeId,
            String resumeName,
            String baseProfileJson,
            String educationJson,
            String experienceJson,
            String skillsJson,
            String attachmentObjectKey,
            String updatedAt
    ) {
        CandidateCenterDtos.BaseProfileResponse baseProfile = baseProfile(baseProfileJson);
        List<String> education = textList(educationJson);
        List<String> experience = textList(experienceJson);
        List<String> skills = textList(skillsJson);
        List<CandidateCenterDtos.WorkExperienceResponse> workExperience = workExperienceList(experienceJson);
        List<CandidateCenterDtos.EducationExperienceResponse> educationExperience = educationExperienceList(educationJson);
        String selfDescription = selfDescription(baseProfile);
        int completed = 0;
        completed += isPresent(baseProfile.displayName())
                || isPresent(baseProfile.cityCode())
                || isPresent(baseProfile.categoryCode())
                || isPresent(baseProfile.contactPhone()) ? 1 : 0;
        completed += (!baseProfile.expectedPositions().isEmpty() && !baseProfile.expectedCities().isEmpty())
                || (isPresent(baseProfile.cityCode()) && isPresent(baseProfile.categoryCode())) ? 1 : 0;
        completed += education.isEmpty() && educationExperience.isEmpty() ? 0 : 1;
        completed += experience.isEmpty() && workExperience.isEmpty() ? 0 : 1;
        completed += isPresent(selfDescription) || !skills.isEmpty() || isPresent(attachmentObjectKey) ? 1 : 0;
        int completion = completed * 20;
        return new ResumeResponse(
                resumeId,
                completion >= 80 ? "complete" : "needs_completion",
                completion,
                blankToDefault(updatedAt, ""),
                blankToDefault(resumeName, "我的简历"),
                baseProfile,
                education,
                experience,
                skills,
                workExperience,
                educationExperience,
                selfDescription,
                isPresent(attachmentObjectKey));
    }

    private ResumeResponse emptyResume() {
        return new ResumeResponse(
                null,
                "draft",
                0,
                "",
                "我的简历",
                new CandidateCenterDtos.BaseProfileResponse(
                        "", "", "", null, "", "", "", "", "", false, "", "", false, List.of(), "", List.of(), ""),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "",
                false);
    }

    private CandidateCenterDtos.BaseProfileResponse baseProfile(String json) {
        JsonNode node = readJson(json);
        return new CandidateCenterDtos.BaseProfileResponse(
                text(node, "display_name"),
                text(node, "city_code"),
                text(node, "category_code"),
                integer(node, "experience_years"),
                text(node, "summary"),
                text(node, "gender"),
                text(node, "birth_date"),
                text(node, "highest_education"),
                text(node, "start_work_date"),
                bool(node, "no_experience"),
                text(node, "contact_phone"),
                text(node, "contact_wechat"),
                bool(node, "wechat_same_as_phone"),
                textArray(node.path("expected_positions")),
                text(node, "expected_salary"),
                textArray(node.path("expected_cities")),
                text(node, "job_status"));
    }

    private List<String> textList(String json) {
        if (!isPresent(json)) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.isArray()) {
                List<String> values = new ArrayList<>();
                node.forEach(item -> {
                    if (item.isTextual() && isPresent(item.asText())) {
                        values.add(item.asText().trim());
                    } else if (item.isObject()) {
                        String compact = compactObject(item);
                        if (isPresent(compact)) {
                            values.add(compact);
                        }
                    }
                });
                return values.stream().limit(20).toList();
            }
            if (node.isTextual() && isPresent(node.asText())) {
                return List.of(node.asText().trim());
            }
            return List.of();
        } catch (Exception exception) {
            return List.of();
        }
    }

    private List<CandidateCenterDtos.WorkExperienceResponse> workExperienceList(String json) {
        JsonNode node = readJson(json);
        if (!node.isArray()) {
            return List.of();
        }
        List<CandidateCenterDtos.WorkExperienceResponse> values = new ArrayList<>();
        node.forEach(item -> {
            if (item.isObject()) {
                values.add(new CandidateCenterDtos.WorkExperienceResponse(
                        text(item, "company_name"),
                        text(item, "position_name"),
                        text(item, "start_date"),
                        text(item, "end_date"),
                        bool(item, "ongoing"),
                        text(item, "responsibility")));
            }
        });
        return values.stream().limit(MAX_LIST_SIZE).toList();
    }

    private List<CandidateCenterDtos.EducationExperienceResponse> educationExperienceList(String json) {
        JsonNode node = readJson(json);
        if (!node.isArray()) {
            return List.of();
        }
        List<CandidateCenterDtos.EducationExperienceResponse> values = new ArrayList<>();
        node.forEach(item -> {
            if (item.isObject()) {
                values.add(new CandidateCenterDtos.EducationExperienceResponse(
                        text(item, "school_name"),
                        text(item, "major_name"),
                        text(item, "start_date"),
                        text(item, "end_date"),
                        bool(item, "ongoing"),
                        text(item, "degree")));
            }
        });
        return values.stream().limit(MAX_LIST_SIZE).toList();
    }

    private String selfDescription(CandidateCenterDtos.BaseProfileResponse baseProfile) {
        return isPresent(baseProfile.summary()) ? baseProfile.summary() : "";
    }

    private String compactObject(JsonNode node) {
        List<String> parts = textArray(node);
        return String.join(" / ", parts);
    }

    private JsonNode readJson(String json) {
        if (!isPresent(json)) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private boolean bool(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isInt()) {
            return value.asInt() == 1;
        }
        if (value.isTextual()) {
            return "true".equalsIgnoreCase(value.asText()) || "1".equals(value.asText());
        }
        return false;
    }

    private List<String> textArray(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return List.of();
        }
        if (node.isArray()) {
            List<String> values = new ArrayList<>();
            node.forEach(item -> {
                if (item.isTextual() && isPresent(item.asText())) {
                    values.add(item.asText().trim());
                }
            });
            return values.stream().limit(MAX_LIST_SIZE).toList();
        }
        if (node.isObject()) {
            List<String> values = new ArrayList<>();
            node.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                if ((value.isTextual() || value.isNumber() || value.isBoolean()) && isPresent(value.asText())) {
                    values.add(value.asText().trim());
                }
            });
            return values.stream().limit(MAX_LIST_SIZE).toList();
        }
        return node.isTextual() && isPresent(node.asText()) ? List.of(node.asText().trim()) : List.of();
    }

    private Integer integer(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isInt() || value.isLong()) {
            return value.asInt();
        }
        if (value.isTextual() && isPresent(value.asText())) {
            try {
                return Integer.parseInt(value.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String applicationStatusLabel(int status) {
        return switch (status) {
            case 0 -> "已投递";
            case 1 -> "待筛选";
            case 2 -> "邀约面试";
            case 3 -> "已签到";
            case 4 -> "已结束";
            case 5 -> "已淘汰";
            default -> "状态" + status;
        };
    }

    private long count(String sql, long candidateId) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class, candidateId);
        return count == null ? 0 : count;
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), MAX_LIST_SIZE);
    }

    private int offset(int page, int size) {
        return (Math.max(page, 1) - 1) * size;
    }

    private String blankToDefault(String value, String defaultValue) {
        return isPresent(value) ? value.trim() : defaultValue;
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    public record OnboardingState(
            Long resumeId,
            String onboardingStatus,
            String currentStep,
            int completionScore,
            String completedAt
    ) {
    }
}
