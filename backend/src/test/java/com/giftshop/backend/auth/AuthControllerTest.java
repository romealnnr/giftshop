package com.giftshop.backend.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.giftshop.backend.security.SecurityConfig;
import com.giftshop.backend.user.EmailAlreadyExistsException;
import com.giftshop.backend.user.InvalidPasswordException;
import com.giftshop.backend.user.Role;
import com.giftshop.backend.user.User;
import com.giftshop.backend.user.UserService;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private static final String VALID_BODY = """
        {"email": "alice@example.com", "password": "correct-horse-battery"}
        """;

    @Test
    void register_returns201WithUser_withoutPassword() throws Exception {
        User user = new User(UUID.randomUUID(), "alice@example.com", Role.USER, true, Instant.now());
        when(userService.register("alice@example.com", "correct-horse-battery")).thenReturn(user);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("alice@example.com"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void register_returns400_whenEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email": "not-an-email", "password": "correct-horse-battery"}
                    """))
            .andExpect(status().isBadRequest());

        verify(userService, never()).register(anyString(), anyString());
    }

    @Test
    void register_returns400_whenPasswordIsTooShort() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email": "alice@example.com", "password": "short"}
                    """))
            .andExpect(status().isBadRequest());

        verify(userService, never()).register(anyString(), anyString());
    }

    @Test
    void register_returns409_whenEmailAlreadyExists() throws Exception {
        when(userService.register(anyString(), anyString()))
            .thenThrow(new EmailAlreadyExistsException(null));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
            .andExpect(status().isConflict());
    }

    @Test
    void register_returns400_whenPasswordIsTooLongInBytes() throws Exception {
        when(userService.register(anyString(), anyString()))
            .thenThrow(new InvalidPasswordException("Password is too long"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
            .andExpect(status().isBadRequest());
    }

    @Test
    void register_acceptsEmailWithSurroundingSpaces() throws Exception {
        User user = new User(UUID.randomUUID(), "alice@example.com", Role.USER, true, Instant.now());
        when(userService.register("alice@example.com", "correct-horse-battery")).thenReturn(user);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email": "  alice@example.com ", "password": "correct-horse-battery"}
                    """))
            .andExpect(status().isCreated());
    }
}