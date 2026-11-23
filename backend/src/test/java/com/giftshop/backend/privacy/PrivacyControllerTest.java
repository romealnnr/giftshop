package com.giftshop.backend.privacy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.giftshop.backend.user.UserService;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.giftshop.backend.security.SecurityConfig;
import com.giftshop.backend.user.IncorrectPasswordException;
import com.giftshop.backend.user.Role;
import com.giftshop.backend.user.User;

@WebMvcTest(PrivacyController.class)
@Import(SecurityConfig.class)
class PrivacyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PrivacyService privacyService;

    private final User alice = new User(
        UUID.randomUUID(), "alice@example.com", Role.USER, true, Instant.now());

    private RequestPostProcessor asAlice() {
        return jwt().jwt(token -> token.subject(alice.id().toString()));
    }

    // ---------- export ----------

    @Test
    void exportData_returns401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/me/data"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void exportData_returnsTheUsersDataAsDownloadableFile() throws Exception {
        when(privacyService.exportData(alice.id()))
            .thenReturn(new UserDataExport(Instant.now(), alice, List.of(), List.of()));

        mockMvc.perform(get("/api/me/data").with(asAlice()))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition",
                "attachment; filename=\"giftshop-my-data.json\""))
            .andExpect(jsonPath("$.profile.email").value("alice@example.com"))
            .andExpect(jsonPath("$.profile.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.orders").isEmpty());
    }

    // ---------- delete ----------

    @Test
    void deleteAccount_returns204_withCorrectPassword() throws Exception {
        mockMvc.perform(delete("/api/me")
                .with(asAlice())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"password": "correct-horse-battery"}
                    """))
            .andExpect(status().isNoContent());

        verify(privacyService).deleteAccount(alice.id(), "correct-horse-battery");
    }

    @Test
    void deleteAccount_returns400_withoutPassword() throws Exception {
        mockMvc.perform(delete("/api/me")
                .with(asAlice())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        verify(privacyService, never()).deleteAccount(any(), any());
    }

    @Test
    void deleteAccount_returns403_withIncorrectPassword() throws Exception {
        doThrow(new IncorrectPasswordException())
            .when(privacyService).deleteAccount(any(), any());

        mockMvc.perform(delete("/api/me")
                .with(asAlice())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"password": "wrong-password-123"}
                    """))
            .andExpect(status().isForbidden());
    }
}
