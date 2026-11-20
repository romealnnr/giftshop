package com.giftshop.backend.auth;

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
class AuthenticationFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registeredUser_canLogIn_andReadTheirProfile() throws Exception {
        String email = "flow-" + UUID.randomUUID() + "@example.com";
        String credentials = """
            {"email": "%s", "password": "correct-horse-battery"}
            """.formatted(email);

        // 1. Register
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials))
            .andExpect(status().isCreated());

        // 2. Log in and keep the token
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(loginResponse, "$.accessToken");

        // 3. Use the token
        mockMvc.perform(get("/api/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email));
    }
}
