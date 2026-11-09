package za.co.ernest.quickbite.order_and_operations_service.state;

import za.co.ernest.quickbite.order_and_operations_service.entity.Order;

public class CreatedOrderState  extends AbstractOrderState{
    @Override
    public void transitionToPaid(Order order) {
        order.setLifecycleState(new PaidOrderState());
    }

    @Override
    public void cancelOrder(Order order) {
        order.setLifecycleState(new CancelledOrderState());
    }

    @Override
    public String getStatusName() {
        return "CREATED";
    }
}
