package za.co.ernest.quickbite.order_and_operations_service.state;

import za.co.ernest.quickbite.order_and_operations_service.entity.Order;

public class DispatchedOrderState extends AbstractOrderState{

    @Override
    public void transitionToDelivered(Order order) {
        order.setLifecycleState(new DeliveredOrderState());
    }

    @Override
    public String getStatusName() {
        return "DISPATCHED";
    }
}
