package com.giftshop.backend.order;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> create(@Valid @RequestBody CreateOrderRequest request,
                                        @AuthenticationPrincipal Jwt jwt) {
        Order order = orderService.create(request.giftCardId(), currentUserId(jwt));
        URI location = URI.create("/api/orders/" + order.id());
        return ResponseEntity.created(location).body(order);
    }

    @GetMapping
    public List<Order> getMine(@AuthenticationPrincipal Jwt jwt) {
        return orderService.getAllForUser(currentUserId(jwt));
    }

    @GetMapping("/{id}")
    public Order getById(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return orderService.getById(id, currentUserId(jwt));
    }

    private static UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
