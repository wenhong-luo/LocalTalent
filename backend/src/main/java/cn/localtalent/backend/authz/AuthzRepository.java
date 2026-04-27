package cn.localtalent.backend.authz;

import cn.localtalent.backend.auth.domain.IdentityType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuthzRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuthzRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<AuthzAccountSnapshot> findAccount(IdentityType identityType, long userId) {
        return switch (identityType) {
            case CANDIDATE -> findCandidate(userId);
            case COMPANY -> findCompanyUser(userId);
            case OPERATOR -> findAdminUser(userId);
        };
    }

    public List<String> findExplicitRoleCodes(IdentityType identityType, long userId) {
        return jdbcTemplate.query(
                "SELECT r.role_code FROM sys_user_role ur "
                        + "JOIN sys_role r ON r.id = ur.role_id "
                        + "WHERE ur.user_type = ? AND ur.user_id = ? AND r.status = 1 "
                        + "ORDER BY r.role_code",
                (rs, rowNum) -> rs.getString("role_code"),
                userType(identityType),
                userId);
    }

    public boolean hasPermission(Collection<String> roleCodes, String apiCode) {
        if (roleCodes.isEmpty() || apiCode == null || apiCode.isBlank()) {
            return false;
        }
        String placeholders = String.join(",", roleCodes.stream().map(ignored -> "?").toList());
        List<Object> args = new ArrayList<>(roleCodes);
        args.add(apiCode);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role r "
                        + "JOIN sys_role_menu rm ON rm.role_id = r.id "
                        + "JOIN sys_menu m ON m.id = rm.menu_id "
                        + "WHERE r.status = 1 AND m.status = 1 "
                        + "AND r.role_code IN (" + placeholders + ") AND m.api_code = ?",
                Integer.class,
                args.toArray());
        return count != null && count > 0;
    }

    private Optional<AuthzAccountSnapshot> findCandidate(long userId) {
        return jdbcTemplate.query(
                "SELECT id, status FROM candidate_user WHERE id = ? LIMIT 1",
                (rs, rowNum) -> new AuthzAccountSnapshot(
                        IdentityType.CANDIDATE,
                        rs.getLong("id"),
                        null,
                        "candidate",
                        rs.getInt("status")),
                userId).stream().findFirst();
    }

    private Optional<AuthzAccountSnapshot> findCompanyUser(long userId) {
        return jdbcTemplate.query(
                "SELECT id, company_id, role_code, status FROM company_user WHERE id = ? LIMIT 1",
                (rs, rowNum) -> new AuthzAccountSnapshot(
                        IdentityType.COMPANY,
                        rs.getLong("id"),
                        rs.getLong("company_id"),
                        rs.getString("role_code"),
                        rs.getInt("status")),
                userId).stream().findFirst();
    }

    private Optional<AuthzAccountSnapshot> findAdminUser(long userId) {
        return jdbcTemplate.query(
                "SELECT id, role_code, status FROM admin_user WHERE id = ? LIMIT 1",
                (rs, rowNum) -> new AuthzAccountSnapshot(
                        IdentityType.OPERATOR,
                        rs.getLong("id"),
                        null,
                        rs.getString("role_code"),
                        rs.getInt("status")),
                userId).stream().findFirst();
    }

    private String userType(IdentityType identityType) {
        return switch (identityType) {
            case CANDIDATE -> "candidate";
            case COMPANY -> "company_user";
            case OPERATOR -> "admin_user";
        };
    }
}
