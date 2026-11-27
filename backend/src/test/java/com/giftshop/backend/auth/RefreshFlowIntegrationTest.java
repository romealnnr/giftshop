package com.giftshop.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.giftshop.backend.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RefreshFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private MvcResult logIn(String credentials) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials))
            .andExpect(status().isOk())
            .andReturn();
    }

    private static Cookie refreshCookieOf(MvcResult result) {
        return result.getResponse().getCookie(AuthController.REFRESH_COOKIE);
    }

    @Test
    void session_canBeRefreshed_rotated_protectedAgainstTheft_andClosed() throws Exception {
        String credentials = """
            {"email": "refresh-%s@example.com", "password": "correct-horse-battery"}
            """.formatted(UUID.randomUUID());
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials))
            .andExpect(status().isCreated());

        // Login: the refresh token arrives in an HttpOnly cookie
        Cookie first = refreshCookieOf(logIn(credentials));
        assertThat(first).isNotNull();
        assertThat(first.isHttpOnly()).isTrue();

        // Refresh: a new access token that works, and a rotated cookie
        MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh").cookie(first))
            .andExpect(status().isOk())
            .andReturn();
        Cookie second = refreshCookieOf(refreshed);
        assertThat(second.getValue()).isNotEqualTo(first.getValue());

        String accessToken = JsonPath.read(refreshed.getResponse().getContentAsString(), "$.accessToken");
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk());

        // Someone reuses the first cookie: theft is detected, the whole session is revoked
        mockMvc.perform(post("/api/auth/refresh").cookie(first))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh").cookie(second))
            .andExpect(status().isUnauthorized());

        // A new login opens a new session, and logout closes it
        Cookie third = refreshCookieOf(logIn(credentials));
        mockMvc.perform(post("/api/auth/logout").cookie(third))
            .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/refresh").cookie(third))
            .andExpect(status().isUnauthorized());
    }
}
