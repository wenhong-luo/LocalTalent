package cn.localtalent.backend.authz;

public enum FieldPolicyDecision {
    DENY(0),
    MASK(1),
    ALLOW(2);

    private final int rank;

    FieldPolicyDecision(int rank) {
        this.rank = rank;
    }

    public boolean strongerThan(FieldPolicyDecision other) {
        return rank > other.rank;
    }

    public static FieldPolicyDecision parse(String value) {
        return FieldPolicyDecision.valueOf(value.trim().toUpperCase());
    }
}
