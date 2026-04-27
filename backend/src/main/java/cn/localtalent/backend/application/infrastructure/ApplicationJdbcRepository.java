package cn.localtalent.backend.application.infrastructure;

import cn.localtalent.backend.application.domain.ApplicationRow;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ApplicationJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public ApplicationJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long create(long jobId, long candidateId, Long resumeId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO job_application "
                            + "(job_id, candidate_id, resume_id, source_type, application_status) "
                            + "VALUES (?, ?, ?, 2, 0)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, jobId);
            statement.setLong(2, candidateId);
            if (resumeId == null) {
                statement.setObject(3, null);
            } else {
                statement.setLong(3, resumeId);
            }
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("application id was not generated");
        }
        return key.longValue();
    }

    public Optional<ApplicationRow> findById(long applicationId) {
        return jdbcTemplate.query(
                baseSelect() + " WHERE a.id = ? LIMIT 1",
                (rs, rowNum) -> row(rs),
                applicationId).stream().findFirst();
    }

    public List<ApplicationRow> listByCompany(long companyId, Long jobId, Integer status, int limit, int offset) {
        QueryParts query = companyQuery(companyId, jobId, status, false);
        query.args().add(limit);
        query.args().add(offset);
        return jdbcTemplate.query(
                query.sql() + " ORDER BY a.apply_time DESC, a.id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> row(rs),
                query.args().toArray());
    }

    public long countByCompany(long companyId, Long jobId, Integer status) {
        QueryParts query = companyQuery(companyId, jobId, status, true);
        Integer count = jdbcTemplate.queryForObject(query.sql(), Integer.class, query.args().toArray());
        return count == null ? 0 : count;
    }

    public boolean resumeBelongsToCandidate(long resumeId, long candidateId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM candidate_resume WHERE id = ? AND candidate_id = ?",
                Integer.class,
                resumeId,
                candidateId);
        return count != null && count > 0;
    }

    public void updateStatus(long applicationId, int status) {
        jdbcTemplate.update(
                "UPDATE job_application SET application_status = ? WHERE id = ?",
                status,
                applicationId);
    }

    private QueryParts companyQuery(long companyId, Long jobId, Integer status, boolean countOnly) {
        StringBuilder sql = new StringBuilder(countOnly ? "SELECT COUNT(*) " : baseSelect());
        if (countOnly) {
            sql.append("FROM job_application a JOIN job_post j ON j.id = a.job_id JOIN company c ON c.id = j.company_id ");
        }
        sql.append(" WHERE j.company_id = ?");
        List<Object> args = new ArrayList<>();
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

    private String baseSelect() {
        return "SELECT a.id, a.job_id, j.company_id, a.candidate_id, a.resume_id, a.source_type, "
                + "a.application_status, a.apply_time, a.updated_at, j.title AS job_title, c.company_name "
                + "FROM job_application a "
                + "JOIN job_post j ON j.id = a.job_id "
                + "JOIN company c ON c.id = j.company_id";
    }

    private ApplicationRow row(ResultSet rs) throws SQLException {
        return new ApplicationRow(
                rs.getLong("id"),
                rs.getLong("job_id"),
                rs.getLong("company_id"),
                rs.getLong("candidate_id"),
                rs.getObject("resume_id", Long.class),
                rs.getInt("source_type"),
                rs.getInt("application_status"),
                rs.getTimestamp("apply_time").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime(),
                rs.getString("job_title"),
                rs.getString("company_name"));
    }

    private record QueryParts(String sql, List<Object> args) {
    }
}
