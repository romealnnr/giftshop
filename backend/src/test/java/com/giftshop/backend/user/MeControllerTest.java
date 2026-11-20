package com.giftshop.backend.user;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.giftshop.backend.security.SecurityConfig;


@WebMvcTest(MeController.class)
@Import(SecurityConfig.class)
class MeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private final User alice = new User(
        UUID.randomUUID(), "alice@example.com", Role.USER, true, Instant.now());

    @Test
    void me_returns401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void me_returns401_withInvalidToken() throws Exception {
        mockMvc.perform(get("/api/me")
                .header("Authorization", "Bearer not-a-real-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void me_returnsTheUserFromTheToken() throws Exception {
        when(userService.getById(alice.id())).thenReturn(alice);

        mockMvc.perform(get("/api/me")
                .with(jwt().jwt(token -> token.subject(alice.id().toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("alice@example.com"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
}