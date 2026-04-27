package cn.localtalent.backend.admin.cms.infrastructure;

import cn.localtalent.backend.admin.cms.api.CmsContentRequest;
import cn.localtalent.backend.admin.cms.domain.CmsContentRow;
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
public class CmsContentJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public CmsContentJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long create(CmsContentRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO cms_content "
                            + "(content_type, title, cover_url, summary, body_html, city_code, status, publish_time) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, CASE WHEN ? = 1 THEN CURRENT_TIMESTAMP ELSE NULL END)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.contentType());
            statement.setString(2, request.title());
            statement.setString(3, request.coverUrl());
            statement.setString(4, request.summary());
            statement.setString(5, request.bodyHtml());
            statement.setString(6, request.cityCode());
            statement.setInt(7, request.status());
            statement.setInt(8, request.status());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("cms content id was not generated");
        }
        return key.longValue();
    }

    public Optional<CmsContentRow> findById(long contentId) {
        return jdbcTemplate.query(baseSelect() + " WHERE id = ? LIMIT 1",
                (rs, rowNum) -> row(rs),
                contentId).stream().findFirst();
    }

    public List<CmsContentRow> list(String contentType, Integer status, int limit, int offset) {
        QueryParts query = query(contentType, status, false);
        query.args().add(limit);
        query.args().add(offset);
        return jdbcTemplate.query(
                query.sql() + " ORDER BY updated_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> row(rs),
                query.args().toArray());
    }

    public long count(String contentType, Integer status) {
        QueryParts query = query(contentType, status, true);
        Integer count = jdbcTemplate.queryForObject(query.sql(), Integer.class, query.args().toArray());
        return count == null ? 0 : count;
    }

    public void update(long contentId, CmsContentRequest request) {
        jdbcTemplate.update(
                "UPDATE cms_content SET content_type = ?, title = ?, cover_url = ?, summary = ?, body_html = ?, "
                        + "city_code = ?, status = ?, "
                        + "publish_time = CASE WHEN ? = 1 THEN COALESCE(publish_time, CURRENT_TIMESTAMP) ELSE NULL END "
                        + "WHERE id = ?",
                request.contentType(),
                request.title(),
                request.coverUrl(),
                request.summary(),
                request.bodyHtml(),
                request.cityCode(),
                request.status(),
                request.status(),
                contentId);
    }

    public void softDelete(long contentId) {
        jdbcTemplate.update(
                "UPDATE cms_content SET status = 0, publish_time = NULL WHERE id = ?",
                contentId);
    }

    private QueryParts query(String contentType, Integer status, boolean countOnly) {
        StringBuilder sql = new StringBuilder(countOnly ? "SELECT COUNT(*) FROM cms_content" : baseSelect());
        List<Object> args = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        if (contentType != null && !contentType.isBlank()) {
            conditions.add("content_type = ?");
            args.add(contentType.trim());
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
        return "SELECT id, content_type, title, cover_url, summary, body_html, city_code, status, publish_time, updated_at "
                + "FROM cms_content";
    }

    private CmsContentRow row(ResultSet rs) throws SQLException {
        return new CmsContentRow(
                rs.getLong("id"),
                rs.getString("content_type"),
                rs.getString("title"),
                rs.getString("cover_url"),
                rs.getString("summary"),
                rs.getString("body_html"),
                rs.getString("city_code"),
                rs.getInt("status"),
                rs.getTimestamp("publish_time") == null ? null : rs.getTimestamp("publish_time").toLocalDateTime(),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime());
    }

    private record QueryParts(String sql, List<Object> args) {
    }
}
