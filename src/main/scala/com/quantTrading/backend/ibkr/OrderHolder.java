package com.quantTrading.backend.ibkr;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;


public class OrderHolder {

    private Integer permId;

    private Order order;

    private Contract contract;

    private OrderState orderState;

    public OrderHolder(int permId, Order order, Contract contract, OrderState orderState) {
        this.permId = permId;
        this.order = order;
        this.contract = contract;
        this.orderState = orderState;
    }

    public Order getOrder() {
        return this.order;
    }

    public Contract getContract() {
        return this.contract;
    }

    public OrderState getOrderState() {
        return this.orderState;
    }
}