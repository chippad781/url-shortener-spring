package com.linksnip.auth;

import com.linksnip.auth.dto.*;
import com.linksnip.common.ClientIp;
import com.linksnip.ratelimit.RateLimitService;
import com.linksnip.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RateLimitService rateLimitService;

    @Value("${app.ratelimit.login-per-minute}")
    private int loginPerMinute;

    public AuthController(AuthService authService, RateLimitService rateLimitService) {
        this.authService = authService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenPair register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public TokenPair login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        // brute-force protection: per-IP fixed window
        rateLimitService.check(
                "rl:login:" + ClientIp.resolve(http),
                loginPerMinute,
                Duration.ofMinutes(1),
                "Too many login attempts. Try again shortly.");
        return authService.login(req);
    }

    @PostMapping("/refresh")
    public TokenPair refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req);
    }

    @GetMapping("/profile")
    public ProfileResponse profile(@AuthenticationPrincipal AuthenticatedUser principal) {
        return authService.profile(principal.getId());
    }

    @PatchMapping("/profile")
    public ProfileResponse updateProfile(@AuthenticationPrincipal AuthenticatedUser principal,
                                         @Valid @RequestBody ProfileUpdateRequest req) {
        return authService.updateProfile(principal.getId(), req);
    }
}
