package com.quantTrading.backend.ibkr;

import com.ib.client.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class OrderManagerService {

    private int orderId = 100;

    private Map<Integer, OrderHolder> orders = new HashMap<>();

    private EClientSocket client;

    public void placeMarketOnCloseOrder(Contract contract, Types.Action action, Decimal quantity) {
        Order order = new Order();
        order.action(action);
        order.orderType("MOC");
        order.totalQuantity(quantity);
        client.placeOrder(orderId++, contract, order);
    }

    /**
     * Order created, get from IB and store
     *
     * @param contract
     * @param order
     * @param orderState
     */
    public void setOrder(Contract contract, Order order, OrderState orderState) {
        orders.put(order.permId(), new OrderHolder(order.permId(), order, contract, orderState));
    }

    /**
     * Order status changed
     *
     * @param permId
     * @param status
     * @param filled
     * @param remaining
     * @param avgFillPrice
     * @param lastFillPrice
     */
    public void changeOrderStatus(int permId, String status, Decimal filled, double remaining, double avgFillPrice, double lastFillPrice) {
        OrderHolder orderHolder = orders.get(permId);
        if(orderHolder != null) {
            orderHolder.getOrderState().status(status);
            orderHolder.getOrder().filledQuantity(filled);
        } else {
            throw new RuntimeException("Order empty for permId=" + permId);
        }
    }

    public Collection<OrderHolder> getAllOrders() {
        return orders.values();
    }

    public Collection<OrderHolder> getActiveOrders() {
        return orders.values().stream()
                .filter(orderHolder -> orderHolder.getOrderState().status().isActive())
                .collect(Collectors.toList());
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public void setClient(EClientSocket client) {
        this.client = client;
    }
}