package com.giftshop.backend.giftcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GiftCardServiceTest {

    @Mock
    private GiftCardRepository repository;

    @InjectMocks
    private GiftCardService service;

    private final GiftCard cinema =
        new GiftCard(1L, "Carte Cinéma", "Loisirs", new BigDecimal("25.00"));

    @Test
    void getAll_returnsAllGiftCards() {
        // Given
        when(repository.findAll()).thenReturn(List.of(cinema));

        // When
        List<GiftCard> result = service.getAll();

        // Then
        assertThat(result).containsExactly(cinema);
    }

    @Test
    void getById_returnsGiftCard_whenItExists() {
        when(repository.findById(1L)).thenReturn(Optional.of(cinema));

        GiftCard result = service.getById(1L);

        assertThat(result).isEqualTo(cinema);
    }

    @Test
    void getById_throwsException_whenGiftCardDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
            .isInstanceOf(GiftCardNotFoundException.class)
            .hasMessageContaining("99");
    }
}
