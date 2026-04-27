package cn.localtalent.backend.authz;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AuthzWebConfig implements WebMvcConfigurer {

    private final AuthzInterceptor authzInterceptor;

    public AuthzWebConfig(AuthzInterceptor authzInterceptor) {
        this.authzInterceptor = authzInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authzInterceptor);
    }
}
