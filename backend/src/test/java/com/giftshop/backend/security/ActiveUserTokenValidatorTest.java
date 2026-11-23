package com.giftshop.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import com.giftshop.backend.user.UserService;

@ExtendWith(MockitoExtension.class)
class ActiveUserTokenValidatorTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private ActiveUserTokenValidator validator;

    private static Jwt tokenWithSubject(String subject) {
        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject(subject)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(900))
            .build();
    }

    @Test
    void acceptsTheTokenOfAnActiveUser() {
        UUID userId = UUID.randomUUID();
        when(userService.isActive(userId)).thenReturn(true);

        assertThat(validator.validate(tokenWithSubject(userId.toString())).hasErrors()).isFalse();
    }

    @Test
    void rejectsTheTokenOfADeletedOrDisabledUser() {
        UUID userId = UUID.randomUUID();
        when(userService.isActive(userId)).thenReturn(false);

        assertThat(validator.validate(tokenWithSubject(userId.toString())).getErrors())
            .extracting(error -> error.getErrorCode())
            .containsExactly("invalid_token");
    }

    @Test
    void rejectsATokenWhoseSubjectIsNotAUserId() {
        assertThat(validator.validate(tokenWithSubject("not-a-uuid")).hasErrors()).isTrue();
    }
}
