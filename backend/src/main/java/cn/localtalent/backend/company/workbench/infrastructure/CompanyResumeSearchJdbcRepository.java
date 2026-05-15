package cn.localtalent.backend.company.workbench.infrastructure;

import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class CompanyResumeSearchJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public CompanyResumeSearchJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ResumeSearchRows search(ResumeSearchQuery query) {
        StringBuilder where = new StringBuilder(
                " WHERE publishable_flag = 1 AND consent_status = 1 AND status = 1 AND visibility_scope = 4");
        List<Object> args = new ArrayList<>();
        addEquals(where, args, "city_code", query.cityCode());
        addEquals(where, args, "category_code", query.categoryCode());
        addEquals(where, args, "education_code", query.educationCode());
        addRange(where, args, "experience_years", query.experienceMin(), true);
        addRange(where, args, "experience_years", query.experienceMax(), false);
        addEquals(where, args, "gender_code", query.gender());
        addLike(where, args, "resume_tags_text", query.resumeTag());
        addEquals(where, args, "industry_code", query.industryCode());
        addLike(where, args, "major_name", query.major());
        addEquals(where, args, "work_nature_code", query.workNature());
        addEquals(where, args, "expected_salary_code", query.expectedSalaryCode());
        addUpdatedWithin(where, args, query.updatedWithinDays());
        addKeyword(where, args, query.keyword());

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM candidate_publish_snapshot" + where,
                Long.class,
                args.toArray());

        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(query.size());
        pageArgs.add((query.page() - 1) * query.size());
        List<ResumeSearchRow> rows = jdbcTemplate.query(
                "SELECT id, snapshot_json, DATE_FORMAT(updated_at, '%Y-%m-%dT%H:%i:%s') AS updated_at_text "
                        + "FROM candidate_publish_snapshot"
                        + where
                        + orderBy(query.sort())
                        + " LIMIT ? OFFSET ?",
                (rs, rowNum) -> new ResumeSearchRow(
                        rs.getLong("id"),
                        rs.getString("snapshot_json"),
                        rs.getString("updated_at_text")),
                pageArgs.toArray());
        return new ResumeSearchRows(rows, total == null ? 0 : total);
    }

    public ResumeSearchRow findVisibleSnapshot(long snapshotId) {
        return jdbcTemplate.query(
                "SELECT id, snapshot_json, DATE_FORMAT(updated_at, '%Y-%m-%dT%H:%i:%s') AS updated_at_text "
                        + "FROM candidate_publish_snapshot "
                        + "WHERE id = ? AND publishable_flag = 1 AND consent_status = 1 AND status = 1 AND visibility_scope = 4 "
                        + "LIMIT 1",
                (rs, rowNum) -> new ResumeSearchRow(
                        rs.getLong("id"),
                        rs.getString("snapshot_json"),
                        rs.getString("updated_at_text")),
                snapshotId)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public long createReport(long companyId, long reporterUserId, long snapshotId, String reasonCode, String remarkSummary) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO company_resume_snapshot_report "
                            + "(company_id, reporter_user_id, snapshot_id, reason_code, remark_summary, status) "
                            + "VALUES (?, ?, ?, ?, ?, 0)",
                    java.sql.Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, companyId);
            statement.setLong(2, reporterUserId);
            statement.setLong(3, snapshotId);
            statement.setString(4, reasonCode);
            statement.setString(5, remarkSummary);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0 : key.longValue();
    }

    public void createRiskReviewForReport(long reportId, long snapshotId, String reasonCode, String remarkSummary) {
        jdbcTemplate.update(
                "INSERT INTO risk_review "
                        + "(risk_type, target_type, target_id, severity, status, title, summary) "
                        + "VALUES ('resume_snapshot_report', 'talent_snapshot', ?, 'medium', 0, ?, ?)",
                snapshotId,
                "企业举报发布快照 #" + snapshotId,
                "report_id=" + reportId + "; reason=" + reasonCode
                        + (remarkSummary == null || remarkSummary.isBlank() ? "" : "; remark=" + remarkSummary));
    }

    public AccessRequestRow createAccessRequest(
            long companyId,
            long requesterUserId,
            long snapshotId,
            String requestType,
            String reasonSummary
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO company_resume_access_request "
                            + "(company_id, requester_user_id, snapshot_id, request_type, reason_summary, status) "
                            + "VALUES (?, ?, ?, ?, ?, 0)",
                    java.sql.Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, companyId);
            statement.setLong(2, requesterUserId);
            statement.setLong(3, snapshotId);
            statement.setString(4, requestType);
            statement.setString(5, reasonSummary);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        long requestId = key == null ? 0 : key.longValue();
        String createdAt = jdbcTemplate.queryForObject(
                "SELECT DATE_FORMAT(created_at, '%Y-%m-%dT%H:%i:%s') FROM company_resume_access_request WHERE id = ?",
                String.class,
                requestId);
        return new AccessRequestRow(requestId, requestType, "submitted", createdAt);
    }

    public void createRiskReviewForAccessRequest(long requestId, long snapshotId, String requestType, String reasonSummary) {
        jdbcTemplate.update(
                "INSERT INTO risk_review "
                        + "(risk_type, target_type, target_id, severity, status, title, summary) "
                        + "VALUES ('resume_access_request', 'talent_snapshot', ?, 'medium', 0, ?, ?)",
                snapshotId,
                "企业受控访问申请 #" + snapshotId,
                "request_id=" + requestId + "; type=" + requestType
                        + (reasonSummary == null || reasonSummary.isBlank() ? "" : "; reason=" + reasonSummary));
    }

    private void addEquals(StringBuilder where, List<Object> args, String columnName, String value) {
        if (value == null) {
            return;
        }
        where.append(" AND ").append(columnName).append(" = ?");
        args.add(value);
    }

    private void addLike(StringBuilder where, List<Object> args, String columnName, String value) {
        if (value == null) {
            return;
        }
        where.append(" AND ").append(columnName).append(" LIKE ?");
        args.add("%" + value + "%");
    }

    private void addRange(StringBuilder where, List<Object> args, String columnName, Integer value, boolean minimum) {
        if (value == null) {
            return;
        }
        where.append(" AND ").append(columnName).append(minimum ? " >= ?" : " <= ?");
        args.add(value);
    }

    private void addUpdatedWithin(StringBuilder where, List<Object> args, Integer days) {
        if (days == null) {
            return;
        }
        where.append(" AND updated_at >= TIMESTAMPADD(DAY, ?, CURRENT_TIMESTAMP)");
        args.add(-days);
    }

    private void addKeyword(StringBuilder where, List<Object> args, String keyword) {
        if (keyword == null) {
            return;
        }
        where.append(" AND (")
                .append("display_name_masked LIKE ?")
                .append(" OR COALESCE(JSON_UNQUOTE(JSON_EXTRACT(snapshot_json, '$.expected_positions_text')), '') LIKE ?")
                .append(" OR COALESCE(JSON_UNQUOTE(JSON_EXTRACT(snapshot_json, '$.expected_cities_text')), '') LIKE ?")
                .append(" OR COALESCE(JSON_UNQUOTE(JSON_EXTRACT(snapshot_json, '$.skills_summary')), '') LIKE ?")
                .append(" OR COALESCE(JSON_UNQUOTE(JSON_EXTRACT(snapshot_json, '$.major_name')), '') LIKE ?")
                .append(")");
        String like = "%" + keyword + "%";
        for (int index = 0; index < 5; index++) {
            args.add(like);
        }
    }

    private String orderBy(String sort) {
        return switch (sort) {
            case "experience_desc" -> " ORDER BY experience_years DESC, updated_at DESC, id DESC";
            case "experience_asc" -> " ORDER BY experience_years ASC, updated_at DESC, id DESC";
            default -> " ORDER BY updated_at DESC, id DESC";
        };
    }

    public record ResumeSearchQuery(
            String keyword,
            String cityCode,
            String categoryCode,
            String educationCode,
            Integer experienceMin,
            Integer experienceMax,
            String gender,
            String resumeTag,
            String industryCode,
            String major,
            String workNature,
            String expectedSalaryCode,
            Integer updatedWithinDays,
            int page,
            int size,
            String sort
    ) {
    }

    public record ResumeSearchRows(List<ResumeSearchRow> rows, long total) {
    }

    public record ResumeSearchRow(long snapshotId, String snapshotJson, String updatedAtText) {
    }

    public record AccessRequestRow(long requestId, String requestType, String status, String createdAt) {
    }
}
