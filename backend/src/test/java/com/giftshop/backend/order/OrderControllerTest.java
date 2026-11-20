package com.giftshop.backend.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.giftshop.backend.giftcard.GiftCardNotFoundException;
import com.giftshop.backend.security.SecurityConfig;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private final UUID buyerId = UUID.fromString("9468152f-196d-469b-ad32-649423170f3e");

    private final Order order = new Order(
        UUID.fromString("3f2a9c1e-7b4d-4e8a-9f12-6c5b8d0e4a71"),
        buyerId,
        2L,
        "Carte Librairie",
        new BigDecimal("30.00"),
        "GIFT-AAAA-BBBB-CCCC",
        Instant.parse("2026-11-16T14:00:00Z")
    );

    /** A request authenticated as the buyer, with a token whose subject is the buyer id. */
    private RequestPostProcessor buyer() {
        return jwt().jwt(token -> token.subject(buyerId.toString()));
    }

    // ---------- create ----------

    @Test
    void create_returns201WithLocationAndOrder() throws Exception {
        when(orderService.create(2L, buyerId)).thenReturn(order);

        mockMvc.perform(post("/api/orders")
                .with(buyer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"giftCardId": 2}
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/orders/" + order.id()))
            .andExpect(jsonPath("$.code").value("GIFT-AAAA-BBBB-CCCC"))
            .andExpect(jsonPath("$.userId").value(buyerId.toString()));
    }

    @Test
    void create_returns401_withoutToken() throws Exception {
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"giftCardId": 2}
                    """))
            .andExpect(status().isUnauthorized());

        verify(orderService, never()).create(any(), any());
    }

    @Test
    void create_returns400_whenGiftCardIdIsMissing() throws Exception {
        mockMvc.perform(post("/api/orders")
                .with(buyer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        verify(orderService, never()).create(any(), any());
    }

    @Test
    void create_returns400_whenGiftCardIdIsNotPositive() throws Exception {
        mockMvc.perform(post("/api/orders")
                .with(buyer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"giftCardId": -1}
                    """))
            .andExpect(status().isBadRequest());

        verify(orderService, never()).create(any(), any());
    }

    @Test
    void create_returns404_whenGiftCardDoesNotExist() throws Exception {
        when(orderService.create(99L, buyerId)).thenThrow(new GiftCardNotFoundException(99L));

        mockMvc.perform(post("/api/orders")
                .with(buyer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"giftCardId": 99}
                    """))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // ---------- read ----------

    @Test
    void getById_returns404_whenOrderIsNotFoundOrNotTheBuyers() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(orderService.getById(unknownId, buyerId)).thenThrow(new OrderNotFoundException(unknownId));

        mockMvc.perform(get("/api/orders/" + unknownId).with(buyer()))
            .andExpect(status().isNotFound());
    }

    @Test
    void getMine_returnsTheBuyersOrders() throws Exception {
        when(orderService.getAllForUser(buyerId)).thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders").with(buyer()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].code").value("GIFT-AAAA-BBBB-CCCC"));
    }
}
