package cn.localtalent.backend.candidate.infrastructure;

import cn.localtalent.backend.candidate.domain.CandidateProfileRaw;
import cn.localtalent.backend.candidate.domain.ConsentRecord;
import cn.localtalent.backend.candidate.domain.PortalSnapshotRow;
import cn.localtalent.backend.candidate.domain.PortalSnapshotRows;
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
public class CandidateJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public CandidateJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<CandidateProfileRaw> findCandidateProfile(long candidateId) {
        return jdbcTemplate.query(
                "SELECT cu.id, cu.real_name, cu.realname_verified_flag, "
                        + "cr.base_profile_json AS base_profile_json, "
                        + "cr.skills_json AS skills_json "
                        + "FROM candidate_user cu "
                        + "LEFT JOIN candidate_resume cr ON cr.id = ("
                        + "  SELECT id FROM candidate_resume "
                        + "  WHERE candidate_id = cu.id AND status = 1 ORDER BY id DESC LIMIT 1"
                        + ") "
                        + "WHERE cu.id = ? AND cu.status = 1 LIMIT 1",
                (rs, rowNum) -> new CandidateProfileRaw(
                        rs.getLong("id"),
                        rs.getString("real_name"),
                        rs.getInt("realname_verified_flag") == 1,
                        rs.getString("base_profile_json"),
                        rs.getString("skills_json")),
                candidateId)
                .stream()
                .findFirst();
    }

    public long insertConsent(
            long candidateId,
            String consentScopeJson,
            String consentVersion,
            boolean realnameVerified,
            boolean secondConfirmed
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
                    PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO candidate_consent "
                            + "(candidate_id, consent_status, consent_scope, consent_version, consent_channel, "
                            + "consent_time, realname_verified_flag, second_confirm_flag, revoke_status) "
                            + "VALUES (?, 1, ?, ?, 'portal', CURRENT_TIMESTAMP, ?, ?, 0)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, candidateId);
            statement.setString(2, consentScopeJson);
            statement.setString(3, consentVersion);
            statement.setInt(4, realnameVerified ? 1 : 0);
            statement.setInt(5, secondConfirmed ? 1 : 0);
            return statement;
        }, keyHolder);
        return generatedId(keyHolder);
    }

    public long insertPublishSnapshot(long candidateId, String consentVersion, String snapshotJson) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
                    PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO candidate_publish_snapshot "
                            + "(candidate_id, source_type, legal_basis, consent_status, publishable_flag, "
                            + "consent_version, visibility_scope, snapshot_json, sync_version, status) "
                            + "VALUES (?, 1, 'consent', 1, 1, ?, 4, ?, 1, 1)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, candidateId);
            statement.setString(2, consentVersion);
            statement.setString(3, snapshotJson);
            return statement;
        }, keyHolder);
        return generatedId(keyHolder);
    }

    public void offlineActiveSnapshots(long candidateId) {
        jdbcTemplate.update(
                "UPDATE candidate_publish_snapshot "
                        + "SET publishable_flag = 0, status = 0, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE candidate_id = ? AND status = 1",
                candidateId);
    }

    public void upsertControlProfileAfterConsent(
            long candidateId,
            String consentVersion,
            long consentId,
            long snapshotId
    ) {
        jdbcTemplate.update(
                "INSERT INTO candidate_control_profile "
                        + "(candidate_id, source_type, legal_basis, consent_status, publishable_flag, "
                        + "visibility_scope, consent_version, current_consent_id, current_snapshot_id, control_status) "
                        + "VALUES (?, 1, 'consent', 1, 1, 4, ?, ?, ?, 1) "
                        + "ON DUPLICATE KEY UPDATE "
                        + "source_type = VALUES(source_type), "
                        + "legal_basis = VALUES(legal_basis), "
                        + "consent_status = VALUES(consent_status), "
                        + "publishable_flag = VALUES(publishable_flag), "
                        + "visibility_scope = VALUES(visibility_scope), "
                        + "consent_version = VALUES(consent_version), "
                        + "current_consent_id = VALUES(current_consent_id), "
                        + "current_snapshot_id = VALUES(current_snapshot_id), "
                        + "control_status = VALUES(control_status)",
                candidateId,
                consentVersion,
                consentId,
                snapshotId);
    }

    public Optional<ConsentRecord> findConsent(long consentId) {
        return jdbcTemplate.query(
                "SELECT id, candidate_id, consent_status, revoke_status "
                        + "FROM candidate_consent WHERE id = ? LIMIT 1",
                (rs, rowNum) -> new ConsentRecord(
                        rs.getLong("id"),
                        rs.getLong("candidate_id"),
                        rs.getInt("consent_status"),
                        rs.getInt("revoke_status")),
                consentId)
                .stream()
                .findFirst();
    }

    public Optional<Long> findCurrentSnapshotId(long candidateId) {
        return jdbcTemplate.query(
                "SELECT current_snapshot_id FROM candidate_control_profile "
                        + "WHERE candidate_id = ? AND current_snapshot_id IS NOT NULL LIMIT 1",
                (rs, rowNum) -> rs.getLong("current_snapshot_id"),
                candidateId)
                .stream()
                .findFirst();
    }

    public void revokeConsent(long consentId, long candidateId) {
        jdbcTemplate.update(
                "UPDATE candidate_consent "
                        + "SET consent_status = 2, revoke_status = 1, revoke_time = CURRENT_TIMESTAMP "
                        + "WHERE id = ? AND candidate_id = ?",
                consentId,
                candidateId);
    }

    public void updateControlProfileAfterRevoke(long candidateId, long consentId) {
        jdbcTemplate.update(
                "UPDATE candidate_control_profile "
                        + "SET consent_status = 2, publishable_flag = 0, current_consent_id = ?, "
                        + "current_snapshot_id = NULL, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE candidate_id = ?",
                consentId,
                candidateId);
    }

    public PortalSnapshotRows findVisibleSnapshots(
            String cityCode,
            String categoryCode,
            Integer experienceMin,
            Integer experienceMax,
            Integer updatedWithinDays,
            String sort,
            int page,
            int size
    ) {
        StringBuilder where = new StringBuilder(
                " WHERE publishable_flag = 1 AND consent_status = 1 AND status = 1 AND visibility_scope = 4");
        List<Object> args = new ArrayList<>();
        addColumnFilter(where, args, "city_code", cityCode);
        addColumnFilter(where, args, "category_code", categoryCode);
        addMinimumFilter(where, args, "experience_years", experienceMin);
        addMaximumFilter(where, args, "experience_years", experienceMax);
        addUpdatedWithinFilter(where, args, updatedWithinDays);

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM candidate_publish_snapshot" + where,
                Long.class,
                args.toArray());

        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((page - 1) * size);
        List<PortalSnapshotRow> rows = jdbcTemplate.query(
                "SELECT id, snapshot_json, "
                        + "DATE_FORMAT(updated_at, '%Y-%m-%dT%H:%i:%s') AS updated_at_text "
                        + "FROM candidate_publish_snapshot"
                        + where
                        + orderBy(sort)
                        + " LIMIT ? OFFSET ?",
                (rs, rowNum) -> new PortalSnapshotRow(
                        rs.getLong("id"),
                        rs.getString("snapshot_json"),
                        rs.getString("updated_at_text")),
                pageArgs.toArray());

        return new PortalSnapshotRows(rows, total == null ? 0 : total);
    }

    private void addMinimumFilter(StringBuilder where, List<Object> args, String columnName, Integer value) {
        if (value == null) {
            return;
        }
        where.append(" AND ").append(columnName).append(" >= ?");
        args.add(value);
    }

    private void addMaximumFilter(StringBuilder where, List<Object> args, String columnName, Integer value) {
        if (value == null) {
            return;
        }
        where.append(" AND ").append(columnName).append(" <= ?");
        args.add(value);
    }

    private void addUpdatedWithinFilter(StringBuilder where, List<Object> args, Integer days) {
        if (days == null) {
            return;
        }
        where.append(" AND updated_at >= TIMESTAMPADD(DAY, ?, CURRENT_TIMESTAMP)");
        args.add(-days);
    }

    private String orderBy(String sort) {
        return switch (sort) {
            case "experience_desc" -> " ORDER BY experience_years DESC, updated_at DESC, id DESC";
            case "experience_asc" -> " ORDER BY experience_years ASC, updated_at DESC, id DESC";
            default -> " ORDER BY updated_at DESC, id DESC";
        };
    }

    private void addColumnFilter(StringBuilder where, List<Object> args, String columnName, String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return;
        }
        where.append(" AND ").append(columnName).append(" = ?");
        args.add(normalized);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private long generatedId(KeyHolder keyHolder) {
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("database did not return generated id");
        }
        return key.longValue();
    }
}
