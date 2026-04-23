package com.intelligenthealthcare.auth.api;

import com.intelligenthealthcare.auth.api.dto.CurrentPatientResponse;
import com.intelligenthealthcare.auth.api.dto.LoginRequest;
import com.intelligenthealthcare.auth.api.dto.RegisterRequest;
import com.intelligenthealthcare.auth.api.dto.TokenResponse;
import com.intelligenthealthcare.auth.application.AuthService;
import com.intelligenthealthcare.auth.domain.PatientAuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 患者注册、登录与当前会话信息；受保护请求需在 Header 中携带 {@code Authorization: Bearer &lt;accessToken&gt;}。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public CurrentPatientResponse me(@AuthenticationPrincipal PatientAuthPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return authService.me(principal);
    }
}
