package com.giftshop.backend.order;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class OrderOwnershipIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /** Registers a new user, logs in and returns their access token. */
    private String newLoggedInUser() throws Exception {
        String credentials = """
            {"email": "buyer-%s@example.com", "password": "correct-horse-battery"}
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

        return "Bearer " + JsonPath.read(loginResponse, "$.accessToken");
    }

    @Test
    void usersCanOnlySeeTheirOwnOrders() throws Exception {
        String alice = newLoggedInUser();
        String bob = newLoggedInUser();

        // Alice buys a gift card
        String orderUrl = mockMvc.perform(post("/api/orders")
                .header("Authorization", alice)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"giftCardId": 2}
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");

        // Alice can read her order
        mockMvc.perform(get(orderUrl).header("Authorization", alice))
            .andExpect(status().isOk());

        // Bob knows the URL, but gets a 404: he cannot even tell that it exists
        mockMvc.perform(get(orderUrl).header("Authorization", bob))
            .andExpect(status().isNotFound());

        // Each one only sees their own orders
        mockMvc.perform(get("/api/orders").header("Authorization", alice))
            .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/orders").header("Authorization", bob))
            .andExpect(jsonPath("$").isEmpty());
    }
}
