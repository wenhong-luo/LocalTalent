package cn.localtalent.backend.audit;

import cn.localtalent.backend.authz.AuthzContext;
import cn.localtalent.backend.authz.AuthzPrincipal;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAspect {

    private final AuditService auditService;

    public AuditAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    @Around("@annotation(auditAction)")
    public Object recordAudit(ProceedingJoinPoint joinPoint, AuditAction auditAction) throws Throwable {
        Object result = joinPoint.proceed();
        AuthzPrincipal principal = AuthzContext.requireCurrentPrincipal();
        auditService.record(
                principal,
                auditAction.bizType(),
                bizId(joinPoint.getArgs(), auditAction.bizIdArgIndex()),
                auditAction.actionType(),
                null,
                null);
        return result;
    }

    private long bizId(Object[] args, int index) {
        if (index < 0 || index >= args.length || !(args[index] instanceof Number number)) {
            throw new IllegalArgumentException("audit bizIdArgIndex must point to a numeric argument");
        }
        return number.longValue();
    }
}
