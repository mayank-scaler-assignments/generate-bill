package com.scaler.repositories;

import com.scaler.models.Order;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class OrderRepositoryImpl implements OrderRepository {

    private final List<Order> orders;

    public OrderRepositoryImpl() {
        this.orders = new ArrayList<>();
    }

    @Override
    public Order save(Order order) {
        orders.add(order);
        return order;
    }

    @Override
    public List<Order> findOrdersByCustomerSession(long customerSessionId) {
        return orders.stream().filter(order ->
                order.getCustomerSession().getId() == customerSessionId)
                .toList();
    }
}
