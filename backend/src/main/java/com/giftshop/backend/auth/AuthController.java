package com.giftshop.backend.auth;

import java.time.Duration;

import org.springframework.http.HttpStatus;
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
    public TokenResponse login(@Valid @RequestBody LoginRequest request,
                               HttpServletRequest httpRequest) {
        rateLimiter.check("login", httpRequest.getRemoteAddr(),
            LOGIN_ATTEMPTS_PER_MINUTE, Duration.ofMinutes(1));
        return TokenResponse.from(authenticationService.login(request.email(), request.password()));
    }
}
