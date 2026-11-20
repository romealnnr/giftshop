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
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
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

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}