package cn.localtalent.backend.admin.ops.infrastructure;

import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.RecommendationResponse;
import cn.localtalent.backend.admin.ops.api.AdminPortalOpsDtos.RiskReviewResponse;
import cn.localtalent.backend.portal.recommendation.api.PortalRecommendationDtos.PortalRecommendationItem;
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
public class AdminPortalOpsJdbcRepository {

    private static final int STATUS_ACTIVE = 1;

    private final JdbcTemplate jdbcTemplate;

    public AdminPortalOpsJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long pendingCompanyCount() {
        return count("SELECT COUNT(*) FROM company WHERE auth_status = 1");
    }

    public long pendingJobCount() {
        return count("SELECT COUNT(*) FROM job_post WHERE audit_status = 1");
    }

    public long pendingExportCount() {
        return count("SELECT COUNT(*) FROM export_apply WHERE approve_status = 0");
    }

    public long publishedContentCount() {
        return count("SELECT COUNT(*) FROM cms_content WHERE status = 1");
    }

    public long publishedEventCount() {
        return count("SELECT COUNT(*) FROM activity_event WHERE status = 1");
    }

    public long activeRecommendationCount() {
        return count("SELECT COUNT(*) FROM portal_recommendation WHERE status = 1");
    }

    public long pendingRiskCount() {
        return count("SELECT COUNT(*) FROM risk_review WHERE status = 0");
    }

    public long recentAuditCount() {
        return count("SELECT COUNT(*) FROM audit_log WHERE created_at >= DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 7 DAY)");
    }

    public List<RecommendationResponse> listRecommendations(
            String slotCode,
            String targetType,
            Integer status,
            int limit,
            int offset
    ) {
        QueryParts query = recommendationQuery(slotCode, targetType, status, false);
        query.args().add(limit);
        query.args().add(offset);
        return jdbcTemplate.query(
                query.sql() + " ORDER BY display_order ASC, updated_at DESC, id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> recommendation(rs, resolveTarget(rs.getString("target_type"), rs.getLong("target_id")).isPresent(), null),
                query.args().toArray());
    }

    public long countRecommendations(String slotCode, String targetType, Integer status) {
        QueryParts query = recommendationQuery(slotCode, targetType, status, true);
        return count(query.sql(), query.args().toArray());
    }

    public Optional<RecommendationResponse> findRecommendation(long id) {
        return jdbcTemplate.query(
                "SELECT id, slot_code, target_type, target_id, title_override, summary_override, display_order, "
                        + "status, start_time, end_time, updated_at FROM portal_recommendation WHERE id = ? LIMIT 1",
                (rs, rowNum) -> recommendation(rs, resolveTarget(rs.getString("target_type"), rs.getLong("target_id")).isPresent(), null),
                id).stream().findFirst();
    }

    public long createRecommendation(
            String slotCode,
            String targetType,
            long targetId,
            String titleOverride,
            String summaryOverride,
            int displayOrder,
            int status,
            LocalDateTime startTime,
            LocalDateTime endTime,
            long operatorId
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO portal_recommendation "
                            + "(slot_code, target_type, target_id, title_override, summary_override, display_order, "
                            + "status, start_time, end_time, operator_id) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, slotCode);
            ps.setString(2, targetType);
            ps.setLong(3, targetId);
            ps.setString(4, titleOverride);
            ps.setString(5, summaryOverride);
            ps.setInt(6, displayOrder);
            ps.setInt(7, status);
            ps.setObject(8, startTime);
            ps.setObject(9, endTime);
            ps.setLong(10, operatorId);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0 : key.longValue();
    }

    public void updateRecommendation(
            long id,
            String slotCode,
            String targetType,
            long targetId,
            String titleOverride,
            String summaryOverride,
            int displayOrder,
            int status,
            LocalDateTime startTime,
            LocalDateTime endTime,
            long operatorId
    ) {
        jdbcTemplate.update(
                "UPDATE portal_recommendation SET slot_code = ?, target_type = ?, target_id = ?, "
                        + "title_override = ?, summary_override = ?, display_order = ?, status = ?, "
                        + "start_time = ?, end_time = ?, operator_id = ? WHERE id = ?",
                slotCode,
                targetType,
                targetId,
                titleOverride,
                summaryOverride,
                displayOrder,
                status,
                startTime,
                endTime,
                operatorId,
                id);
    }

    public void offlineRecommendation(long id, long operatorId) {
        jdbcTemplate.update(
                "UPDATE portal_recommendation SET status = 0, operator_id = ? WHERE id = ?",
                operatorId,
                id);
    }

    public List<RiskReviewResponse> listRiskReviews(String riskType, Integer status, String severity, int limit, int offset) {
        QueryParts query = riskQuery(riskType, status, severity, false);
        query.args().add(limit);
        query.args().add(offset);
        return jdbcTemplate.query(
                query.sql() + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> risk(rs),
                query.args().toArray());
    }

