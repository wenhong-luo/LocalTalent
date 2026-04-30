package cn.localtalent.backend.company.infrastructure;

import cn.localtalent.backend.company.api.PortalCompanySearchCriteria;
import cn.localtalent.backend.company.domain.PortalCompanyJobRow;
import cn.localtalent.backend.company.domain.PortalCompanyRow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PortalCompanyJdbcRepository {

    private static final int APPROVED = 2;

    private final JdbcTemplate jdbcTemplate;

    public PortalCompanyJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PortalCompanyRow> listVisible(PortalCompanySearchCriteria criteria, int limit, int offset) {
        QueryParts query = visibleQuery(criteria, false);
        query.args().add(limit);
        query.args().add(offset);
        return jdbcTemplate.query(
                query.sql() + " ORDER BY c.updated_at DESC, c.id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> companyRow(rs),
                query.args().toArray());
    }

    public long countVisible(PortalCompanySearchCriteria criteria) {
        QueryParts query = visibleQuery(criteria, true);
        Integer count = jdbcTemplate.queryForObject(query.sql(), Integer.class, query.args().toArray());
        return count == null ? 0 : count;
    }

    public Optional<PortalCompanyRow> findVisibleById(long companyId) {
        return jdbcTemplate.query(
                baseSelect() + " WHERE c.id = ? AND c.auth_status = ? GROUP BY c.id LIMIT 1",
                (rs, rowNum) -> companyRow(rs),
                companyId,
                APPROVED).stream().findFirst();
    }

    public List<PortalCompanyJobRow> listVisibleJobs(long companyId, int limit) {
        return jdbcTemplate.query(
                "SELECT id, title, category_code, city_code, salary_min, salary_max, updated_at "
                        + "FROM job_post "
                        + "WHERE company_id = ? AND status = 2 AND audit_status = 2 "
                        + "ORDER BY updated_at DESC, id DESC LIMIT ?",
                (rs, rowNum) -> jobRow(rs),
                companyId,
                limit);
    }

    private QueryParts visibleQuery(PortalCompanySearchCriteria criteria, boolean countOnly) {
        StringBuilder sql = new StringBuilder(countOnly ? "SELECT COUNT(*) FROM company c" : baseSelect());
        List<Object> args = new ArrayList<>();
        sql.append(" WHERE c.auth_status = ?");
        args.add(APPROVED);
        if (criteria == null) {
            return new QueryParts(sql.toString(), args);
        }
        if ("0".equals(criteria.verified())) {
            sql.append(" AND 1 = 0");
        }
        if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
            sql.append(" AND (c.company_name LIKE ? OR c.company_profile LIKE ?)");
            String keyword = "%" + criteria.keyword().trim() + "%";
            args.add(keyword);
            args.add(keyword);
        }
        if (criteria.cityCode() != null && !criteria.cityCode().isBlank()) {
            sql.append(" AND c.city_code = ?");
            args.add(criteria.cityCode().trim());
        }
        if (criteria.industryCode() != null && !criteria.industryCode().isBlank()) {
            sql.append(" AND c.industry_code = ?");
            args.add(criteria.industryCode().trim());
        }
        if (criteria.natureCode() != null && !criteria.natureCode().isBlank()) {
            sql.append(" AND c.nature_code = ?");
            args.add(criteria.natureCode().trim());
        }
        if (criteria.scaleCode() != null && !criteria.scaleCode().isBlank()) {
            sql.append(" AND c.scale_code = ?");
            args.add(criteria.scaleCode().trim());
        }
        return new QueryParts(sql.toString(), args);
    }

    private String baseSelect() {
        return "SELECT c.id, c.company_name, c.city_code, c.industry_code, c.nature_code, c.scale_code, "
                + "c.company_profile, c.updated_at, "
                + "(SELECT COUNT(*) FROM job_post j "
                + "WHERE j.company_id = c.id AND j.status = 2 AND j.audit_status = 2) AS open_job_count "
                + "FROM company c";
    }

    private PortalCompanyRow companyRow(ResultSet rs) throws SQLException {
        return new PortalCompanyRow(
                rs.getLong("id"),
                rs.getString("company_name"),
                rs.getString("city_code"),
                rs.getString("industry_code"),
                rs.getString("nature_code"),
                rs.getString("scale_code"),
                rs.getString("company_profile"),
                rs.getLong("open_job_count"),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime());
    }

    private PortalCompanyJobRow jobRow(ResultSet rs) throws SQLException {
        return new PortalCompanyJobRow(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("category_code"),
                rs.getString("city_code"),
                rs.getObject("salary_min", Integer.class),
                rs.getObject("salary_max", Integer.class),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime());
    }

    private record QueryParts(String sql, List<Object> args) {
    }
}
