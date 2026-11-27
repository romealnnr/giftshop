package com.giftshop.backend.auth;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.giftshop.backend.security.RateLimiter;
import com.giftshop.backend.user.User;
import com.giftshop.backend.user.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** Against password guessing: at most 10 login attempts per minute from one IP address. */
    static final int LOGIN_ATTEMPTS_PER_MINUTE = 10;

    static final String REFRESH_COOKIE = "giftshop_refresh";

    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final RateLimiter rateLimiter;

    public AuthController(UserService userService,
                          AuthenticationService authenticationService,
                          RateLimiter rateLimiter) {
        this.userService = userService;
        this.authenticationService = authenticationService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public User register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request.email(), request.password());
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        rateLimiter.check("login", httpRequest.getRemoteAddr(),
            LOGIN_ATTEMPTS_PER_MINUTE, Duration.ofMinutes(1));
        return withRefreshCookie(authenticationService.login(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken == null) {
            throw new InvalidRefreshTokenException();
        }
        return withRefreshCookie(authenticationService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken != null) {
            authenticationService.logout(refreshToken);
        }
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
            .build();
    }

    private static ResponseEntity<TokenResponse> withRefreshCookie(LoginResult result) {
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE,
                refreshCookie(result.refreshToken(), RefreshTokenService.TIME_TO_LIVE).toString())
            .body(TokenResponse.from(result.accessToken()));
    }

    /**
     * The refresh token cookie:
     * HttpOnly (JavaScript cannot read it), Secure (HTTPS only, localhost excepted),
     * SameSite=Strict (never sent from another site: CSRF protection),
     * Path=/api/auth (never sent to the rest of the API).
     */
    private static ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .path("/api/auth")
            .maxAge(maxAge)
            .build();
    }
}
