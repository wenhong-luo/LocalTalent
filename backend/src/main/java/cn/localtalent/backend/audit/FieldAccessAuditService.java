package cn.localtalent.backend.audit;

import cn.localtalent.backend.authz.AuthzPrincipal;
import cn.localtalent.backend.common.trace.TraceIdContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class FieldAccessAuditService implements FieldAccessRecorder {

    private final JdbcTemplate jdbcTemplate;

    public FieldAccessAuditService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void record(
            AuthzPrincipal principal,
            String bizType,
            long bizId,
            String fieldName,
            String accessType
    ) {
        jdbcTemplate.update(
                "INSERT INTO field_access_log "
                        + "(operator_id, operator_role, biz_type, biz_id, field_name, access_type, trace_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                principal.userId(),
                principal.primaryRole(),
                bizType,
                bizId,
                fieldName,
                accessType,
                TraceIdContext.getCurrentTraceId());
    }
}
