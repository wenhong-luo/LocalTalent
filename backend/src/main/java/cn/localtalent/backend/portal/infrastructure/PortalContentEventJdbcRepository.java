package cn.localtalent.backend.portal.infrastructure;

import cn.localtalent.backend.portal.domain.PortalContentRow;
import cn.localtalent.backend.portal.domain.PortalEventRow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PortalContentEventJdbcRepository {

    private static final int PUBLIC_STATUS = 1;

    private final JdbcTemplate jdbcTemplate;

    public PortalContentEventJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PortalContentRow> listContents(String contentType, String cityCode, int limit, int offset) {
        QueryParts query = contentQuery(contentType, cityCode, false);
        query.args().add(limit);
        query.args().add(offset);
        return jdbcTemplate.query(
                query.sql() + " ORDER BY COALESCE(publish_time, updated_at) DESC, id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> contentRow(rs),
                query.args().toArray());
    }

    public long countContents(String contentType, String cityCode) {
        QueryParts query = contentQuery(contentType, cityCode, true);
        Integer count = jdbcTemplate.queryForObject(query.sql(), Integer.class, query.args().toArray());
        return count == null ? 0 : count;
    }

    public Optional<PortalContentRow> findContentById(long contentId) {
        return jdbcTemplate.query(
                contentBaseSelect() + " WHERE id = ? AND status = ? LIMIT 1",
                (rs, rowNum) -> contentRow(rs),
                contentId,
                PUBLIC_STATUS).stream().findFirst();
    }

    public List<PortalEventRow> listEvents(String typeCode, String cityCode, int limit, int offset) {
        QueryParts query = eventQuery(typeCode, cityCode, false);
        query.args().add(limit);
        query.args().add(offset);
        return jdbcTemplate.query(
                query.sql() + " ORDER BY start_time DESC, updated_at DESC, id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> eventRow(rs),
                query.args().toArray());
    }

    public long countEvents(String typeCode, String cityCode) {
        QueryParts query = eventQuery(typeCode, cityCode, true);
        Integer count = jdbcTemplate.queryForObject(query.sql(), Integer.class, query.args().toArray());
        return count == null ? 0 : count;
    }

    public Optional<PortalEventRow> findEventById(long eventId) {
        return jdbcTemplate.query(
                eventBaseSelect() + " WHERE id = ? AND status = ? LIMIT 1",
                (rs, rowNum) -> eventRow(rs),
                eventId,
                PUBLIC_STATUS).stream().findFirst();
    }

    private QueryParts contentQuery(String contentType, String cityCode, boolean countOnly) {
        StringBuilder sql = new StringBuilder(countOnly ? "SELECT COUNT(*) FROM cms_content" : contentBaseSelect());
        List<Object> args = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        conditions.add("status = ?");
        args.add(PUBLIC_STATUS);
        if (contentType != null && !contentType.isBlank()) {
            conditions.add("content_type = ?");
            args.add(contentType.trim());
        }
        if (cityCode != null && !cityCode.isBlank()) {
            conditions.add("city_code = ?");
            args.add(cityCode.trim());
        }
        sql.append(" WHERE ").append(String.join(" AND ", conditions));
        return new QueryParts(sql.toString(), args);
    }

    private QueryParts eventQuery(String typeCode, String cityCode, boolean countOnly) {
        StringBuilder sql = new StringBuilder(countOnly ? "SELECT COUNT(*) FROM activity_event" : eventBaseSelect());
        List<Object> args = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        conditions.add("status = ?");
        args.add(PUBLIC_STATUS);
        if (typeCode != null && !typeCode.isBlank()) {
            conditions.add("type_code = ?");
            args.add(typeCode.trim());
        }
        if (cityCode != null && !cityCode.isBlank()) {
            conditions.add("city_code = ?");
            args.add(cityCode.trim());
        }
        sql.append(" WHERE ").append(String.join(" AND ", conditions));
        return new QueryParts(sql.toString(), args);
    }

    private String contentBaseSelect() {
        return "SELECT id, content_type, title, cover_url, summary, body_html, city_code, publish_time, updated_at "
                + "FROM cms_content";
    }

    private String eventBaseSelect() {
        return "SELECT id, title, type_code, city_code, start_time, end_time, location, status, updated_at "
                + "FROM activity_event";
    }

    private PortalContentRow contentRow(ResultSet rs) throws SQLException {
        return new PortalContentRow(
                rs.getLong("id"),
                rs.getString("content_type"),
                rs.getString("title"),
                rs.getString("cover_url"),
                rs.getString("summary"),
                rs.getString("body_html"),
                rs.getString("city_code"),
                rs.getTimestamp("publish_time") == null ? null : rs.getTimestamp("publish_time").toLocalDateTime(),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime());
    }

    private PortalEventRow eventRow(ResultSet rs) throws SQLException {
        return new PortalEventRow(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("type_code"),
                rs.getString("city_code"),
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time").toLocalDateTime(),
                rs.getString("location"),
                rs.getInt("status"),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime());
    }

    private record QueryParts(String sql, List<Object> args) {
    }
}
