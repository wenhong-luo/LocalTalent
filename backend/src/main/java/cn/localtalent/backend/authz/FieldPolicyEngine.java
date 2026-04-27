package cn.localtalent.backend.authz;

import cn.localtalent.backend.audit.FieldAccessRecorder;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class FieldPolicyEngine {

    private final FieldPolicyRepository fieldPolicyRepository;
    private final FieldAccessRecorder fieldAccessRecorder;

    public FieldPolicyEngine(
            FieldPolicyRepository fieldPolicyRepository,
            FieldAccessRecorder fieldAccessRecorder
    ) {
        this.fieldPolicyRepository = fieldPolicyRepository;
        this.fieldAccessRecorder = fieldAccessRecorder;
    }

    public Map<String, Object> apply(
            AuthzPrincipal principal,
            String bizType,
            long bizId,
            Collection<ProtectedField> fields
    ) {
        Map<String, ProtectedField> byName = new LinkedHashMap<>();
        for (ProtectedField field : fields) {
            byName.put(field.fieldName(), field);
        }

        Map<String, List<FieldPolicy>> policies = fieldPolicyRepository.findPolicies(
                principal.roleCodes(),
                bizType,
                byName.keySet());
        Map<String, Object> result = new LinkedHashMap<>();
        for (ProtectedField field : byName.values()) {
            FieldPolicyDecision decision = decide(field, policies.getOrDefault(field.fieldName(), List.of()));
            Object value = switch (decision) {
                case ALLOW -> field.value();
                case MASK -> mask(field.value(), strongestMaskRule(policies.getOrDefault(field.fieldName(), List.of())));
                case DENY -> null;
            };
            result.put(field.fieldName(), value);
            if (field.sensitivity() == FieldSensitivity.S3 || field.sensitivity() == FieldSensitivity.S4) {
                fieldAccessRecorder.record(principal, bizType, bizId, field.fieldName(), decision.name());
            }
        }
        return result;
    }

    private FieldPolicyDecision decide(ProtectedField field, List<FieldPolicy> policies) {
        FieldPolicyDecision decision = defaultDecision(field.sensitivity());
        for (FieldPolicy policy : policies) {
            if (policy.decision().strongerThan(decision)) {
                decision = policy.decision();
            }
        }
        return decision;
    }

    private FieldPolicyDecision defaultDecision(FieldSensitivity sensitivity) {
        return sensitivity == FieldSensitivity.S1 ? FieldPolicyDecision.ALLOW : FieldPolicyDecision.DENY;
    }

    private String strongestMaskRule(List<FieldPolicy> policies) {
        for (FieldPolicy policy : policies) {
            if (policy.decision() == FieldPolicyDecision.MASK && policy.maskRule() != null && !policy.maskRule().isBlank()) {
                return policy.maskRule();
            }
        }
        return "DEFAULT";
    }

    private Object mask(Object value, String maskRule) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return switch (maskRule == null ? "DEFAULT" : maskRule.toUpperCase()) {
            case "MOBILE" -> maskMobile(text);
            case "EMAIL" -> maskEmail(text);
            case "NAME" -> maskName(text);
            default -> "***";
        };
    }

    private String maskMobile(String value) {
        if (value.length() < 7) {
            return "***";
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    private String maskEmail(String value) {
        int at = value.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return value.charAt(0) + "***" + value.substring(at);
    }

    private String maskName(String value) {
        if (value.isBlank()) {
            return "***";
        }
        if (value.length() == 1) {
            return "*";
        }
        return value.charAt(0) + "*";
    }
}
