package com.scaler.services;

import com.scaler.exceptions.CustomerSessionNotFound;
import com.scaler.models.Bill;


public interface OrderService {

    public Bill generateBill(long userId) throws CustomerSessionNotFound;
}
