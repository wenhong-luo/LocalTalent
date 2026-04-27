package cn.localtalent.backend.interview.infrastructure;

import cn.localtalent.backend.application.domain.ApplicationRow;
import cn.localtalent.backend.interview.api.InterviewSessionCreateRequest;
import cn.localtalent.backend.interview.domain.InterviewSessionRow;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class InterviewJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public InterviewJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long createSession(ApplicationRow application, InterviewSessionCreateRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO interview_session "
                            + "(application_id, job_id, company_id, session_name, session_time, location, status) "
                            + "VALUES (?, ?, ?, ?, ?, ?, 1)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, application.applicationId());
            statement.setLong(2, application.jobId());
            statement.setLong(3, application.companyId());
            statement.setString(4, request.sessionName());
            statement.setObject(5, request.sessionTime());
            statement.setString(6, request.location());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("session id was not generated");
        }
        return key.longValue();
    }

    public Optional<InterviewSessionRow> findById(long sessionId) {
        return jdbcTemplate.query(
                baseSelect() + " WHERE s.id = ? LIMIT 1",
                (rs, rowNum) -> row(rs),
                sessionId).stream().findFirst();
    }

    public Optional<InterviewSessionRow> findByCodeHash(String codeHash) {
        return jdbcTemplate.query(
                baseSelect() + " WHERE s.signin_code_hash = ? LIMIT 1",
                (rs, rowNum) -> row(rs),
                codeHash).stream().findFirst();
    }

    public void updateSigninCode(long sessionId, String codeHash, LocalDateTime expiresAt) {
        jdbcTemplate.update(
                "UPDATE interview_session SET qr_code = NULL, signin_code_hash = ?, "
                        + "signin_code_expires_at = ?, signin_code_used_at = NULL WHERE id = ?",
                codeHash,
                expiresAt,
                sessionId);
    }

    public int markCodeUsed(long sessionId) {
        return jdbcTemplate.update(
                "UPDATE interview_session SET signin_code_used_at = CURRENT_TIMESTAMP "
                        + "WHERE id = ? AND signin_code_hash IS NOT NULL "
                        + "AND signin_code_used_at IS NULL AND signin_code_expires_at >= CURRENT_TIMESTAMP",
                sessionId);
    }

    public long insertSignin(long sessionId, long candidateId, String signChannel, String deviceId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO interview_signin "
                            + "(session_id, candidate_id, sign_channel, device_id, consent_redirect_flag) "
                            + "VALUES (?, ?, ?, ?, 0)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, sessionId);
            statement.setLong(2, candidateId);
            statement.setString(3, signChannel);
            statement.setString(4, deviceId);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("signin id was not generated");
        }
        return key.longValue();
    }

    private String baseSelect() {
        return "SELECT s.id, s.application_id, s.job_id, s.company_id, a.candidate_id, "
                + "s.session_name, s.session_time, s.location, s.signin_code_hash, "
                + "s.signin_code_expires_at, s.signin_code_used_at, s.status, a.application_status "
                + "FROM interview_session s LEFT JOIN job_application a ON a.id = s.application_id";
    }

    private InterviewSessionRow row(ResultSet rs) throws SQLException {
        return new InterviewSessionRow(
                rs.getLong("id"),
                rs.getObject("application_id", Long.class),
                rs.getLong("job_id"),
                rs.getLong("company_id"),
                rs.getObject("candidate_id", Long.class),
                rs.getString("session_name"),
                rs.getTimestamp("session_time").toLocalDateTime(),
                rs.getString("location"),
                rs.getString("signin_code_hash"),
                rs.getTimestamp("signin_code_expires_at") == null
                        ? null : rs.getTimestamp("signin_code_expires_at").toLocalDateTime(),
                rs.getTimestamp("signin_code_used_at") == null
                        ? null : rs.getTimestamp("signin_code_used_at").toLocalDateTime(),
                rs.getInt("status"),
                rs.getObject("application_status", Integer.class));
    }
}
