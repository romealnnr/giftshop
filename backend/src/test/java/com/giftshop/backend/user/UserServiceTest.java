package com.giftshop.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String PASSWORD = "correct-horse-battery";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user(boolean enabled) {
        return new User(UUID.randomUUID(), "alice@example.com", Role.USER, enabled, Instant.now());
    }

    // ---------- register ----------

    @Test
    void register_storesHashedPassword_neverTheRawPassword() {
        when(passwordEncoder.encode(PASSWORD)).thenReturn("hashed-password");

        userService.register("alice@example.com", PASSWORD);

        verify(userRepository).save(any(User.class), eq("hashed-password"));
        verify(userRepository, never()).save(any(User.class), eq(PASSWORD));
    }

    @Test
    void register_normalizesEmail() {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");

        User user = userService.register("  Alice@Example.COM  ", PASSWORD);

        assertThat(user.email()).isEqualTo("alice@example.com");
    }

    @Test
    void register_alwaysCreatesEnabledUserWithUserRole() {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);

        userService.register("alice@example.com", PASSWORD);

        verify(userRepository).save(savedUser.capture(), anyString());
        assertThat(savedUser.getValue().role()).isEqualTo(Role.USER);
        assertThat(savedUser.getValue().enabled()).isTrue();
        assertThat(savedUser.getValue().id()).isNotNull();
    }

    @Test
    void register_rejectsPasswordLongerThan72Bytes() {
        String tooLong = "é".repeat(40); // 40 characters, but 80 bytes in UTF-8

        assertThatThrownBy(() -> userService.register("alice@example.com", tooLong))
            .isInstanceOf(InvalidPasswordException.class);

        verify(userRepository, never()).save(any(), any());
    }

    @Test
    void register_propagatesException_whenEmailAlreadyExists() {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
        doThrow(new EmailAlreadyExistsException(null))
            .when(userRepository).save(any(User.class), anyString());

        assertThatThrownBy(() -> userService.register("alice@example.com", PASSWORD))
            .isInstanceOf(EmailAlreadyExistsException.class);
    }

    // ---------- requireActiveUser ----------

    @Test
    void requireActiveUser_acceptsEnabledUser() {
        User alice = user(true);
        when(userRepository.findById(alice.id())).thenReturn(Optional.of(alice));

        assertThatCode(() -> userService.requireActiveUser(alice.id()))
            .doesNotThrowAnyException();
    }

    @Test
    void requireActiveUser_rejectsDeletedUser() {
        UUID deletedUserId = UUID.randomUUID();
        when(userRepository.findById(deletedUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.requireActiveUser(deletedUserId))
            .isInstanceOf(InactiveAccountException.class);
    }

    @Test
    void requireActiveUser_rejectsDisabledUser() {
        User disabled = user(false);
        when(userRepository.findById(disabled.id())).thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> userService.requireActiveUser(disabled.id()))
            .isInstanceOf(InactiveAccountException.class);
    }

    // ---------- deleteAccount ----------

    @Test
    void deleteAccount_deletesUser_whenPasswordIsCorrect() {
        User alice = user(true);
        when(userRepository.findCredentialsById(alice.id()))
            .thenReturn(Optional.of(new UserCredentials(alice, "stored-hash")));
        when(passwordEncoder.matches(PASSWORD, "stored-hash")).thenReturn(true);

        userService.deleteAccount(alice.id(), PASSWORD);

        verify(userRepository).deleteById(alice.id());
    }

    @Test
    void deleteAccount_keepsUser_whenPasswordIsWrong() {
        User alice = user(true);
        when(userRepository.findCredentialsById(alice.id()))
            .thenReturn(Optional.of(new UserCredentials(alice, "stored-hash")));
        when(passwordEncoder.matches("wrong-password-123", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteAccount(alice.id(), "wrong-password-123"))
            .isInstanceOf(IncorrectPasswordException.class);

        verify(userRepository, never()).deleteById(any());
    }
}
