package cn.localtalent.backend.authz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcFieldPolicyRepository implements FieldPolicyRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcFieldPolicyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<String, List<FieldPolicy>> findPolicies(
            Collection<String> roleCodes,
            String bizType,
            Collection<String> fieldNames
    ) {
        if (roleCodes.isEmpty() || fieldNames.isEmpty()) {
            return Map.of();
        }
        String rolePlaceholders = String.join(",", roleCodes.stream().map(ignored -> "?").toList());
        String fieldPlaceholders = String.join(",", fieldNames.stream().map(ignored -> "?").toList());
        List<Object> args = new ArrayList<>(roleCodes);
        args.add(bizType);
        args.addAll(fieldNames);

        return jdbcTemplate.query(
                "SELECT p.field_name, p.policy_type, p.mask_rule FROM sys_role_field_policy p "
                        + "JOIN sys_role r ON r.id = p.role_id "
                        + "WHERE r.status = 1 AND p.status = 1 "
                        + "AND r.role_code IN (" + rolePlaceholders + ") "
                        + "AND p.biz_type = ? AND p.field_name IN (" + fieldPlaceholders + ")",
                rs -> {
                    Map<String, List<FieldPolicy>> policies = new LinkedHashMap<>();
                    while (rs.next()) {
                        FieldPolicy policy = new FieldPolicy(
                                rs.getString("field_name"),
                                FieldPolicyDecision.parse(rs.getString("policy_type")),
                                rs.getString("mask_rule"));
                        policies.computeIfAbsent(policy.fieldName(), ignored -> new ArrayList<>()).add(policy);
                    }
                    return policies;
                },
                args.toArray());
    }
}
