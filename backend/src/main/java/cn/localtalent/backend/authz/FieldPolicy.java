package cn.localtalent.backend.authz;

record FieldPolicy(
        String fieldName,
        FieldPolicyDecision decision,
        String maskRule
) {
}
