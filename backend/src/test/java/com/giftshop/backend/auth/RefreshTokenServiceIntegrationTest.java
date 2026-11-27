package com.giftshop.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.giftshop.backend.TestcontainersConfiguration;
import com.giftshop.backend.user.Role;
import com.giftshop.backend.user.User;
import com.giftshop.backend.user.UserRepository;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RefreshTokenServiceIntegrationTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID userId;

    @BeforeEach
    void setUp() {
        User user = new User(UUID.randomUUID(), "session-" + UUID.randomUUID() + "@example.com",
            Role.USER, true, Instant.now());
        userRepository.save(user, "fake-hash-for-tests");
        userId = user.id();
    }

    private int countTokensWithHash(String hash) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM refresh_tokens WHERE token_hash = ?", Integer.class, hash);
    }

    @Test
    void issue_storesOnlyTheHashOfTheToken() {
        String token = refreshTokenService.issue(userId);

        assertThat(countTokensWithHash(token)).isZero();
        assertThat(countTokensWithHash(RefreshTokenService.sha256(token))).isEqualTo(1);
    }

    @Test
    void rotate_returnsANewToken_andTheOldOneCanNoLongerBeUsed() {
        String first = refreshTokenService.issue(userId);

        RotatedRefreshToken rotated = refreshTokenService.rotate(first);

        assertThat(rotated.userId()).isEqualTo(userId);
        assertThat(rotated.token()).isNotEqualTo(first);
        assertThatThrownBy(() -> refreshTokenService.rotate(first))
            .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void reusingARotatedToken_revokesTheWholeFamily() {
        String stolen = refreshTokenService.issue(userId);
        String legitimate = refreshTokenService.rotate(stolen).token();

        // The thief uses the old token: reuse is detected
        assertThatThrownBy(() -> refreshTokenService.rotate(stolen))
            .isInstanceOf(InvalidRefreshTokenException.class);

        // The revocation was committed despite the exception (noRollbackFor):
        // the newest token of the family no longer works either
        assertThatThrownBy(() -> refreshTokenService.rotate(legitimate))
            .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void otherSessionsOfTheUser_areNotAffectedByATheft() {
        String phone = refreshTokenService.issue(userId);
        String laptop = refreshTokenService.issue(userId);
        refreshTokenService.rotate(phone);

        assertThatThrownBy(() -> refreshTokenService.rotate(phone))
            .isInstanceOf(InvalidRefreshTokenException.class);

        assertThatCode(() -> refreshTokenService.rotate(laptop)).doesNotThrowAnyException();
    }

    @Test
    void rotate_rejectsAnExpiredToken() {
        String token = refreshTokenService.issue(userId);
        jdbcTemplate.update(
            "UPDATE refresh_tokens SET expires_at = NOW() - INTERVAL '1 minute' WHERE token_hash = ?",
            RefreshTokenService.sha256(token));

        assertThatThrownBy(() -> refreshTokenService.rotate(token))
            .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void revoke_endsTheSession_andIgnoresUnknownTokens() {
        String token = refreshTokenService.issue(userId);

        refreshTokenService.revoke(token);

        assertThatThrownBy(() -> refreshTokenService.rotate(token))
            .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatCode(() -> refreshTokenService.revoke("unknown-token"))
            .doesNotThrowAnyException();
    }

    @Test
    void deletingTheUser_deletesTheirRefreshTokens() {
        String token = refreshTokenService.issue(userId);

        userRepository.deleteById(userId);

        assertThat(countTokensWithHash(RefreshTokenService.sha256(token))).isZero();
    }
}
