package com.scaler.controllers;

import com.scaler.dtos.*;
import com.scaler.exceptions.CustomerSessionNotFound;
import com.scaler.services.OrderService;

public class OrderController {


    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    public GenerateBillResponseDto generateBill(GenerateBillRequestDto requestDto){
        GenerateBillResponseDto generateBillRequestDto = new GenerateBillResponseDto();
        try {
            generateBillRequestDto.setBill(orderService.generateBill(requestDto.getUserId()));
            generateBillRequestDto.setResponseStatus(ResponseStatus.SUCCESS);
            return generateBillRequestDto;
        } catch (CustomerSessionNotFound e) {
            generateBillRequestDto.setResponseStatus(ResponseStatus.FAILURE);
        }
        return generateBillRequestDto;
    }


}
