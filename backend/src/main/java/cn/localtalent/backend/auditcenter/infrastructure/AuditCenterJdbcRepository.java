package cn.localtalent.backend.auditcenter.infrastructure;

import cn.localtalent.backend.auditcenter.domain.AuditLogQuery;
import cn.localtalent.backend.auditcenter.domain.AuditLogRow;
import cn.localtalent.backend.auditcenter.domain.FieldAccessLogQuery;
import cn.localtalent.backend.auditcenter.domain.FieldAccessLogRow;
import cn.localtalent.backend.auditcenter.domain.OpenApiLogQuery;
import cn.localtalent.backend.auditcenter.domain.OpenApiLogRow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuditCenterJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuditCenterJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AuditLogRow> listAuditLogs(AuditLogQuery query, int limit, int offset) {
        QueryParts parts = auditLogQuery(query, false);
        parts.args().add(limit);
        parts.args().add(offset);
        return jdbcTemplate.query(
                parts.sql() + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> auditRow(rs),
                parts.args().toArray());
    }

    public long countAuditLogs(AuditLogQuery query) {
        QueryParts parts = auditLogQuery(query, true);
        return count(parts);
    }

    public List<FieldAccessLogRow> listFieldAccessLogs(FieldAccessLogQuery query, int limit, int offset) {
        QueryParts parts = fieldAccessLogQuery(query, false);
        parts.args().add(limit);
        parts.args().add(offset);
        return jdbcTemplate.query(
                parts.sql() + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> fieldAccessRow(rs),
                parts.args().toArray());
    }

    public long countFieldAccessLogs(FieldAccessLogQuery query) {
        QueryParts parts = fieldAccessLogQuery(query, true);
        return count(parts);
    }

    public List<OpenApiLogRow> listOpenApiLogs(OpenApiLogQuery query, int limit, int offset) {
        QueryParts parts = openApiLogQuery(query, false);
        parts.args().add(limit);
        parts.args().add(offset);
        return jdbcTemplate.query(
                parts.sql() + " ORDER BY request_time DESC, id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> openApiRow(rs),
                parts.args().toArray());
    }

    public long countOpenApiLogs(OpenApiLogQuery query) {
        QueryParts parts = openApiLogQuery(query, true);
        return count(parts);
    }

    private QueryParts auditLogQuery(AuditLogQuery query, boolean countOnly) {
        StringBuilder sql = new StringBuilder(countOnly
                ? "SELECT COUNT(*) FROM audit_log"
                : "SELECT id, operator_id, operator_role, biz_type, biz_id, action_type, "
                        + "before_json, after_json, trace_id, created_at FROM audit_log");
        List<Object> args = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        addStringCondition(conditions, args, "trace_id", query.traceId());
        addStringCondition(conditions, args, "biz_type", query.bizType());
        addLongCondition(conditions, args, "biz_id", query.bizId());
        addStringCondition(conditions, args, "action_type", query.actionType());
        addLongCondition(conditions, args, "operator_id", query.operatorId());
        addStringCondition(conditions, args, "operator_role", query.operatorRole());
        addTimeRange(conditions, args, "created_at", query.startTime(), query.endTime());
        appendWhere(sql, conditions);
        return new QueryParts(sql.toString(), args);
    }

    private QueryParts fieldAccessLogQuery(FieldAccessLogQuery query, boolean countOnly) {
        StringBuilder sql = new StringBuilder(countOnly
                ? "SELECT COUNT(*) FROM field_access_log"
                : "SELECT id, operator_id, operator_role, biz_type, biz_id, field_name, "
                        + "access_type, trace_id, created_at FROM field_access_log");
        List<Object> args = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        addStringCondition(conditions, args, "trace_id", query.traceId());
        addStringCondition(conditions, args, "biz_type", query.bizType());
        addLongCondition(conditions, args, "biz_id", query.bizId());
        addStringCondition(conditions, args, "field_name", query.fieldName());
        addStringCondition(conditions, args, "access_type", query.accessType());
        addLongCondition(conditions, args, "operator_id", query.operatorId());
        addStringCondition(conditions, args, "operator_role", query.operatorRole());
        addTimeRange(conditions, args, "created_at", query.startTime(), query.endTime());
        appendWhere(sql, conditions);
        return new QueryParts(sql.toString(), args);
    }

    private QueryParts openApiLogQuery(OpenApiLogQuery query, boolean countOnly) {
        StringBuilder sql = new StringBuilder(countOnly
                ? "SELECT COUNT(*) FROM open_api_log"
                : "SELECT id, source_system, client_code, api_code, trace_id, request_uri, request_method, "
                        + "biz_type, biz_id, http_status, success_flag, request_hash, idempotency_key, "
                        + "duration_ms, request_summary_json, response_summary_json, request_time, response_time, "
                        + "error_msg FROM open_api_log");
        List<Object> args = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        addStringCondition(conditions, args, "trace_id", query.traceId());
        addStringCondition(conditions, args, "client_code", query.clientCode());
        addStringCondition(conditions, args, "source_system", query.sourceSystem());
        addStringCondition(conditions, args, "api_code", query.apiCode());
        if (query.successFlag() != null) {
            conditions.add("success_flag = ?");
            args.add(query.successFlag() ? 1 : 0);
        }
        addIntegerCondition(conditions, args, "http_status", query.httpStatus());
        addTimeRange(conditions, args, "request_time", query.startTime(), query.endTime());
        appendWhere(sql, conditions);
        return new QueryParts(sql.toString(), args);
    }

    private void addStringCondition(List<String> conditions, List<Object> args, String column, String value) {
        if (value != null && !value.isBlank()) {
            conditions.add(column + " = ?");
            args.add(value.trim());
        }
    }

    private void addLongCondition(List<String> conditions, List<Object> args, String column, Long value) {
        if (value != null) {
            conditions.add(column + " = ?");
            args.add(value);
        }
    }

    private void addIntegerCondition(List<String> conditions, List<Object> args, String column, Integer value) {
        if (value != null) {
            conditions.add(column + " = ?");
            args.add(value);
        }
    }

    private void addTimeRange(
            List<String> conditions,
            List<Object> args,
            String column,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        if (startTime != null) {
            conditions.add(column + " >= ?");
            args.add(Timestamp.valueOf(startTime));
        }
        if (endTime != null) {
            conditions.add(column + " <= ?");
            args.add(Timestamp.valueOf(endTime));
        }
    }

    private void appendWhere(StringBuilder sql, List<String> conditions) {
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
    }

    private long count(QueryParts parts) {
        Long value = jdbcTemplate.queryForObject(parts.sql(), Long.class, parts.args().toArray());
        return value == null ? 0 : value;
    }

    private AuditLogRow auditRow(ResultSet rs) throws SQLException {
        return new AuditLogRow(
                rs.getLong("id"),
                nullableLong(rs, "operator_id"),
                rs.getString("operator_role"),
                rs.getString("biz_type"),
                nullableLong(rs, "biz_id"),
                rs.getString("action_type"),
                rs.getString("before_json"),
                rs.getString("after_json"),
                rs.getString("trace_id"),
                nullableTime(rs, "created_at"));
    }

    private FieldAccessLogRow fieldAccessRow(ResultSet rs) throws SQLException {
        return new FieldAccessLogRow(
                rs.getLong("id"),
                nullableLong(rs, "operator_id"),
                rs.getString("operator_role"),
                rs.getString("biz_type"),
                nullableLong(rs, "biz_id"),
                rs.getString("field_name"),
                rs.getString("access_type"),
                rs.getString("trace_id"),
                nullableTime(rs, "created_at"));
    }

    private OpenApiLogRow openApiRow(ResultSet rs) throws SQLException {
        return new OpenApiLogRow(
                rs.getLong("id"),
                rs.getString("source_system"),
                rs.getString("client_code"),
                rs.getString("api_code"),
                rs.getString("trace_id"),
                rs.getString("request_uri"),
                rs.getString("request_method"),
                rs.getString("biz_type"),
                nullableLong(rs, "biz_id"),
                nullableInteger(rs, "http_status"),
                nullableBoolean(rs, "success_flag"),
                rs.getString("request_hash"),
                rs.getString("idempotency_key"),
                nullableLong(rs, "duration_ms"),
                rs.getString("request_summary_json"),
                rs.getString("response_summary_json"),
                nullableTime(rs, "request_time"),
                nullableTime(rs, "response_time"),
                rs.getString("error_msg"));
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Integer nullableInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private Boolean nullableBoolean(ResultSet rs, String column) throws SQLException {
        boolean value = rs.getBoolean(column);
        return rs.wasNull() ? null : value;
    }

    private LocalDateTime nullableTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record QueryParts(String sql, List<Object> args) {
    }
}
