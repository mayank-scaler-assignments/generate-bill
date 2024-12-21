package com.scaler.services;

import com.scaler.exceptions.CustomerSessionNotFound;
import com.scaler.models.*;
import com.scaler.repositories.CustomerSessionRepository;
import com.scaler.repositories.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImple implements OrderService {

    private final OrderRepository orderRepository;
    private final CustomerSessionRepository customerSessionRepository;

    public OrderServiceImple(OrderRepository orderRepository, CustomerSessionRepository customerSessionRepository) {
        this.orderRepository = orderRepository;
        this.customerSessionRepository = customerSessionRepository;
    }

    @Override
    public Bill generateBill(long userId) throws CustomerSessionNotFound {
        // Fetch the active customer session, throw exception if not found
        CustomerSession customerSession = customerSessionRepository
                .findActiveCustomerSessionByUserId(userId)
                .orElseThrow(() -> new CustomerSessionNotFound("Customer session not found"));

        // Mark the session as ended and save
        customerSession.setCustomerSessionStatus(CustomerSessionStatus.ENDED);
        customerSessionRepository.save(customerSession);

        // Initialize a map to store the aggregated ordered items
        Map<MenuItem, Integer> orderedItems = new HashMap<>();

        // Aggregate ordered items for the customer session using streams
        orderRepository.findOrdersByCustomerSession(customerSession.getId()).forEach(order ->
                order.getOrderedItems().forEach((menuItem, quantity) ->
                        orderedItems.merge(menuItem, quantity, Integer::sum)
                )
        );

        // Calculate the total bill amount, GST, and service charge
        double billAmount = orderedItems.entrySet().stream()
                .mapToDouble(entry -> entry.getKey().getPrice() * entry.getValue())
                .sum();
        double gst = billAmount * 0.05;
        double serviceCharge = billAmount * 0.1;

        // Create and return the bill object
        Bill bill = new Bill();
        bill.setOrderedItems(orderedItems);
        bill.setTotalAmount(billAmount + gst + serviceCharge);
        bill.setGst(gst);
        bill.setServiceCharge(serviceCharge);
        return bill;
    }
}
