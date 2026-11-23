package com.giftshop.backend.user;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    /** BCrypt only accepts passwords up to 72 bytes. */
    static final int MAX_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user with the USER role.
     * The email is normalized and the password is hashed before being stored:
     * the raw password is never saved.
     *
     * @throws InvalidPasswordException if the password is too long for BCrypt
     * @throws EmailAlreadyExistsException if the email is already registered
     */
    public User register(String email, String rawPassword) {
        if (isTooLong(rawPassword)) {
            throw new InvalidPasswordException("Password is too long");
        }

        User user = new User(
            UUID.randomUUID(),
            normalizeEmail(email),
            Role.USER,
            true,
            Instant.now()
        );
        userRepository.save(user, passwordEncoder.encode(rawPassword));
        return user;
    }

    public User getById(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(UserNotFoundException::new);
    }

    /**
     * Checks that the user still exists and is enabled.
     *
     * <p>An access token stays valid until it expires, even if the account is
     * deleted or disabled in the meantime. Sensitive actions must therefore
     * check the account again instead of trusting the token alone.
     *
     * @throws InactiveAccountException if the account was deleted or disabled
     */
    public void requireActiveUser(UUID id) {
        User user = userRepository.findById(id)
            .orElseThrow(InactiveAccountException::new);
        if (!user.enabled()) {
            throw new InactiveAccountException();
        }
    }

    /**
     * Deletes the account (GDPR right to erasure).
     *
     * <p>The password is asked again, so that a stolen access token alone is not
     * enough to delete an account.
     *
     * @throws IncorrectPasswordException if the password is wrong
     * @throws UserNotFoundException if the account no longer exists
     */
    public void deleteAccount(UUID id, String rawPassword) {
        UserCredentials credentials = userRepository.findCredentialsById(id)
            .orElseThrow(UserNotFoundException::new);

        if (isTooLong(rawPassword)
                || !passwordEncoder.matches(rawPassword, credentials.passwordHash())) {
            throw new IncorrectPasswordException();
        }

        userRepository.deleteById(id);
    }

    private static boolean isTooLong(String rawPassword) {
        return rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES;
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
