package za.co.ernest.quickbite.order_and_operations_service.state;

import za.co.ernest.quickbite.order_and_operations_service.entity.Order;

public interface OrderState {

    void transitionToPaid(Order order);
    void transitionToPreparing(Order order);
    void transitionToDispatched(Order order);
    void transitionToDelivered(Order order);
    void cancelOrder(Order order);

    String getStatusName();
}
