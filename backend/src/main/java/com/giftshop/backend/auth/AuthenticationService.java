package com.giftshop.backend.auth;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.giftshop.backend.security.TokenService;
import com.giftshop.backend.user.User;
import com.giftshop.backend.user.UserCredentials;
import com.giftshop.backend.user.UserRepository;
import com.giftshop.backend.user.UserService;

@Service
public class AuthenticationService {

    private static final int MAX_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    /** Hash checked when the email is unknown, so both cases take the same time. */
    private final String dummyPasswordHash;

    public AuthenticationService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 TokenService tokenService,
                                 RefreshTokenService refreshTokenService,
                                 UserService userService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
        this.userService = userService;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-used-against-timing-attacks");
    }

    /**
     * Checks the credentials and opens a new session.
     *
     * <p>An unknown email and a wrong password produce the same exception, and take
     * about the same time (a password hash is always checked), so that nobody can
     * find out which emails are registered.
     *
     * @throws InvalidCredentialsException if the email or the password is wrong
     * @throws AccountDisabledException if the credentials are correct but the account is disabled
     */
    public LoginResult login(String email, String rawPassword) {
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new InvalidCredentialsException();
        }

        Optional<UserCredentials> credentials =
            userRepository.findCredentialsByEmail(UserService.normalizeEmail(email));

        if (credentials.isEmpty()) {
            passwordEncoder.matches(rawPassword, dummyPasswordHash);
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(rawPassword, credentials.get().passwordHash())) {
            throw new InvalidCredentialsException();
        }

        User user = credentials.get().user();
        if (!user.enabled()) {
            throw new AccountDisabledException();
        }

        return new LoginResult(
            tokenService.createAccessToken(user),
            refreshTokenService.issue(user.id()));
    }

    /**
     * Exchanges a refresh token for a new access token and a new refresh token.
     *
     * <p>The user is read again from the database, so a disabled account cannot
     * refresh, and a role change is taken into account in the new access token.
     *
     * @throws InvalidRefreshTokenException if the refresh token is not valid,
     *         or if the account is no longer active
     */
    public LoginResult refresh(String rawRefreshToken) {
        RotatedRefreshToken rotated = refreshTokenService.rotate(rawRefreshToken);

        if (!userService.isActive(rotated.userId())) {
            refreshTokenService.revoke(rotated.token());
            throw new InvalidRefreshTokenException();
        }

        User user = userService.getById(rotated.userId());
        return new LoginResult(tokenService.createAccessToken(user), rotated.token());
    }

    /** Ends the session of this refresh token. */
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }
}
