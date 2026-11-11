package com.giftshop.backend.order;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Order> create(@Valid @RequestBody CreateOrderRequest request) {
        Order order = orderService.create(request.giftCardId());
        
        //This URL will allow you to retrieve the created order.
        URI location = URI.create("/api/orders/" + order.id());
        
        return ResponseEntity.created(location).body(order);
    }

    @GetMapping("/{id}")
    public Order getById(@PathVariable UUID id) {
        return orderService.getById(id);
    }
}