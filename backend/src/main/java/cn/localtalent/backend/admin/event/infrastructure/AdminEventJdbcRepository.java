package cn.localtalent.backend.admin.event.infrastructure;

import cn.localtalent.backend.admin.event.api.AdminEventRequest;
import cn.localtalent.backend.admin.event.domain.AdminEventRow;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AdminEventJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminEventJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long create(AdminEventRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO activity_event "
                            + "(title, type_code, city_code, start_time, end_time, location, organizer_company_id, status) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.title());
            statement.setString(2, request.typeCode());
            statement.setString(3, request.cityCode());
            statement.setTimestamp(4, Timestamp.valueOf(request.startTime()));
            statement.setTimestamp(5, Timestamp.valueOf(request.endTime()));
            statement.setString(6, request.location());
            if (request.organizerCompanyId() == null) {
                statement.setObject(7, null);
            } else {
                statement.setLong(7, request.organizerCompanyId());
            }
            statement.setInt(8, request.status());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("activity event id was not generated");
        }
        return key.longValue();
    }

    public Optional<AdminEventRow> findById(long eventId) {
        return jdbcTemplate.query(baseSelect() + " WHERE id = ? LIMIT 1",
                (rs, rowNum) -> row(rs),
                eventId).stream().findFirst();
    }

    public List<AdminEventRow> list(String typeCode, String cityCode, Integer status, int limit, int offset) {
        QueryParts query = query(typeCode, cityCode, status, false);
        query.args().add(limit);
        query.args().add(offset);
        return jdbcTemplate.query(
                query.sql() + " ORDER BY start_time DESC, updated_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> row(rs),
                query.args().toArray());
    }

    public long count(String typeCode, String cityCode, Integer status) {
        QueryParts query = query(typeCode, cityCode, status, true);
        Integer count = jdbcTemplate.queryForObject(query.sql(), Integer.class, query.args().toArray());
        return count == null ? 0 : count;
    }

    public void update(long eventId, AdminEventRequest request) {
        jdbcTemplate.update(
                "UPDATE activity_event SET title = ?, type_code = ?, city_code = ?, start_time = ?, end_time = ?, "
                        + "location = ?, organizer_company_id = ?, status = ? WHERE id = ?",
                request.title(),
                request.typeCode(),
                request.cityCode(),
                Timestamp.valueOf(request.startTime()),
                Timestamp.valueOf(request.endTime()),
                request.location(),
                request.organizerCompanyId(),
                request.status(),
                eventId);
    }

    public void softDelete(long eventId) {
        jdbcTemplate.update("UPDATE activity_event SET status = 0 WHERE id = ?", eventId);
    }

    private QueryParts query(String typeCode, String cityCode, Integer status, boolean countOnly) {
        StringBuilder sql = new StringBuilder(countOnly ? "SELECT COUNT(*) FROM activity_event" : baseSelect());
        List<Object> args = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        if (typeCode != null && !typeCode.isBlank()) {
            conditions.add("type_code = ?");
            args.add(typeCode.trim());
        }
        if (cityCode != null && !cityCode.isBlank()) {
            conditions.add("city_code = ?");
            args.add(cityCode.trim());
        }
        if (status != null) {
            conditions.add("status = ?");
            args.add(status);
        }
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        return new QueryParts(sql.toString(), args);
    }

    private String baseSelect() {
        return "SELECT id, title, type_code, city_code, start_time, end_time, location, organizer_company_id, status, updated_at "
                + "FROM activity_event";
    }

    private AdminEventRow row(ResultSet rs) throws SQLException {
        return new AdminEventRow(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("type_code"),
                rs.getString("city_code"),
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time").toLocalDateTime(),
                rs.getString("location"),
                rs.getObject("organizer_company_id", Long.class),
                rs.getInt("status"),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime());
    }

    private record QueryParts(String sql, List<Object> args) {
    }
}
