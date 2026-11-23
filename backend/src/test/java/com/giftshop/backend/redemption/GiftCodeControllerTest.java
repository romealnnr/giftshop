package com.giftshop.backend.redemption;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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

import com.giftshop.backend.security.RateLimiter;
import com.giftshop.backend.security.SecurityConfig;
import com.giftshop.backend.security.TooManyRequestsException;

@WebMvcTest(GiftCodeController.class)
@Import(SecurityConfig.class)
class GiftCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RedemptionService redemptionService;

    @MockitoBean
    private RateLimiter rateLimiter;

    private final UUID userId = UUID.randomUUID();

    private RequestPostProcessor user() {
        return jwt().jwt(token -> token.subject(userId.toString()));
    }

    private static String redeemBody(String amount) {
        return """
            {"code": "GIFT-AAAA-BBBB-CCCC", "amount": %s}
            """.formatted(amount);
    }

    // ---------- balance ----------

    @Test
    void balance_returns401_withoutToken() throws Exception {
        mockMvc.perform(post("/api/gift-codes/balance")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code": "GIFT-AAAA-BBBB-CCCC"}
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void balance_returnsTheRemainingBalance() throws Exception {
        when(redemptionService.getBalance("GIFT-AAAA-BBBB-CCCC", userId))
            .thenReturn(new GiftCodeBalance("Carte Restaurant",
                new BigDecimal("50.00"), new BigDecimal("30.00")));

        mockMvc.perform(post("/api/gift-codes/balance")
                .with(user())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code": "GIFT-AAAA-BBBB-CCCC"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.giftCardName").value("Carte Restaurant"))
            .andExpect(jsonPath("$.balance").value(30.0))
            .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    void balance_returns429_whenTooManyAttempts() throws Exception {
        doThrow(new TooManyRequestsException(30))
            .when(rateLimiter).check(eq("gift-code"), eq(userId.toString()), anyInt(), any());

        mockMvc.perform(post("/api/gift-codes/balance")
                .with(user())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code": "GIFT-AAAA-BBBB-CCCC"}
                    """))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().string("Retry-After", "30"));

        verify(redemptionService, never()).getBalance(any(), any());
    }

    // ---------- redeem ----------

    @Test
    void redeem_returnsTheRemainingBalance() throws Exception {
        when(redemptionService.redeem("GIFT-AAAA-BBBB-CCCC", new BigDecimal("20.00"), userId))
            .thenReturn(new RedemptionResult(new BigDecimal("20.00"), new BigDecimal("30.00")));

        mockMvc.perform(post("/api/gift-codes/redeem")
                .with(user())
                .contentType(MediaType.APPLICATION_JSON)
                .content(redeemBody("20.00")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.redeemedAmount").value(20.0))
            .andExpect(jsonPath("$.remainingBalance").value(30.0));
    }

    @Test
    void redeem_returns400_whenAmountIsNegative() throws Exception {
        mockMvc.perform(post("/api/gift-codes/redeem")
                .with(user())
                .contentType(MediaType.APPLICATION_JSON)
                .content(redeemBody("-50.00")))
            .andExpect(status().isBadRequest());

        verify(redemptionService, never()).redeem(any(), any(), any());
    }

    @Test
    void redeem_returns400_whenAmountHasMoreThanTwoDecimals() throws Exception {
        mockMvc.perform(post("/api/gift-codes/redeem")
                .with(user())
                .contentType(MediaType.APPLICATION_JSON)
                .content(redeemBody("0.001")))
            .andExpect(status().isBadRequest());

        verify(redemptionService, never()).redeem(any(), any(), any());
    }

    @Test
    void redeem_returns404_whenCodeIsInvalid() throws Exception {
        when(redemptionService.redeem(any(), any(), any())).thenThrow(new InvalidGiftCodeException());

        mockMvc.perform(post("/api/gift-codes/redeem")
                .with(user())
                .contentType(MediaType.APPLICATION_JSON)
                .content(redeemBody("20.00")))
            .andExpect(status().isNotFound());
    }

    @Test
    void redeem_returns409_whenBalanceIsInsufficient() throws Exception {
        when(redemptionService.redeem(any(), any(), any())).thenThrow(new InsufficientBalanceException());

        mockMvc.perform(post("/api/gift-codes/redeem")
                .with(user())
                .contentType(MediaType.APPLICATION_JSON)
                .content(redeemBody("20.00")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("Insufficient balance"));
    }
}
