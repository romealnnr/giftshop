package com.giftshop.backend.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

import com.giftshop.backend.security.AccessToken;
import com.giftshop.backend.security.RateLimiter;
import com.giftshop.backend.security.SecurityConfig;
import com.giftshop.backend.security.TooManyRequestsException;
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

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private RateLimiter rateLimiter;

    private static final String VALID_BODY = """
        {"email": "alice@example.com", "password": "correct-horse-battery"}
        """;

    // ---------- register ----------

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

    // ---------- login ----------

    @Test
    void login_returns200WithBearerToken() throws Exception {
        when(authenticationService.login("alice@example.com", "correct-horse-battery"))
            .thenReturn(new AccessToken("signed-token", 900));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("signed-token"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void login_returns401_whenCredentialsAreInvalid() throws Exception {
        when(authenticationService.login(anyString(), anyString()))
            .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void login_returns400_whenPasswordIsMissing() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email": "alice@example.com"}
                    """))
            .andExpect(status().isBadRequest());

        verify(authenticationService, never()).login(anyString(), anyString());
    }

    @Test
    void login_returns429_andDoesNotCheckThePassword_whenTooManyAttempts() throws Exception {
        doThrow(new TooManyRequestsException(42))
            .when(rateLimiter).check(eq("login"), anyString(), anyInt(), any());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().string("Retry-After", "42"));

        verify(authenticationService, never()).login(anyString(), anyString());
    }
}
