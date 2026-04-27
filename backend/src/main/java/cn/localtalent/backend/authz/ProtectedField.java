package cn.localtalent.backend.authz;

public record ProtectedField(
        String fieldName,
        Object value,
        FieldSensitivity sensitivity
) {
}
