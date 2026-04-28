package cn.localtalent.backend.exporting.infrastructure;

import cn.localtalent.backend.exporting.domain.ExportApplyRow;
import cn.localtalent.backend.exporting.domain.ExportCandidateRow;
import cn.localtalent.backend.exporting.domain.ExportScope;
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
public class ExportApplyJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public ExportApplyJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long create(
            long companyId,
            long applyUserId,
            String applyIdentityType,
            String applyRoleCode,
            String bizType,
            String scopeJson,
            String reason
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO export_apply "
                            + "(company_id, apply_user_id, apply_identity_type, apply_role_code, biz_type, scope_json, "
                            + "reason, approve_status, generate_status, download_count) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0, 0)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, companyId);
            statement.setLong(2, applyUserId);
            statement.setString(3, applyIdentityType);
            statement.setString(4, applyRoleCode);
            statement.setString(5, bizType);
            statement.setString(6, scopeJson);
            statement.setString(7, reason);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("export id was not generated");
        }
        return key.longValue();
    }

    public Optional<ExportApplyRow> findById(long exportId) {
        return jdbcTemplate.query(
                "SELECT * FROM export_apply WHERE id = ? LIMIT 1",
                (rs, rowNum) -> row(rs),
                exportId).stream().findFirst();
    }

    public List<ExportApplyRow> listForReview(Integer approveStatus, int limit, int offset) {
        QueryParts query = reviewQuery(approveStatus, false);
        query.args().add(limit);
        query.args().add(offset);
        return jdbcTemplate.query(
                query.sql() + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> row(rs),
                query.args().toArray());
    }

    public long countForReview(Integer approveStatus) {
        QueryParts query = reviewQuery(approveStatus, true);
        Long count = jdbcTemplate.queryForObject(query.sql(), Long.class, query.args().toArray());
        return count == null ? 0 : count;
    }

    public boolean jobBelongsToCompany(long companyId, long jobId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM job_post WHERE id = ? AND company_id = ?",
                Integer.class,
                jobId,
                companyId);
        return count != null && count > 0;
    }

    public List<ExportCandidateRow> listApplicationCandidates(long companyId, ExportScope scope) {
        StringBuilder sql = new StringBuilder(
                "SELECT a.id AS application_id, j.title AS job_title, a.application_status, a.apply_time, "
                        + "COALESCE(cu.real_name, '求职者') AS display_name, cu.mobile AS contact_mobile, "
                        + "cu.email AS contact_email, "
                        + "COALESCE(JSON_UNQUOTE(JSON_EXTRACT(r.skills_json, '$.summary')), '') AS skills_summary "
                        + "FROM job_application a "
                        + "JOIN job_post j ON j.id = a.job_id "
                        + "JOIN candidate_user cu ON cu.id = a.candidate_id "
                        + "LEFT JOIN candidate_resume r ON r.id = a.resume_id "
                        + "WHERE j.company_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (scope.jobId() != null) {
            sql.append(" AND a.job_id = ?");
            args.add(scope.jobId());
        }
        if (scope.status() != null) {
            sql.append(" AND a.application_status = ?");
            args.add(scope.status());
        }
        sql.append(" ORDER BY a.apply_time DESC, a.id DESC");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> candidateRow(rs), args.toArray());
    }

    public void approve(long exportId, int approveStatus, long approveUserId, String rejectReason) {
        jdbcTemplate.update(
                "UPDATE export_apply SET approve_status = ?, approve_user_id = ?, approve_time = CURRENT_TIMESTAMP, "
                        + "reject_reason = ?, generate_status = CASE WHEN ? = 1 THEN 0 ELSE generate_status END "
                        + "WHERE id = ?",
                approveStatus,
                approveUserId,
                rejectReason,
                approveStatus,
                exportId);
    }

    public void markGenerating(long exportId) {
        jdbcTemplate.update(
                "UPDATE export_apply SET generate_status = 1, error_msg = NULL WHERE id = ?",
                exportId);
    }

    public void markGenerated(long exportId, String objectKey, String sha256, long sizeBytes) {
        jdbcTemplate.update(
                "UPDATE export_apply SET generate_status = 2, file_object_key = ?, file_sha256 = ?, "
                        + "file_size_bytes = ?, generated_at = CURRENT_TIMESTAMP, error_msg = NULL WHERE id = ?",
                objectKey,
                sha256,
                sizeBytes,
                exportId);
    }

    public void markFailed(long exportId, String errorMsg) {
        jdbcTemplate.update(
                "UPDATE export_apply SET generate_status = 3, error_msg = ? WHERE id = ?",
                truncate(errorMsg, 500),
                exportId);
    }

    public boolean markDownloadIssued(long exportId, String downloadUrl, LocalDateTime expireTime) {
        int updated = jdbcTemplate.update(
                "UPDATE export_apply SET download_url = ?, expire_time = ?, download_issued_at = CURRENT_TIMESTAMP, "
                        + "download_count = download_count + 1 WHERE id = ? AND download_count = 0",
                downloadUrl,
                expireTime,
                exportId);
        return updated == 1;
    }

    private QueryParts reviewQuery(Integer approveStatus, boolean countOnly) {
        StringBuilder sql = new StringBuilder(countOnly ? "SELECT COUNT(*) FROM export_apply" : "SELECT * FROM export_apply");
        List<Object> args = new ArrayList<>();
        if (approveStatus != null) {
            sql.append(" WHERE approve_status = ?");
            args.add(approveStatus);
        }
        return new QueryParts(sql.toString(), args);
    }

    private ExportCandidateRow candidateRow(ResultSet rs) throws SQLException {
        return new ExportCandidateRow(
                rs.getLong("application_id"),
                rs.getString("job_title"),
                rs.getInt("application_status"),
                rs.getTimestamp("apply_time").toLocalDateTime(),
                rs.getString("display_name"),
                rs.getString("contact_mobile"),
                rs.getString("contact_email"),
                rs.getString("skills_summary"));
    }

    private ExportApplyRow row(ResultSet rs) throws SQLException {
        return new ExportApplyRow(
                rs.getLong("id"),
                nullableLong(rs, "company_id"),
                rs.getLong("apply_user_id"),
                rs.getString("apply_identity_type"),
                rs.getString("apply_role_code"),
                rs.getString("biz_type"),
                rs.getString("scope_json"),
                rs.getString("reason"),
                rs.getInt("approve_status"),
                nullableLong(rs, "approve_user_id"),
                nullableTime(rs, "approve_time"),
                rs.getString("reject_reason"),
                rs.getInt("generate_status"),
                rs.getString("file_object_key"),
                rs.getString("file_sha256"),
                nullableLong(rs, "file_size_bytes"),
                nullableTime(rs, "generated_at"),
                rs.getString("download_url"),
                nullableTime(rs, "expire_time"),
                nullableTime(rs, "download_issued_at"),
                rs.getInt("download_count"),
                rs.getString("error_msg"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime());
    }

    private Long nullableLong(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }

    private LocalDateTime nullableTime(ResultSet rs, String columnName) throws SQLException {
        var timestamp = rs.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record QueryParts(String sql, List<Object> args) {
    }
}
