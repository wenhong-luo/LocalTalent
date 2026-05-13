package cn.localtalent.backend.job.infrastructure;

import cn.localtalent.backend.job.api.JobCreateRequest;
import cn.localtalent.backend.job.api.JobUpdateRequest;
import cn.localtalent.backend.job.api.PortalJobSearchCriteria;
import cn.localtalent.backend.common.json.AuditJsonMapper;
import cn.localtalent.backend.job.domain.JobPostRow;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JobJdbcRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = AuditJsonMapper.create();

    public JobJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long create(long companyId, JobCreateRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO job_post "
                            + "(company_id, source_type, title, job_nature_code, category_code, category_name, "
                            + "experience_code, education_code, recruit_count, city_code, work_region_path, address, "
                            + "salary_min, salary_max, salary_negotiable, job_desc, welfare_codes, department_name, "
                            + "age_min, age_max, age_unlimited, recruitment_time_code, contact_mode, contact_name, "
                            + "contact_mobile, contact_phone, contact_email, contact_wechat, contact_hidden, "
                            + "notify_enabled, resume_subscription_enabled, status, audit_status, status_changed_at) "
                            + "VALUES (?, 1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, CURRENT_TIMESTAMP)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, companyId);
            statement.setString(2, request.title());
            statement.setString(3, request.jobNatureCode());
            statement.setString(4, request.categoryCode());
            statement.setString(5, request.categoryName());
            statement.setString(6, request.experienceCode());
            statement.setString(7, request.educationCode());
            setNullableInt(statement, 8, request.recruitCount());
            statement.setString(9, request.cityCode());
            statement.setString(10, request.workRegionPath());
            statement.setString(11, request.address());
            setNullableInt(statement, 12, request.salaryMin());
            setNullableInt(statement, 13, request.salaryMax());
            statement.setBoolean(14, Boolean.TRUE.equals(request.salaryNegotiable()));
            statement.setString(15, request.jobDesc());
            statement.setString(16, toJsonArray(request.welfareCodes()));
            statement.setString(17, request.departmentName());
            setNullableInt(statement, 18, request.ageMin());
            setNullableInt(statement, 19, request.ageMax());
            statement.setBoolean(20, Boolean.TRUE.equals(request.ageUnlimited()));
            statement.setString(21, request.recruitmentTimeCode());
            statement.setString(22, request.contactMode());
            statement.setString(23, request.contactName());
            statement.setString(24, request.contactMobile());
            statement.setString(25, request.contactPhone());
            statement.setString(26, request.contactEmail());
            statement.setString(27, request.contactWechat());
            statement.setBoolean(28, !Boolean.FALSE.equals(request.contactHidden()));
            statement.setBoolean(29, Boolean.TRUE.equals(request.notifyEnabled()));
            statement.setBoolean(30, Boolean.TRUE.equals(request.resumeSubscriptionEnabled()));
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("job id was not generated");
        }
        return key.longValue();
    }

    public Optional<JobPostRow> findById(long jobId) {
        return jdbcTemplate.query(baseSelect() + " WHERE j.id = ? LIMIT 1",
                (rs, rowNum) -> row(rs),
                jobId).stream().findFirst();
    }

    public List<JobPostRow> listByCompany(long companyId, int limit, int offset) {
        return jdbcTemplate.query(
                baseSelect() + " WHERE j.company_id = ? AND j.deleted_at IS NULL ORDER BY j.updated_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> row(rs),
                companyId,
                limit,
                offset);
    }

    public long countByCompany(long companyId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM job_post WHERE company_id = ? AND deleted_at IS NULL",
                Integer.class,
                companyId);
        return count == null ? 0 : count;
    }

    public List<JobPostRow> listDeletedByCompany(long companyId, int limit, int offset) {
        return jdbcTemplate.query(
                baseSelect() + " WHERE j.company_id = ? AND j.deleted_at IS NOT NULL ORDER BY j.deleted_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> row(rs),
                companyId,
                limit,
                offset);
    }

    public long countDeletedByCompany(long companyId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM job_post WHERE company_id = ? AND deleted_at IS NOT NULL",
                Integer.class,
                companyId);
        return count == null ? 0 : count;
    }

    public void update(long jobId, JobUpdateRequest request) {
        jdbcTemplate.update(
                "UPDATE job_post SET title = ?, job_nature_code = ?, category_code = ?, category_name = ?, "
                        + "experience_code = ?, education_code = ?, recruit_count = ?, city_code = ?, "
                        + "work_region_path = ?, address = ?, salary_min = ?, salary_max = ?, salary_negotiable = ?, "
                        + "job_desc = ?, welfare_codes = CAST(? AS JSON), department_name = ?, age_min = ?, age_max = ?, "
                        + "age_unlimited = ?, recruitment_time_code = ?, contact_mode = ?, contact_name = ?, "
                        + "contact_mobile = ?, contact_phone = ?, contact_email = ?, contact_wechat = ?, contact_hidden = ?, "
                        + "notify_enabled = ?, resume_subscription_enabled = ?, status = 1, audit_status = 1, "
                        + "review_memo = NULL, reject_reason = NULL, "
                        + "review_user_id = NULL, review_time = NULL, published_at = NULL, status_changed_at = CURRENT_TIMESTAMP "
                        + "WHERE id = ? AND deleted_at IS NULL",
                request.title(),
                request.jobNatureCode(),
                request.categoryCode(),
                request.categoryName(),
                request.experienceCode(),
                request.educationCode(),
                request.recruitCount(),
                request.cityCode(),
                request.workRegionPath(),
                request.address(),
                request.salaryMin(),
                request.salaryMax(),
                Boolean.TRUE.equals(request.salaryNegotiable()),
                request.jobDesc(),
                toJsonArray(request.welfareCodes()),
                request.departmentName(),
                request.ageMin(),
                request.ageMax(),
                Boolean.TRUE.equals(request.ageUnlimited()),
                request.recruitmentTimeCode(),
                request.contactMode(),
                request.contactName(),
                request.contactMobile(),
                request.contactPhone(),
                request.contactEmail(),
                request.contactWechat(),
                !Boolean.FALSE.equals(request.contactHidden()),
                Boolean.TRUE.equals(request.notifyEnabled()),
                Boolean.TRUE.equals(request.resumeSubscriptionEnabled()),
                jobId);
    }

    public void submitReview(long jobId) {
        jdbcTemplate.update(
                "UPDATE job_post SET status = 1, audit_status = 1, review_memo = NULL, reject_reason = NULL, "
                        + "review_user_id = NULL, review_time = NULL, published_at = NULL, offline_reason = NULL, "
                        + "status_changed_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL",
                jobId);
    }

    public void offline(long jobId, String reason) {
        jdbcTemplate.update(
                "UPDATE job_post SET status = 3, offline_reason = ?, published_at = NULL, "
                        + "status_changed_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL",
                reason,
                jobId);
    }

    public void softDelete(long jobId, long deletedBy, String reason) {
        jdbcTemplate.update(
                "UPDATE job_post SET status = 3, published_at = NULL, deleted_at = COALESCE(deleted_at, CURRENT_TIMESTAMP), "
                        + "deleted_by = COALESCE(deleted_by, ?), delete_reason = COALESCE(delete_reason, ?), "
                        + "status_changed_at = CURRENT_TIMESTAMP WHERE id = ?",
                deletedBy,
                reason,
                jobId);
    }

    public void restoreDraft(long jobId) {
        jdbcTemplate.update(
                "UPDATE job_post SET status = 1, audit_status = 1, review_memo = NULL, reject_reason = NULL, "
                        + "review_user_id = NULL, review_time = NULL, published_at = NULL, offline_reason = NULL, "
                        + "deleted_at = NULL, deleted_by = NULL, delete_reason = NULL, "
                        + "status_changed_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NOT NULL",
                jobId);
    }

    public void review(long jobId, int auditStatus, int status, String memo, String rejectReason, long reviewerId) {
        jdbcTemplate.update(
                "UPDATE job_post SET audit_status = ?, status = ?, review_memo = ?, reject_reason = ?, "
                        + "review_user_id = ?, review_time = CURRENT_TIMESTAMP, "
                        + "published_at = CASE WHEN ? = 2 THEN CURRENT_TIMESTAMP ELSE NULL END, "
                        + "offline_reason = CASE WHEN ? = 2 THEN NULL ELSE offline_reason END, "
                        + "status_changed_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL",
                auditStatus,
                status,
                memo,
                rejectReason,
                reviewerId,
                status,
                status,
                jobId);
    }

    public List<JobPostRow> listForReview(Integer auditStatus, int limit, int offset) {
        if (auditStatus == null) {
            return jdbcTemplate.query(
                    baseSelect() + " WHERE j.deleted_at IS NULL ORDER BY j.updated_at DESC LIMIT ? OFFSET ?",
                    (rs, rowNum) -> row(rs),
                    limit,
                    offset);
        }
        return jdbcTemplate.query(
                baseSelect() + " WHERE j.deleted_at IS NULL AND j.audit_status = ? ORDER BY j.updated_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> row(rs),
                auditStatus,
                limit,
                offset);
    }

    public long countForReview(Integer auditStatus) {
        Integer count;
        if (auditStatus == null) {
            count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM job_post WHERE deleted_at IS NULL", Integer.class);
        } else {
            count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM job_post WHERE deleted_at IS NULL AND audit_status = ?",
                    Integer.class,
                    auditStatus);
        }
        return count == null ? 0 : count;
    }

    public List<JobPostRow> listVisible(PortalJobSearchCriteria criteria, int limit, int offset) {
        QueryParts query = visibleQuery(criteria, false);
        query.args().add(limit);
        query.args().add(offset);
        return jdbcTemplate.query(
                query.sql() + visibleOrderBy(criteria == null ? null : criteria.sort()) + " LIMIT ? OFFSET ?",
                (rs, rowNum) -> row(rs),
                query.args().toArray());
    }

    public long countVisible(PortalJobSearchCriteria criteria) {
        QueryParts query = visibleQuery(criteria, true);
        Integer count = jdbcTemplate.queryForObject(query.sql(), Integer.class, query.args().toArray());
        return count == null ? 0 : count;
    }

    public Optional<JobPostRow> findVisibleById(long jobId) {
        return jdbcTemplate.query(
                baseSelect()
                        + " WHERE j.id = ? AND j.deleted_at IS NULL AND j.status = 2 AND j.audit_status = 2 AND c.auth_status = 2 LIMIT 1",
                (rs, rowNum) -> row(rs),
                jobId).stream().findFirst();
    }

    private QueryParts visibleQuery(PortalJobSearchCriteria criteria, boolean countOnly) {
        StringBuilder sql = new StringBuilder(countOnly ? "SELECT COUNT(*) " : baseSelect());
        if (countOnly) {
            sql.append("FROM job_post j JOIN company c ON c.id = j.company_id ");
        }
        sql.append(" WHERE j.deleted_at IS NULL AND j.status = 2 AND j.audit_status = 2 AND c.auth_status = 2");
        List<Object> args = new ArrayList<>();
        if (criteria == null) {
            return new QueryParts(sql.toString(), args);
        }
        if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
            sql.append(" AND (j.title LIKE ? OR c.company_name LIKE ?)");
            String keyword = "%" + criteria.keyword().trim() + "%";
            args.add(keyword);
            args.add(keyword);
        }
        if (criteria.cityCode() != null && !criteria.cityCode().isBlank()) {
            sql.append(" AND j.city_code = ?");
            args.add(criteria.cityCode().trim());
        }
        if (criteria.categoryCode() != null && !criteria.categoryCode().isBlank()) {
            sql.append(" AND j.category_code = ?");
            args.add(criteria.categoryCode().trim());
        }
        if (criteria.salaryMin() != null) {
            sql.append(" AND j.salary_max IS NOT NULL AND j.salary_max >= ?");
            args.add(criteria.salaryMin());
        }
        if (criteria.salaryMax() != null) {
            sql.append(" AND j.salary_min IS NOT NULL AND j.salary_min <= ?");
            args.add(criteria.salaryMax());
        }
        if (criteria.industryCode() != null && !criteria.industryCode().isBlank()) {
            sql.append(" AND c.industry_code = ?");
            args.add(criteria.industryCode().trim());
        }
        if (criteria.scaleCode() != null && !criteria.scaleCode().isBlank()) {
            sql.append(" AND c.scale_code = ?");
            args.add(criteria.scaleCode().trim());
        }
        if (criteria.updatedWithin() != null) {
            sql.append(" AND j.updated_at >= ?");
            args.add(LocalDateTime.now().minusDays(criteria.updatedWithin()));
        }
        return new QueryParts(sql.toString(), args);
    }

    private String visibleOrderBy(String sort) {
        if ("salary_asc".equals(sort)) {
            return " ORDER BY j.salary_min IS NULL, j.salary_min ASC, j.updated_at DESC";
        }
        if ("salary_desc".equals(sort)) {
            return " ORDER BY j.salary_max IS NULL, j.salary_max DESC, j.updated_at DESC";
        }
        return " ORDER BY j.updated_at DESC";
    }

    private String baseSelect() {
        return "SELECT j.id, j.company_id, c.company_name, c.auth_status AS company_auth_status, "
                + "j.title, j.job_nature_code, j.category_code, j.category_name, j.experience_code, "
                + "j.education_code, j.recruit_count, j.city_code, j.work_region_path, j.address, "
                + "j.salary_min, j.salary_max, j.salary_negotiable, j.job_desc, CAST(j.welfare_codes AS CHAR) AS welfare_codes, "
                + "j.department_name, j.age_min, j.age_max, j.age_unlimited, j.recruitment_time_code, "
                + "j.contact_mode, j.contact_name, j.contact_mobile, j.contact_phone, j.contact_email, "
                + "j.contact_wechat, j.contact_hidden, j.notify_enabled, j.resume_subscription_enabled, "
                + "j.status, j.audit_status, j.review_memo, j.reject_reason, j.review_user_id, "
                + "j.review_time, j.published_at, j.offline_reason, j.status_changed_at, j.updated_at, "
                + "j.deleted_at, j.deleted_by, j.delete_reason "
                + "FROM job_post j JOIN company c ON c.id = j.company_id";
    }

    private JobPostRow row(ResultSet rs) throws SQLException {
        return new JobPostRow(
                rs.getLong("id"),
                rs.getLong("company_id"),
                rs.getString("company_name"),
                rs.getInt("company_auth_status"),
                rs.getString("title"),
                rs.getString("job_nature_code"),
                rs.getString("category_code"),
                rs.getString("category_name"),
                rs.getString("experience_code"),
                rs.getString("education_code"),
                rs.getObject("recruit_count", Integer.class),
                rs.getString("city_code"),
                rs.getString("work_region_path"),
                rs.getString("address"),
                rs.getObject("salary_min", Integer.class),
                rs.getObject("salary_max", Integer.class),
                rs.getBoolean("salary_negotiable"),
                rs.getString("job_desc"),
                readStringList(rs.getString("welfare_codes")),
                rs.getString("department_name"),
                rs.getObject("age_min", Integer.class),
                rs.getObject("age_max", Integer.class),
                rs.getBoolean("age_unlimited"),
                rs.getString("recruitment_time_code"),
                rs.getString("contact_mode"),
                rs.getString("contact_name"),
                rs.getString("contact_mobile"),
                rs.getString("contact_phone"),
                rs.getString("contact_email"),
                rs.getString("contact_wechat"),
                rs.getBoolean("contact_hidden"),
                rs.getBoolean("notify_enabled"),
                rs.getBoolean("resume_subscription_enabled"),
                rs.getInt("status"),
                rs.getInt("audit_status"),
                rs.getString("review_memo"),
                rs.getString("reject_reason"),
                rs.getObject("review_user_id", Long.class),
                rs.getTimestamp("review_time") == null ? null : rs.getTimestamp("review_time").toLocalDateTime(),
                rs.getTimestamp("published_at") == null ? null : rs.getTimestamp("published_at").toLocalDateTime(),
                rs.getString("offline_reason"),
                rs.getTimestamp("status_changed_at") == null ? null : rs.getTimestamp("status_changed_at").toLocalDateTime(),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime(),
                rs.getTimestamp("deleted_at") == null ? null : rs.getTimestamp("deleted_at").toLocalDateTime(),
                rs.getObject("deleted_by", Long.class),
                rs.getString("delete_reason"));
    }

    private void setNullableInt(PreparedStatement statement, int parameterIndex, Integer value) throws SQLException {
        if (value == null) {
            statement.setObject(parameterIndex, null);
        } else {
            statement.setInt(parameterIndex, value);
        }
    }

    private String toJsonArray(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception exception) {
            return "[]";
        }
    }

    private List<String> readStringList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<List<String>>() {
            });
        } catch (Exception exception) {
            return List.of();
        }
    }

    private record QueryParts(String sql, List<Object> args) {
    }
}