    public long countRiskReviews(String riskType, Integer status, String severity) {
        QueryParts query = riskQuery(riskType, status, severity, true);
        return count(query.sql(), query.args().toArray());
    }

    public Optional<RiskReviewResponse> findRiskReview(long id) {
        return jdbcTemplate.query(
                "SELECT id, risk_type, target_type, target_id, severity, status, title, summary, decision, "
                        + "handler_id, handled_at, created_at, updated_at FROM risk_review WHERE id = ? LIMIT 1",
                (rs, rowNum) -> risk(rs),
                id).stream().findFirst();
    }

    public void handleRiskReview(long id, int status, String decision, long handlerId) {
        jdbcTemplate.update(
                "UPDATE risk_review SET status = ?, decision = ?, handler_id = ?, handled_at = CURRENT_TIMESTAMP WHERE id = ?",
                status,
                decision,
                handlerId,
                id);
    }

    public List<RecommendationResponse> activeRecommendations(String slotCode, int limit) {
        return jdbcTemplate.query(
                "SELECT id, slot_code, target_type, target_id, title_override, summary_override, display_order, "
                        + "status, start_time, end_time, updated_at FROM portal_recommendation "
                        + "WHERE slot_code = ? AND status = 1 "
                        + "AND (start_time IS NULL OR start_time <= CURRENT_TIMESTAMP) "
                        + "AND (end_time IS NULL OR end_time >= CURRENT_TIMESTAMP) "
                        + "ORDER BY display_order ASC, updated_at DESC, id DESC LIMIT ?",
                (rs, rowNum) -> recommendation(rs, true, null),
                slotCode,
                limit);
    }

    public Optional<PortalRecommendationItem> resolveTargetCard(RecommendationResponse recommendation) {
        return resolveTarget(recommendation.targetType(), recommendation.targetId())
                .map(card -> new PortalRecommendationItem(
                        recommendation.targetType(),
                        recommendation.targetId(),
                        override(recommendation.titleOverride(), card.title()),
                        override(recommendation.summaryOverride(), card.summary()),
                        card.tags(),
                        card.url(),
                        card.cityCode(),
                        card.updatedAt(),
                        recommendation.displayOrder()));
    }

    private Optional<TargetCard> resolveTarget(String targetType, long targetId) {
        return switch (targetType) {
            case "job" -> findJobCard(targetId);
            case "company" -> findCompanyCard(targetId);
            case "content" -> findContentCard(targetId);
            case "event" -> findEventCard(targetId);
            case "talent_snapshot" -> findTalentSnapshotCard(targetId);
            default -> Optional.empty();
        };
    }

    private Optional<TargetCard> findJobCard(long jobId) {
        return jdbcTemplate.query(
                "SELECT j.id, j.title, j.city_code, j.salary_min, j.salary_max, j.updated_at, c.company_name "
                        + "FROM job_post j JOIN company c ON c.id = j.company_id "
                        + "WHERE j.id = ? AND j.status = 2 AND j.audit_status = 2 AND c.auth_status = 2 LIMIT 1",
                (rs, rowNum) -> new TargetCard(
                        rs.getString("title"),
                        rs.getString("company_name") + " · " + salary(rs.getObject("salary_min", Integer.class), rs.getObject("salary_max", Integer.class)),
                        List.of("在线职位", "认证企业"),
                        "/jobs/" + rs.getLong("id"),
                        rs.getString("city_code"),
                        timestamp(rs, "updated_at")),
                jobId).stream().findFirst();
    }

    private Optional<TargetCard> findCompanyCard(long companyId) {
        return jdbcTemplate.query(
                "SELECT id, company_name, city_code, industry_code, updated_at FROM company "
                        + "WHERE id = ? AND auth_status = 2 LIMIT 1",
                (rs, rowNum) -> new TargetCard(
                        rs.getString("company_name"),
                        nullable(rs.getString("industry_code"), "认证企业"),
                        List.of("认证企业"),
                        "/companies/" + rs.getLong("id"),
                        rs.getString("city_code"),
                        timestamp(rs, "updated_at")),
                companyId).stream().findFirst();
    }

    private Optional<TargetCard> findContentCard(long contentId) {
        return jdbcTemplate.query(
                "SELECT id, title, summary, city_code, updated_at FROM cms_content "
                        + "WHERE id = ? AND status = 1 LIMIT 1",
                (rs, rowNum) -> new TargetCard(
                        rs.getString("title"),
                        nullable(rs.getString("summary"), "公开内容"),
                        List.of("公开内容"),
                        "/articles/" + rs.getLong("id"),
                        rs.getString("city_code"),
                        timestamp(rs, "updated_at")),
                contentId).stream().findFirst();
    }

