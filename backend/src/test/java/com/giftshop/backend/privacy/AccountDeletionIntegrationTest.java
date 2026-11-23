package com.giftshop.backend.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.giftshop.backend.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccountDeletionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deletingAnAccount_anonymizesOrders_andMakesTheTokenUseless() throws Exception {
        String credentials = """
            {"email": "gdpr-%s@example.com", "password": "correct-horse-battery"}
            """.formatted(UUID.randomUUID());

        // Register, log in, buy a gift card
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

        String orderUrl = mockMvc.perform(post("/api/orders")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"giftCardId": 2}
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");
        UUID orderId = UUID.fromString(orderUrl.substring(orderUrl.lastIndexOf('/') + 1));

        // A wrong password does not delete anything
        mockMvc.perform(delete("/api/me")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"password": "wrong-password-123"}
                    """))
            .andExpect(status().isForbidden());

        // The right password deletes the account
        mockMvc.perform(delete("/api/me")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"password": "correct-horse-battery"}
                    """))
            .andExpect(status().isNoContent());

        // The order is kept for accounting, but no longer linked to anyone
        Integer anonymizedOrders = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM orders WHERE id = ? AND user_id IS NULL",
            Integer.class, orderId);
        assertThat(anonymizedOrders).isEqualTo(1);

        // The token is still signed and not expired, but it can no longer buy
        mockMvc.perform(post("/api/orders")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"giftCardId": 2}
                    """))
            .andExpect(status().isUnauthorized());

        // And the user can no longer log in
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials))
            .andExpect(status().isUnauthorized());
    }
}
