package com.giftshop.backend.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.giftshop.backend.giftcard.GiftCardNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Import;
import com.giftshop.backend.security.SecurityConfig;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private final Order order = new Order(
        UUID.fromString("3f2a9c1e-7b4d-4e8a-9f12-6c5b8d0e4a71"),
        2L,
        "Carte Librairie",
        new BigDecimal("30.00"),
        "GIFT-AAAA-BBBB-CCCC",
        Instant.parse("2026-11-16T14:00:00Z")
    );

    @Test
    void create_returns201WithLocationAndOrder() throws Exception {
        when(orderService.create(2L)).thenReturn(order);

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"giftCardId": 2}
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/orders/" + order.id()))
            .andExpect(jsonPath("$.code").value("GIFT-AAAA-BBBB-CCCC"))
            .andExpect(jsonPath("$.giftCardName").value("Carte Librairie"));
    }

    @Test
    void create_returns400_whenGiftCardIdIsMissing() throws Exception {
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        verify(orderService, never()).create(any());
    }

    @Test
    void create_returns400_whenGiftCardIdIsNotPositive() throws Exception {
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"giftCardId": -1}
                    """))
            .andExpect(status().isBadRequest());

        verify(orderService, never()).create(any());
    }

    @Test
    void create_returns404_whenGiftCardDoesNotExist() throws Exception {
        when(orderService.create(99L)).thenThrow(new GiftCardNotFoundException(99L));

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"giftCardId": 99}
                    """))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void getById_returns404_whenOrderDoesNotExist() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(orderService.getById(unknownId)).thenThrow(new OrderNotFoundException(unknownId));

        mockMvc.perform(get("/api/orders/" + unknownId))
            .andExpect(status().isNotFound());
    }
}