package cn.localtalent.backend.auth.domain;

import cn.localtalent.backend.common.exception.ApiException;
import java.util.Locale;
import org.springframework.http.HttpStatus;

public enum IdentityType {
    CANDIDATE("candidate"),
    COMPANY("company"),
    OPERATOR("operator");

    private final String value;

    IdentityType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static IdentityType parse(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALID_400", "identity_type is required");
        }
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        for (IdentityType type : values()) {
            if (type.value.equals(normalized)) {
                return type;
            }
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "VALID_400", "unsupported identity_type");
    }
}
