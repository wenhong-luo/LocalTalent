package cn.localtalent.backend.company.workbench.infrastructure;

import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.ApplicationItemResponse;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.CompanyProfileResponse;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.InterviewSessionItemResponse;
import cn.localtalent.backend.company.workbench.api.CompanyWorkbenchDtos.WorkbenchStatsResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CompanyWorkbenchJdbcRepository {

    private static final int APPLICATION_STATUS_APPLIED = 0;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CompanyWorkbenchJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<CompanyProfileResponse> findProfile(long companyId) {
        return jdbcTemplate.query(
                "SELECT id, company_name, industry_code, nature_code, scale_code, city_code, address, "
                        + "company_profile, auth_status, auth_reject_reason, certification_material_summary_json, updated_at "
                        + "FROM company WHERE id = ? LIMIT 1",
                (rs, rowNum) -> profile(rs),
                companyId).stream().findFirst();
    }

    public int authStatus(long companyId) {
        Integer status = jdbcTemplate.queryForObject(
                "SELECT auth_status FROM company WHERE id = ?",
                Integer.class,
                companyId);
        return status == null ? 0 : status;
    }

    public void updateProfile(
            long companyId,
            String companyName,
            String industryCode,
            String natureCode,
            String scaleCode,
            String cityCode,
            String address,
            String companyProfile
    ) {
        jdbcTemplate.update(
                "UPDATE company SET company_name = ?, industry_code = ?, nature_code = ?, scale_code = ?, "
                        + "city_code = ?, address = ?, company_profile = ? WHERE id = ?",
                companyName,
                industryCode,
                natureCode,
                scaleCode,
                cityCode,
                address,
                companyProfile,
                companyId);
    }

    public void submitCertification(
            long companyId,
            String companyName,
            String licenseNo,
            String industryCode,
            String natureCode,
            String scaleCode,
            String cityCode,
            String address,
            String companyProfile,
            String materialSummaryJson
    ) {
        jdbcTemplate.update(
                "UPDATE company SET company_name = ?, license_no = ?, industry_code = ?, nature_code = ?, scale_code = ?, "
                        + "city_code = ?, address = ?, company_profile = ?, certification_material_summary_json = CAST(? AS JSON), "
                        + "auth_status = 1, auth_reject_reason = NULL, auth_review_user_id = NULL, auth_review_time = NULL, "
                        + "auth_submit_time = CURRENT_TIMESTAMP WHERE id = ?",
                companyName,
                licenseNo,
                industryCode,
                natureCode,
                scaleCode,
                cityCode,
                address,
                companyProfile,
                materialSummaryJson,
                companyId);
    }

    public WorkbenchStatsResponse stats(long companyId) {
        return new WorkbenchStatsResponse(
                count("SELECT COUNT(*) FROM job_post WHERE company_id = ?", companyId),
                count("SELECT COUNT(*) FROM job_application a JOIN job_post j ON j.id = a.job_id WHERE j.company_id = ?", companyId),
                count("SELECT COUNT(*) FROM job_application a JOIN job_post j ON j.id = a.job_id "
                        + "WHERE j.company_id = ? AND a.application_status = ?", companyId, APPLICATION_STATUS_APPLIED),
                count("SELECT COUNT(*) FROM interview_session WHERE company_id = ?", companyId),
                count("SELECT COUNT(*) FROM export_apply WHERE company_id = ?", companyId));
    }

    public List<ApplicationItemResponse> listApplications(long companyId, Long jobId, Integer status, int limit, int offset) {
        QueryParts query = applicationQuery(companyId, jobId, status, false);
        query.args().add(limit);
        query.args().add(offset);
        return jdbcTemplate.query(
                query.sql() + " ORDER BY a.apply_time DESC, a.id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> application(rs),
                query.args().toArray());
    }

    public long countApplications(long companyId, Long jobId, Integer status) {
        QueryParts query = applicationQuery(companyId, jobId, status, true);
        return count(query.sql(), query.args().toArray());
    }

    public Optional<ApplicationItemResponse> findApplication(long companyId, long applicationId) {
        return jdbcTemplate.query(
                applicationSelect()
                        + " WHERE j.company_id = ? AND a.id = ? LIMIT 1",
                (rs, rowNum) -> application(rs),
                companyId,
                applicationId).stream().findFirst();
    }

    public boolean updateStage(long companyId, long applicationId, int status, String note, long changedBy) {
        return jdbcTemplate.update(
                "UPDATE job_application a JOIN job_post j ON j.id = a.job_id "
                        + "SET a.application_status = ?, a.company_stage_note = ?, a.stage_changed_by = ?, "
                        + "a.stage_changed_at = CURRENT_TIMESTAMP "
                        + "WHERE a.id = ? AND j.company_id = ?",
                status,
                note,
                changedBy,
                applicationId,
                companyId) == 1;
    }

    public List<InterviewSessionItemResponse> listInterviewSessions(long companyId, int limit, int offset) {
        return jdbcTemplate.query(
                "SELECT s.id, s.application_id, s.job_id, j.title AS job_title, s.status, "
                        + "s.session_name, s.session_time, s.location "
                        + "FROM interview_session s JOIN job_post j ON j.id = s.job_id "
                        + "WHERE s.company_id = ? ORDER BY s.session_time DESC, s.id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> session(rs),
                companyId,
                limit,
                offset);
    }

    public long countInterviewSessions(long companyId) {
        return count("SELECT COUNT(*) FROM interview_session WHERE company_id = ?", companyId);
    }

    private QueryParts applicationQuery(long companyId, Long jobId, Integer status, boolean countOnly) {
        StringBuilder sql = new StringBuilder(countOnly ? "SELECT COUNT(*) " : applicationSelect());
        if (countOnly) {
            sql.append("FROM job_application a JOIN job_post j ON j.id = a.job_id ");
        }
        sql.append(" WHERE j.company_id = ?");
        List<Object> args = new java.util.ArrayList<>();
        args.add(companyId);
        if (jobId != null) {
            sql.append(" AND a.job_id = ?");
            args.add(jobId);
        }
        if (status != null) {
            sql.append(" AND a.application_status = ?");
            args.add(status);
        }
        return new QueryParts(sql.toString(), args);
    }

    private String applicationSelect() {
        return "SELECT a.id, a.job_id, j.title AS job_title, a.application_status, a.apply_time, "
                + "a.company_stage_note, a.stage_changed_at, "
                + "JSON_UNQUOTE(JSON_EXTRACT(cr.base_profile_json, '$.display_name')) AS display_name, "
                + "JSON_UNQUOTE(JSON_EXTRACT(cr.base_profile_json, '$.city_code')) AS candidate_city_code, "
                + "JSON_EXTRACT(cr.base_profile_json, '$.experience_years') AS experience_years_json, "
                + "cr.skills_json, cr.attachment_object_key "
                + "FROM job_application a "
                + "JOIN job_post j ON j.id = a.job_id "
                + "LEFT JOIN candidate_resume cr ON cr.id = a.resume_id";
    }

    private ApplicationItemResponse application(ResultSet rs) throws SQLException {
        return new ApplicationItemResponse(
                rs.getLong("id"),
                rs.getLong("job_id"),
                rs.getString("job_title"),
                rs.getInt("application_status"),
                statusLabel(rs.getInt("application_status")),
                timestamp(rs, "apply_time"),
                maskName(rs.getString("display_name")),
                rs.getString("candidate_city_code"),
                skillsSummary(rs.getString("skills_json")),
                experienceYears(rs.getString("experience_years_json")),
                rs.getString("attachment_object_key") != null && !rs.getString("attachment_object_key").isBlank(),
                rs.getString("company_stage_note"),
                timestamp(rs, "stage_changed_at"));
    }

    private InterviewSessionItemResponse session(ResultSet rs) throws SQLException {
        return new InterviewSessionItemResponse(
                rs.getLong("id"),
                rs.getObject("application_id", Long.class),
                rs.getLong("job_id"),
                rs.getString("job_title"),
                rs.getInt("status"),
                rs.getString("session_name"),
                timestamp(rs, "session_time"),
                rs.getString("location"));
    }

    private CompanyProfileResponse profile(ResultSet rs) throws SQLException {
        return new CompanyProfileResponse(
                rs.getLong("id"),
                rs.getString("company_name"),
                rs.getString("industry_code"),
                rs.getString("nature_code"),
                rs.getString("scale_code"),
                rs.getString("city_code"),
                rs.getString("address"),
                rs.getString("company_profile"),
                rs.getInt("auth_status"),
                rs.getString("auth_reject_reason"),
                jsonMap(rs.getString("certification_material_summary_json")),
                timestamp(rs, "updated_at"));
    }

    private long count(String sql, Object... args) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class, args);
        return count == null ? 0 : count;
    }

    private LocalDateTime timestamp(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column) == null ? null : rs.getTimestamp(column).toLocalDateTime();
    }

    private Map<String, Object> jsonMap(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception exception) {
            return Collections.emptyMap();
        }
    }

    private String maskName(String value) {
        if (value == null || value.isBlank()) {
            return "求职者";
        }
        String normalized = value.trim();
        if (normalized.length() <= 1) {
            return normalized + "*";
        }
        return normalized.charAt(0) + "*";
    }

    private Integer experienceYears(String raw) {
        if (raw == null || raw.isBlank() || "null".equals(raw)) {
            return null;
        }
        try {
            return Integer.parseInt(raw.replace("\"", ""));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String skillsSummary(String skillsJson) {
        if (skillsJson == null || skillsJson.isBlank()) {
            return "";
        }
        try {
            List<String> skills = objectMapper.readValue(skillsJson, new TypeReference<List<String>>() {});
            return String.join(" / ", skills.stream().filter(skill -> skill != null && !skill.isBlank()).limit(5).toList());
        } catch (Exception exception) {
            return "";
        }
    }

    private String statusLabel(int status) {
        return switch (status) {
            case 1 -> "已查看";
            case 2 -> "邀约面试";
            case 3 -> "已签到";
            case 4 -> "已归档";
            case 5 -> "已拒绝";
            default -> "已投递";
        };
    }

    private record QueryParts(String sql, List<Object> args) {
    }
}
