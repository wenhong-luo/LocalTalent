package cn.localtalent.backend.auth.api;

import cn.localtalent.backend.auth.application.AuthService;
import cn.localtalent.backend.auth.oidc.OidcAuthService;
import cn.localtalent.backend.auth.oidc.OidcConfigResponse;
import cn.localtalent.backend.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OidcAuthService oidcAuthService;

    public AuthController(AuthService authService, OidcAuthService oidcAuthService) {
        this.authService = authService;
        this.oidcAuthService = oidcAuthService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthRegisterResponse> register(@RequestBody AuthRegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthLoginResponse> login(@RequestBody AuthLoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<AuthIdentityResponse> me(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        return ApiResponse.success(authService.me(authorizationHeader));
    }

    @GetMapping("/oidc/config")
    public ApiResponse<OidcConfigResponse> oidcConfig() {
        return ApiResponse.success(oidcAuthService.config());
    }

    @GetMapping("/oidc/login")
    public ResponseEntity<Void> oidcLogin(
            @RequestParam("identity_type") String identityType,
            @RequestParam(value = "redirect", required = false) String redirect
    ) {
        return oidcAuthService.login(identityType, redirect);
    }

    @GetMapping("/oidc/callback")
    public ResponseEntity<String> oidcCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state
    ) {
        return oidcAuthService.callback(code, state);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return oidcAuthService.logout();
    }
}
