package cn.localtalent.backend.authz;

import cn.localtalent.backend.common.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthzInterceptor implements HandlerInterceptor {

    private final AuthzPrincipalResolver principalResolver;
    private final AuthzRepository authzRepository;

    public AuthzInterceptor(AuthzPrincipalResolver principalResolver, AuthzRepository authzRepository) {
        this.principalResolver = principalResolver;
        this.authzRepository = authzRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequirePermission requirePermission = requirePermission(handlerMethod);
        if (requirePermission == null) {
            return true;
        }

        AuthzPrincipal principal = principalResolver.resolve(request.getHeader("Authorization"));
        if (!authzRepository.hasPermission(principal.roleCodes(), requirePermission.value())) {
            throw forbidden("permission denied");
        }

        AuthzContext.setCurrentPrincipal(principal);
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        AuthzContext.clear();
    }

    private RequirePermission requirePermission(HandlerMethod handlerMethod) {
        RequirePermission methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(),
                RequirePermission.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequirePermission.class);
    }

    private ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "AUTHZ_403", message);
    }
}
