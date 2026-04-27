package cn.localtalent.backend.authz;

import cn.localtalent.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public final class AuthzContext {

    private static final ThreadLocal<AuthzPrincipal> PRINCIPAL = new ThreadLocal<>();

    private AuthzContext() {
    }

    public static void setCurrentPrincipal(AuthzPrincipal principal) {
        PRINCIPAL.set(principal);
    }

    public static AuthzPrincipal requireCurrentPrincipal() {
        AuthzPrincipal principal = PRINCIPAL.get();
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_401", "authentication required");
        }
        return principal;
    }

    public static AuthzPrincipal currentPrincipalOrNull() {
        return PRINCIPAL.get();
    }

    public static void clear() {
        PRINCIPAL.remove();
    }
}
