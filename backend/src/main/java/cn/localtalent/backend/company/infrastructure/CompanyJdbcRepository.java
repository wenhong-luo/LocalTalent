package cn.localtalent.backend.company.infrastructure;

import cn.localtalent.backend.company.api.CompanyApplyRequest;
import cn.localtalent.backend.company.domain.CompanyReviewRow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CompanyJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public CompanyJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<CompanyReviewRow> findById(long companyId) {
        return jdbcTemplate.query(
                "SELECT id, company_name, license_no, industry_code, scale_code, city_code, address, "
                        + "company_profile, auth_status, auth_reject_reason, auth_review_user_id, "
                        + "auth_review_time, auth_submit_time "
                        + "FROM company WHERE id = ? LIMIT 1",
                (rs, rowNum) -> row(rs),
                companyId).stream().findFirst();
    }

    public void submitApplication(long companyId, CompanyApplyRequest request) {
        jdbcTemplate.update(
                "UPDATE company SET company_name = ?, license_no = ?, industry_code = ?, scale_code = ?, "
                        + "city_code = ?, address = ?, company_profile = ?, auth_status = 1, "
                        + "auth_reject_reason = NULL, auth_review_user_id = NULL, auth_review_time = NULL, "
                        + "auth_submit_time = CURRENT_TIMESTAMP WHERE id = ?",
                request.companyName(),
                request.licenseNo(),
                request.industryCode(),
                request.scaleCode(),
                request.cityCode(),
                request.address(),
                request.companyProfile(),
                companyId);
    }

    public void review(long companyId, int authStatus, String rejectReason, long reviewerId) {
        jdbcTemplate.update(
                "UPDATE company SET auth_status = ?, auth_reject_reason = ?, auth_review_user_id = ?, "
                        + "auth_review_time = CURRENT_TIMESTAMP WHERE id = ?",
                authStatus,
                rejectReason,
                reviewerId,
                companyId);
    }

    public List<CompanyReviewRow> listForReview(Integer authStatus, int limit, int offset) {
        if (authStatus == null) {
            return jdbcTemplate.query(
                    "SELECT id, company_name, license_no, industry_code, scale_code, city_code, address, "
                            + "company_profile, auth_status, auth_reject_reason, auth_review_user_id, "
                            + "auth_review_time, auth_submit_time "
                            + "FROM company ORDER BY updated_at DESC LIMIT ? OFFSET ?",
                    (rs, rowNum) -> row(rs),
                    limit,
                    offset);
        }
        return jdbcTemplate.query(
                "SELECT id, company_name, license_no, industry_code, scale_code, city_code, address, "
                        + "company_profile, auth_status, auth_reject_reason, auth_review_user_id, "
                        + "auth_review_time, auth_submit_time "
                        + "FROM company WHERE auth_status = ? ORDER BY updated_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> row(rs),
                authStatus,
                limit,
                offset);
    }

    public long countForReview(Integer authStatus) {
        Integer count;
        if (authStatus == null) {
            count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM company", Integer.class);
        } else {
            count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM company WHERE auth_status = ?",
                    Integer.class,
                    authStatus);
        }
        return count == null ? 0 : count;
    }

    private CompanyReviewRow row(ResultSet rs) throws SQLException {
        Long reviewUserId = rs.getObject("auth_review_user_id", Long.class);
        return new CompanyReviewRow(
                rs.getLong("id"),
                rs.getString("company_name"),
                rs.getString("license_no"),
                rs.getString("industry_code"),
                rs.getString("scale_code"),
                rs.getString("city_code"),
                rs.getString("address"),
                rs.getString("company_profile"),
                rs.getInt("auth_status"),
                rs.getString("auth_reject_reason"),
                reviewUserId,
                rs.getTimestamp("auth_review_time") == null ? null : rs.getTimestamp("auth_review_time").toLocalDateTime(),
                rs.getTimestamp("auth_submit_time") == null ? null : rs.getTimestamp("auth_submit_time").toLocalDateTime());
    }
}
