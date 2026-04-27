package cn.localtalent.backend.auth.application;

import cn.localtalent.backend.auth.api.AuthIdentityResponse;
import cn.localtalent.backend.auth.api.AuthLoginRequest;
import cn.localtalent.backend.auth.api.AuthLoginResponse;
import cn.localtalent.backend.auth.api.AuthRegisterRequest;
import cn.localtalent.backend.auth.api.AuthRegisterResponse;
import cn.localtalent.backend.auth.domain.AuthAccount;
import cn.localtalent.backend.auth.domain.AuthIdentity;
import cn.localtalent.backend.auth.domain.IdentityType;
import cn.localtalent.backend.auth.domain.JwtPrincipal;
import cn.localtalent.backend.auth.infrastructure.AuthJdbcRepository;
import cn.localtalent.backend.auth.infrastructure.JwtService;
import cn.localtalent.backend.auth.infrastructure.PasswordHasher;
import cn.localtalent.backend.common.exception.ApiException;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;

    private final AuthJdbcRepository authRepository;
    private final PasswordHasher passwordHasher;
    private final JwtService jwtService;

    public AuthService(AuthJdbcRepository authRepository, PasswordHasher passwordHasher, JwtService jwtService) {
        this.authRepository = authRepository;
        this.passwordHasher = passwordHasher;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthRegisterResponse register(AuthRegisterRequest request) {
        IdentityType identityType = IdentityType.parse(request.identityType());
        if (identityType == IdentityType.OPERATOR) {
            throw validation("operator self registration is not supported");
        }

        String password = required(request.password(), "password");
        validatePassword(password);
        String passwordHash = passwordHasher.hash(password);

        try {
            AuthIdentity identity = switch (identityType) {
                case CANDIDATE -> registerCandidate(request, passwordHash);
                case COMPANY -> registerCompany(request, passwordHash);
                case OPERATOR -> throw validation("operator self registration is not supported");
            };
            return new AuthRegisterResponse(toResponse(identity));
        } catch (DuplicateKeyException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "AUTH_409", "account already exists");
        }
    }

    @Transactional
    public AuthLoginResponse login(AuthLoginRequest request) {
        IdentityType identityType = IdentityType.parse(request.identityType());
        String account = normalizeAccount(required(request.account(), "account"));
        String password = required(request.password(), "password");

        AuthAccount authAccount = findAccount(identityType, account)
                .filter(accountRow -> accountRow.status() == 1)
                .filter(accountRow -> passwordHasher.matches(password, accountRow.passwordHash()))
                .orElseThrow(this::unauthorized);

        AuthIdentity identity = new AuthIdentity(
                authAccount.identityType(),
                authAccount.userId(),
                authAccount.companyId(),
                authAccount.displayName(),
                authAccount.status());
        authRepository.updateLastLogin(identity);

        String token = jwtService.issueToken(identity);
        return new AuthLoginResponse(token, "Bearer", jwtService.ttlSeconds(), toResponse(identity));
    }

    @Transactional(readOnly = true)
    public AuthIdentityResponse me(String authorizationHeader) {
        String token = bearerToken(authorizationHeader);
        JwtPrincipal principal = jwtService.parse(token);
        AuthIdentity identity = authRepository.findIdentity(principal.identityType(), principal.userId())
                .filter(identityRow -> identityRow.status() == 1)
                .orElseThrow(this::unauthorized);
        return toResponse(identity);
    }

    private AuthIdentity registerCandidate(AuthRegisterRequest request, String passwordHash) {
        String mobile = normalizeNullable(request.mobile());
        String email = normalizeEmail(request.email());
        if (mobile == null && email == null) {
            throw validation("mobile or email is required");
        }

        String displayName = optionalText(request.displayName(), 64);
        long userId = authRepository.createCandidate(mobile, email, passwordHash, displayName);
        String responseName = displayName == null ? "求职者" : displayName;
        return new AuthIdentity(IdentityType.CANDIDATE, userId, null, responseName, 1);
    }

    private AuthIdentity registerCompany(AuthRegisterRequest request, String passwordHash) {
        String companyName = limit(required(request.companyName(), "company_name"), 200);
        String licenseNo = limit(required(request.licenseNo(), "license_no"), 64);
        String userName = limit(required(request.userName(), "user_name"), 100);
        String mobile = normalizeNullable(request.mobile());
        String email = normalizeEmail(request.email());
        if (mobile == null && email == null) {
            throw validation("mobile or email is required");
        }

        long userId = authRepository.createCompany(companyName, licenseNo, userName, mobile, email, passwordHash);
        return authRepository.findIdentity(IdentityType.COMPANY, userId)
                .orElseGet(() -> new AuthIdentity(IdentityType.COMPANY, userId, null, userName, 1));
    }

    private Optional<AuthAccount> findAccount(IdentityType identityType, String account) {
        return switch (identityType) {
            case CANDIDATE -> authRepository.findCandidateByAccount(account);
            case COMPANY -> authRepository.findCompanyByAccount(account);
            case OPERATOR -> authRepository.findOperatorByAccount(account);
        };
    }

    private AuthIdentityResponse toResponse(AuthIdentity identity) {
        return new AuthIdentityResponse(
                identity.identityType().value(),
                identity.userId(),
                identity.companyId(),
                identity.displayName(),
                identity.status());
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

    private String required(String value, String fieldName) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw validation(fieldName + " is required");
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeEmail(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeAccount(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private String optionalText(String value, int maxLength) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        return limit(normalized, maxLength);
    }

    private String limit(String value, int maxLength) {
        if (value.length() > maxLength) {
            throw validation("field length is too long");
        }
        return value;
    }

    private void validatePassword(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            throw validation("password length must be between 8 and 128");
        }
    }

    private ApiException validation(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALID_400", message);
    }

    private ApiException unauthorized() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_401", "invalid credentials");
    }
}