    private Optional<TargetCard> findEventCard(long eventId) {
        return jdbcTemplate.query(
                "SELECT id, title, type_code, city_code, updated_at FROM activity_event "
                        + "WHERE id = ? AND status = 1 LIMIT 1",
                (rs, rowNum) -> new TargetCard(
                        rs.getString("title"),
                        nullable(rs.getString("type_code"), "公开招聘会"),
                        List.of("招聘会"),
                        "/job-fairs/" + rs.getLong("id"),
                        rs.getString("city_code"),
                        timestamp(rs, "updated_at")),
                eventId).stream().findFirst();
    }

    private Optional<TargetCard> findTalentSnapshotCard(long snapshotId) {
        return jdbcTemplate.query(
                "SELECT id, display_name_masked, city_code, category_code, experience_years, updated_at, "
                        + "JSON_UNQUOTE(JSON_EXTRACT(snapshot_json, '$.skills_summary')) AS skills_summary "
                        + "FROM candidate_publish_snapshot "
                        + "WHERE id = ? AND publishable_flag = 1 AND consent_status = 1 AND status = 1 "
                        + "AND visibility_scope IN (2, 4) LIMIT 1",
                (rs, rowNum) -> new TargetCard(
                        nullable(rs.getString("display_name_masked"), "发布快照"),
                        nullable(rs.getString("skills_summary"), "人才服务区发布快照"),
                        List.of("发布快照", nullable(rs.getString("category_code"), "公开摘要")),
                        "/portal/talent-service-area",
                        rs.getString("city_code"),
                        timestamp(rs, "updated_at")),
                snapshotId).stream().findFirst();
    }

    private QueryParts recommendationQuery(String slotCode, String targetType, Integer status, boolean countOnly) {
        StringBuilder sql = new StringBuilder(countOnly ? "SELECT COUNT(*) FROM portal_recommendation" :
                "SELECT id, slot_code, target_type, target_id, title_override, summary_override, display_order, "
                        + "status, start_time, end_time, updated_at FROM portal_recommendation");
        List<Object> args = new ArrayList<>();
        sql.append(" WHERE 1 = 1");
        if (slotCode != null) {
            sql.append(" AND slot_code = ?");
            args.add(slotCode);
        }
        if (targetType != null) {
            sql.append(" AND target_type = ?");
            args.add(targetType);
        }
        if (status != null) {
            sql.append(" AND status = ?");
            args.add(status);
        }
        return new QueryParts(sql.toString(), args);
    }

    private QueryParts riskQuery(String riskType, Integer status, String severity, boolean countOnly) {
        StringBuilder sql = new StringBuilder(countOnly ? "SELECT COUNT(*) FROM risk_review" :
                "SELECT id, risk_type, target_type, target_id, severity, status, title, summary, decision, "
                        + "handler_id, handled_at, created_at, updated_at FROM risk_review");
        List<Object> args = new ArrayList<>();
        sql.append(" WHERE 1 = 1");
        if (riskType != null) {
            sql.append(" AND risk_type = ?");
            args.add(riskType);
        }
        if (status != null) {
            sql.append(" AND status = ?");
            args.add(status);
        }
        if (severity != null) {
            sql.append(" AND severity = ?");
            args.add(severity);
        }
        return new QueryParts(sql.toString(), args);
    }

    private RecommendationResponse recommendation(ResultSet rs, boolean targetValid, String invalidReason) throws SQLException {
        return new RecommendationResponse(
                rs.getLong("id"),
                rs.getString("slot_code"),
                rs.getString("target_type"),
                rs.getLong("target_id"),
                rs.getString("title_override"),
                rs.getString("summary_override"),
                rs.getInt("display_order"),
                rs.getInt("status"),
                timestamp(rs, "start_time"),
                timestamp(rs, "end_time"),
                targetValid,
                invalidReason,
                timestamp(rs, "updated_at"));
    }

    private RiskReviewResponse risk(ResultSet rs) throws SQLException {
        return new RiskReviewResponse(
                rs.getLong("id"),
                rs.getString("risk_type"),
                rs.getString("target_type"),
                rs.getLong("target_id"),
                rs.getString("severity"),
                rs.getInt("status"),
                rs.getString("title"),
                rs.getString("summary"),
                rs.getString("decision"),
                rs.getObject("handler_id", Long.class),
                timestamp(rs, "handled_at"),
                timestamp(rs, "created_at"),
                timestamp(rs, "updated_at"));
    }

    private long count(String sql, Object... args) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class, args);
        return count == null ? 0 : count;
    }

    private LocalDateTime timestamp(ResultSet rs, String columnName) throws SQLException {
        return rs.getTimestamp(columnName) == null ? null : rs.getTimestamp(columnName).toLocalDateTime();
    }

    private String salary(Integer min, Integer max) {
        if (min == null && max == null) {
            return "薪资面议";
        }
        if (min == null) {
            return max + "以内";
        }
        if (max == null) {
            return min + "起";
        }
        return min + "-" + max;
    }

    private String nullable(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String override(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record QueryParts(String sql, List<Object> args) {
    }

    private record TargetCard(
            String title,
            String summary,
            List<String> tags,
            String url,
            String cityCode,
            LocalDateTime updatedAt
    ) {
    }
}
