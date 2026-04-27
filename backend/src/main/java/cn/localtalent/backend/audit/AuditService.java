package cn.localtalent.backend.audit;

import cn.localtalent.backend.authz.AuthzPrincipal;
import cn.localtalent.backend.common.trace.TraceIdContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final JdbcTemplate jdbcTemplate;

    public AuditService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void record(
            AuthzPrincipal principal,
            String bizType,
            long bizId,
            String actionType,
            String beforeJson,
            String afterJson
    ) {
        jdbcTemplate.update(
                "INSERT INTO audit_log "
                        + "(operator_id, operator_role, biz_type, biz_id, action_type, before_json, after_json, trace_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                principal.userId(),
                principal.primaryRole(),
                bizType,
                bizId,
                actionType,
                beforeJson,
                afterJson,
                TraceIdContext.getCurrentTraceId());
    }
}
