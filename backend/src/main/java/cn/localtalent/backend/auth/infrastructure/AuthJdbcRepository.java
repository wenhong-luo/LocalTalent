package cn.localtalent.backend.auth.infrastructure;

import cn.localtalent.backend.auth.domain.AuthAccount;
import cn.localtalent.backend.auth.domain.AuthIdentity;
import cn.localtalent.backend.auth.domain.IdentityType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AuthJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuthJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long createCandidate(String mobile, String email, String passwordHash, String displayName) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO candidate_user (mobile, email, password_hash, real_name, register_channel, status) "
                            + "VALUES (?, ?, ?, ?, 'portal', 1)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, mobile);
            statement.setString(2, email);
            statement.setString(3, passwordHash);
            statement.setString(4, displayName);
            return statement;
        }, keyHolder);
        return generatedId(keyHolder);
    }

    public long createCompany(
            String companyName,
            String licenseNo,
            String userName,
            String mobile,
            String email,
            String passwordHash
    ) {
        long companyId = createCompanyRow(companyName, licenseNo);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO company_user (company_id, user_name, role_code, mobile, email, password_hash, status) "
                            + "VALUES (?, ?, 'company_admin', ?, ?, ?, 1)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, companyId);
            statement.setString(2, userName);
            statement.setString(3, mobile);
            statement.setString(4, email);
            statement.setString(5, passwordHash);
            return statement;
        }, keyHolder);
        return generatedId(keyHolder);
    }

    public Optional<AuthAccount> findCandidateByAccount(String account) {
        List<AuthAccount> accounts = jdbcTemplate.query(
                "SELECT id, password_hash, COALESCE(real_name, '求职者') AS display_name, status "
                        + "FROM candidate_user WHERE mobile = ? OR email = ? LIMIT 1",
                (rs, rowNum) -> new AuthAccount(
                        IdentityType.CANDIDATE,
                        rs.getLong("id"),
                        null,
                        rs.getString("display_name"),
                        rs.getInt("status"),
                        rs.getString("password_hash")),
                account,
                account);
        return accounts.stream().findFirst();
    }

    public Optional<AuthAccount> findCompanyByAccount(String account) {
        List<AuthAccount> accounts = jdbcTemplate.query(
                "SELECT id, company_id, password_hash, user_name AS display_name, status "
                        + "FROM company_user WHERE mobile = ? OR email = ? ORDER BY id LIMIT 1",
                (rs, rowNum) -> new AuthAccount(
                        IdentityType.COMPANY,
                        rs.getLong("id"),
                        rs.getLong("company_id"),
                        rs.getString("display_name"),
                        rs.getInt("status"),
                        rs.getString("password_hash")),
                account,
                account);
        return accounts.stream().findFirst();
    }

    public Optional<AuthAccount> findOperatorByAccount(String account) {
        List<AuthAccount> accounts = jdbcTemplate.query(
                "SELECT id, password_hash, display_name, status FROM admin_user WHERE username = ? LIMIT 1",
                (rs, rowNum) -> new AuthAccount(
                        IdentityType.OPERATOR,
                        rs.getLong("id"),
                        null,
                        rs.getString("display_name"),
                        rs.getInt("status"),
                        rs.getString("password_hash")),
                account);
        return accounts.stream().findFirst();
    }

    public Optional<AuthIdentity> findIdentity(IdentityType identityType, long userId) {
        return switch (identityType) {
            case CANDIDATE -> findCandidateIdentity(userId);
            case COMPANY -> findCompanyIdentity(userId);
            case OPERATOR -> findOperatorIdentity(userId);
        };
    }

    public void updateLastLogin(AuthIdentity identity) {
        if (identity.identityType() == IdentityType.COMPANY) {
            jdbcTemplate.update("UPDATE company_user SET last_login_at = CURRENT_TIMESTAMP WHERE id = ?", identity.userId());
        }
        if (identity.identityType() == IdentityType.OPERATOR) {
            jdbcTemplate.update("UPDATE admin_user SET last_login_at = CURRENT_TIMESTAMP WHERE id = ?", identity.userId());
        }
    }

    private long createCompanyRow(String companyName, String licenseNo) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO company (company_name, license_no, auth_status, source_system) VALUES (?, ?, 1, 'portal')",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, companyName);
            statement.setString(2, licenseNo);
            return statement;
        }, keyHolder);
        return generatedId(keyHolder);
    }

    private Optional<AuthIdentity> findCandidateIdentity(long userId) {
        List<AuthIdentity> identities = jdbcTemplate.query(
                "SELECT id, COALESCE(real_name, '求职者') AS display_name, status FROM candidate_user WHERE id = ? LIMIT 1",
                (rs, rowNum) -> identity(IdentityType.CANDIDATE, rs, null),
                userId);
        return identities.stream().findFirst();
    }

    private Optional<AuthIdentity> findCompanyIdentity(long userId) {
        List<AuthIdentity> identities = jdbcTemplate.query(
                "SELECT cu.id, cu.company_id, CONCAT(c.company_name, '/', cu.user_name) AS display_name, cu.status "
                        + "FROM company_user cu JOIN company c ON c.id = cu.company_id WHERE cu.id = ? LIMIT 1",
                (rs, rowNum) -> identity(IdentityType.COMPANY, rs, rs.getLong("company_id")),
                userId);
        return identities.stream().findFirst();
    }

    private Optional<AuthIdentity> findOperatorIdentity(long userId) {
        List<AuthIdentity> identities = jdbcTemplate.query(
                "SELECT id, display_name, status FROM admin_user WHERE id = ? LIMIT 1",
                (rs, rowNum) -> identity(IdentityType.OPERATOR, rs, null),
                userId);
        return identities.stream().findFirst();
    }

    private AuthIdentity identity(IdentityType type, ResultSet rs, Long companyId) throws SQLException {
        return new AuthIdentity(
                type,
                rs.getLong("id"),
                companyId,
                rs.getString("display_name"),
                rs.getInt("status"));
    }

    private long generatedId(KeyHolder keyHolder) {
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("database did not return generated id");
        }
        return key.longValue();
    }
}
