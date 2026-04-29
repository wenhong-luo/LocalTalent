package cn.localtalent.backend.job.infrastructure;

import cn.localtalent.backend.job.api.JobCreateRequest;
import cn.localtalent.backend.job.api.JobUpdateRequest;
import cn.localtalent.backend.job.api.PortalJobSearchCriteria;
import cn.localtalent.backend.job.domain.JobPostRow;
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

    public JobJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long create(long companyId, JobCreateRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO job_post "
                            + "(company_id, source_type, title, category_code, city_code, salary_min, salary_max, "
                            + "job_desc, status, audit_status, status_changed_at) "
                            + "VALUES (?, 1, ?, ?, ?, ?, ?, ?, 1, 1, CURRENT_TIMESTAMP)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, companyId);
            statement.setString(2, request.title());
            statement.setString(3, request.categoryCode());
            statement.setString(4, request.cityCode());
            if (request.salaryMin() == null) {
                statement.setObject(5, null);
            } else {
                statement.setInt(5, request.salaryMin());
            }
            if (request.salaryMax() == null) {
                statement.setObject(6, null);
            } else {
                statement.setInt(6, request.salaryMax());
            }
            statement.setString(7, request.jobDesc());
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
                baseSelect() + " WHERE j.company_id = ? ORDER BY j.updated_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> row(rs),
                companyId,
                limit,
                offset);
    }

    public long countByCompany(long companyId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM job_post WHERE company_id = ?",
                Integer.class,
                companyId);
        return count == null ? 0 : count;
    }

    public void update(long jobId, JobUpdateRequest request) {
        jdbcTemplate.update(
                "UPDATE job_post SET title = ?, category_code = ?, city_code = ?, salary_min = ?, salary_max = ?, "
                        + "job_desc = ?, status = 1, audit_status = 1, review_memo = NULL, reject_reason = NULL, "
                        + "review_user_id = NULL, review_time = NULL, published_at = NULL, status_changed_at = CURRENT_TIMESTAMP "
                        + "WHERE id = ?",
                request.title(),
                request.categoryCode(),
                request.cityCode(),
                request.salaryMin(),
                request.salaryMax(),
                request.jobDesc(),
                jobId);
    }

    public void submitReview(long jobId) {
        jdbcTemplate.update(
                "UPDATE job_post SET status = 1, audit_status = 1, review_memo = NULL, reject_reason = NULL, "
                        + "review_user_id = NULL, review_time = NULL, published_at = NULL, offline_reason = NULL, "
                        + "status_changed_at = CURRENT_TIMESTAMP WHERE id = ?",
                jobId);
    }

    public void offline(long jobId, String reason) {
        jdbcTemplate.update(
                "UPDATE job_post SET status = 3, offline_reason = ?, published_at = NULL, "
                        + "status_changed_at = CURRENT_TIMESTAMP WHERE id = ?",
                reason,
                jobId);
    }

    public void review(long jobId, int auditStatus, int status, String memo, String rejectReason, long reviewerId) {
        jdbcTemplate.update(
                "UPDATE job_post SET audit_status = ?, status = ?, review_memo = ?, reject_reason = ?, "
                        + "review_user_id = ?, review_time = CURRENT_TIMESTAMP, "
                        + "published_at = CASE WHEN ? = 2 THEN CURRENT_TIMESTAMP ELSE NULL END, "
                        + "offline_reason = CASE WHEN ? = 2 THEN NULL ELSE offline_reason END, "
                        + "status_changed_at = CURRENT_TIMESTAMP WHERE id = ?",
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
                    baseSelect() + " ORDER BY j.updated_at DESC LIMIT ? OFFSET ?",
                    (rs, rowNum) -> row(rs),
                    limit,
                    offset);
        }
        return jdbcTemplate.query(
                baseSelect() + " WHERE j.audit_status = ? ORDER BY j.updated_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> row(rs),
                auditStatus,
                limit,
                offset);
    }

    public long countForReview(Integer auditStatus) {
        Integer count;
        if (auditStatus == null) {
            count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM job_post", Integer.class);
        } else {
            count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM job_post WHERE audit_status = ?",
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
                        + " WHERE j.id = ? AND j.status = 2 AND j.audit_status = 2 AND c.auth_status = 2 LIMIT 1",
                (rs, rowNum) -> row(rs),
                jobId).stream().findFirst();
    }

    private QueryParts visibleQuery(PortalJobSearchCriteria criteria, boolean countOnly) {
        StringBuilder sql = new StringBuilder(countOnly ? "SELECT COUNT(*) " : baseSelect());
        if (countOnly) {
            sql.append("FROM job_post j JOIN company c ON c.id = j.company_id ");
        }
        sql.append(" WHERE j.status = 2 AND j.audit_status = 2 AND c.auth_status = 2");
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
                + "j.title, j.category_code, j.city_code, j.salary_min, j.salary_max, j.job_desc, "
                + "j.status, j.audit_status, j.review_memo, j.reject_reason, j.review_user_id, "
                + "j.review_time, j.published_at, j.offline_reason, j.status_changed_at, j.updated_at "
                + "FROM job_post j JOIN company c ON c.id = j.company_id";
    }

    private JobPostRow row(ResultSet rs) throws SQLException {
        return new JobPostRow(
                rs.getLong("id"),
                rs.getLong("company_id"),
                rs.getString("company_name"),
                rs.getInt("company_auth_status"),
                rs.getString("title"),
                rs.getString("category_code"),
                rs.getString("city_code"),
                rs.getObject("salary_min", Integer.class),
                rs.getObject("salary_max", Integer.class),
                rs.getString("job_desc"),
                rs.getInt("status"),
                rs.getInt("audit_status"),
                rs.getString("review_memo"),
                rs.getString("reject_reason"),
                rs.getObject("review_user_id", Long.class),
                rs.getTimestamp("review_time") == null ? null : rs.getTimestamp("review_time").toLocalDateTime(),
                rs.getTimestamp("published_at") == null ? null : rs.getTimestamp("published_at").toLocalDateTime(),
                rs.getString("offline_reason"),
                rs.getTimestamp("status_changed_at") == null ? null : rs.getTimestamp("status_changed_at").toLocalDateTime(),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime());
    }

    private record QueryParts(String sql, List<Object> args) {
    }
}
