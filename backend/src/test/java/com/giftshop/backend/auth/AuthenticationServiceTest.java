package com.giftshop.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.giftshop.backend.security.AccessToken;
import com.giftshop.backend.security.TokenService;
import com.giftshop.backend.user.Role;
import com.giftshop.backend.user.User;
import com.giftshop.backend.user.UserCredentials;
import com.giftshop.backend.user.UserRepository;

class AuthenticationServiceTest {

    private static final String PASSWORD = "correct-horse-battery";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final TokenService tokenService = mock(TokenService.class);
    private final PasswordEncoder passwordEncoder =
        spy(PasswordEncoderFactories.createDelegatingPasswordEncoder());

    private AuthenticationService authenticationService;
    private String aliceHash;

    private final AccessToken token = new AccessToken("signed-token", 900);

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(userRepository, passwordEncoder, tokenService);
        aliceHash = passwordEncoder.encode(PASSWORD);
    }

    private User alice(boolean enabled) {
        return new User(UUID.randomUUID(), "alice@example.com", Role.USER, enabled, Instant.now());
    }

    private void registered(User user) {
        when(userRepository.findCredentialsByEmail("alice@example.com"))
            .thenReturn(Optional.of(new UserCredentials(user, aliceHash)));
    }

    @Test
    void login_returnsToken_whenCredentialsAreValid() {
        User alice = alice(true);
        registered(alice);
        when(tokenService.createAccessToken(alice)).thenReturn(token);

        assertThat(authenticationService.login("alice@example.com", PASSWORD)).isEqualTo(token);
    }

    @Test
    void login_normalizesEmail() {
        User alice = alice(true);
        registered(alice);
        when(tokenService.createAccessToken(alice)).thenReturn(token);

        assertThat(authenticationService.login("  Alice@Example.COM ", PASSWORD)).isEqualTo(token);
    }

    @Test
    void login_rejectsWrongPassword() {
        registered(alice(true));

        assertThatThrownBy(() -> authenticationService.login("alice@example.com", "wrong-password-123"))
            .isInstanceOf(InvalidCredentialsException.class);

        verify(tokenService, never()).createAccessToken(any());
    }

    @Test
    void login_givesSameErrorForUnknownEmailAndWrongPassword() {
        Throwable unknownEmail =
            catchThrowable(() -> authenticationService.login("nobody@example.com", PASSWORD));

        registered(alice(true));
        Throwable wrongPassword =
            catchThrowable(() -> authenticationService.login("alice@example.com", "wrong-password-123"));

        assertThat(unknownEmail)
            .isInstanceOf(InvalidCredentialsException.class)
            .hasMessage(wrongPassword.getMessage());
    }

    @Test
    void login_checksAPasswordHash_evenWhenEmailIsUnknown() {
        catchThrowable(() -> authenticationService.login("nobody@example.com", PASSWORD));

        verify(passwordEncoder).matches(eq(PASSWORD), anyString());
    }

    @Test
    void login_revealsDisabledAccount_onlyAfterCorrectPassword() {
        registered(alice(false));

        assertThatThrownBy(() -> authenticationService.login("alice@example.com", "wrong-password-123"))
            .isInstanceOf(InvalidCredentialsException.class);

        assertThatThrownBy(() -> authenticationService.login("alice@example.com", PASSWORD))
            .isInstanceOf(AccountDisabledException.class);
    }
}
