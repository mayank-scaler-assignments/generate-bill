package com.scaler.repositories;

import com.scaler.models.CustomerSession;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class CustomerSessionRepositoryImpl implements CustomerSessionRepository {

    private final List<CustomerSession> customerSessions;

    public CustomerSessionRepositoryImpl() {
        this.customerSessions = new ArrayList<>();
    }

    @Override
    public CustomerSession save(CustomerSession customerSession) {
        customerSessions.add(customerSession);
        return customerSession;
    }

    @Override
    public Optional<CustomerSession> findActiveCustomerSessionByUserId(long userId) {
        return customerSessions.stream().filter(customerSession ->
                customerSession.getUser().getId() == userId )
                .findFirst();
    }
}
