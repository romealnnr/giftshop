package com.giftshop.backend.auth;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.giftshop.backend.security.AccessToken;
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

    /** Hash checked when the email is unknown, so both cases take the same time. */
    private final String dummyPasswordHash;

    public AuthenticationService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-used-against-timing-attacks");
    }

    /**
     * Checks the credentials and returns an access token.
     *
     * <p>An unknown email and a wrong password produce the same exception, and take
     * about the same time (a password hash is always checked), so that nobody can
     * find out which emails are registered.
     *
     * @throws InvalidCredentialsException if the email or the password is wrong
     * @throws AccountDisabledException if the credentials are correct but the account is disabled
     */
    public AccessToken login(String email, String rawPassword) {
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

        return tokenService.createAccessToken(user);
    }
}