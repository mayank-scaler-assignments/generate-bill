package com.scaler.repositories;

import com.scaler.models.Order;

import java.util.List;

public interface OrderRepository {

    Order save(Order order);

    List<Order> findOrdersByCustomerSession(long customerSessionId);
}
