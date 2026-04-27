package cn.localtalent.backend.authz;

import cn.localtalent.backend.auth.domain.JwtPrincipal;
import cn.localtalent.backend.auth.infrastructure.JwtService;
import cn.localtalent.backend.common.exception.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AuthzPrincipalResolver {

    private final JwtService jwtService;
    private final AuthzRepository authzRepository;

    public AuthzPrincipalResolver(JwtService jwtService, AuthzRepository authzRepository) {
        this.jwtService = jwtService;
        this.authzRepository = authzRepository;
    }

    public AuthzPrincipal resolve(String authorizationHeader) {
        JwtPrincipal jwtPrincipal = jwtService.parse(bearerToken(authorizationHeader));
        AuthzAccountSnapshot account = authzRepository.findAccount(
                        jwtPrincipal.identityType(),
                        jwtPrincipal.userId())
                .filter(accountRow -> accountRow.status() == 1)
                .orElseThrow(this::unauthorized);

        List<String> roleCodes = authzRepository.findExplicitRoleCodes(
                jwtPrincipal.identityType(),
                jwtPrincipal.userId());
        if (roleCodes.isEmpty()) {
            roleCodes = List.of(account.defaultRoleCode());
        }

        return new AuthzPrincipal(
                jwtPrincipal.identityType(),
                jwtPrincipal.userId(),
                account.companyId(),
                roleCodes,
                jwtPrincipal.tokenId());
    }

    private String bearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw unauthorized();
        }
        String prefix = "Bearer ";
        if (!authorizationHeader.regionMatches(true, 0, prefix, 0, prefix.length())) {
            throw unauthorized();
        }
        String token = authorizationHeader.substring(prefix.length()).trim();
        if (token.isBlank()) {
            throw unauthorized();
        }
        return token;
    }

    private ApiException unauthorized() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_401", "authentication required");
    }
}
