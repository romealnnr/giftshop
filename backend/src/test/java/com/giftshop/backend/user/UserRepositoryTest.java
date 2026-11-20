package com.giftshop.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.giftshop.backend.TestcontainersConfiguration;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

    private static final String HASH = "fake-hash-for-tests";

    @Autowired
    private UserRepository repository;

    @Autowired
    private UserJpaRepository jpaRepository;

    @BeforeEach
    void cleanDatabase() {
        jpaRepository.deleteAll();
    }

    private User userWithEmail(String email) {
        return new User(UUID.randomUUID(), email, Role.USER, true, Instant.now());
    }

    @Test
    void save_savesUser_whenEmailIsNew() {
        User user = userWithEmail("alice@example.com");

        repository.save(user, HASH);

        assertThat(repository.findById(user.id()))
            .hasValueSatisfying(found -> {
                assertThat(found.email()).isEqualTo("alice@example.com");
                assertThat(found.role()).isEqualTo(Role.USER);
                assertThat(found.enabled()).isTrue();
            });
    }

    @Test
    void save_throwsException_whenEmailAlreadyExists() {
        repository.save(userWithEmail("alice@example.com"), HASH);
        User duplicate = userWithEmail("alice@example.com");

        assertThatThrownBy(() -> repository.save(duplicate, HASH))
            .isInstanceOf(EmailAlreadyExistsException.class);

        assertThat(repository.findById(duplicate.id())).isEmpty();
    }

    @Test
    void findCredentialsByEmail_returnsPasswordHash() {
        User user = userWithEmail("alice@example.com");
        repository.save(user, HASH);

        assertThat(repository.findCredentialsByEmail("alice@example.com"))
            .hasValueSatisfying(credentials -> {
                assertThat(credentials.user().id()).isEqualTo(user.id());
                assertThat(credentials.passwordHash()).isEqualTo(HASH);
            });
    }

    @Test
    void database_rejectsEmailThatIsNotLowercase() {
        User user = userWithEmail("Alice@Example.com");

        assertThatThrownBy(() -> repository.save(user, HASH))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
