package com.giftshop.backend.privacy;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

import com.giftshop.backend.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DeletedAccountTokenIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void tokenOfADeletedAccount_isRejectedOnEveryRoute_evenIfTheStatusWasCached() throws Exception {
        String credentials = """
            {"email": "cache-%s@example.com", "password": "correct-horse-battery"}
            """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials))
            .andExpect(status().isCreated());

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String token = "Bearer " + JsonPath.read(loginResponse, "$.accessToken");

        // The token works, and "active" is now cached in Redis
        mockMvc.perform(get("/api/orders").header("Authorization", token))
            .andExpect(status().isOk());

        // The account is deleted: the cached status must be evicted
        mockMvc.perform(delete("/api/me")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"password": "correct-horse-battery"}
                    """))
            .andExpect(status().isNoContent());

        // The same, still signed and unexpired token is now rejected everywhere
        mockMvc.perform(get("/api/orders").header("Authorization", token))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/me/data").header("Authorization", token))
            .andExpect(status().isUnauthorized());
    }
}
