package com.giftshop.backend.order;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;


@Repository
public  class OrderRepository {

    // ConcurrentHashMap allows for thread-safe concurrent access when multiple HTTP requests arrive simultaneously.
    private final Map<UUID, Order> ordersById = new ConcurrentHashMap<>();
    private final Map<String, UUID> orderIdsByCode = new ConcurrentHashMap<>();

    /**
     * Saves the order only if its gift code is not already used.
     *
     * return true if saved, false if the code already exists
     */
    public  boolean saveIfCodeIsUnique(Order order){
        UUID existingOrderId = orderIdsByCode.putIfAbsent(order.code(), order.id());
        
        if (existingOrderId != null) {
            return false;
        }
        ordersById.put(order.id(), order);
        return true;
    }

    public Optional<Order> findById(UUID id){
        return Optional.ofNullable((ordersById.get(id)));
    }

}