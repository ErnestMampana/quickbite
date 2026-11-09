package za.co.ernest.quickbite.order_and_operations_service.state;

import za.co.ernest.quickbite.order_and_operations_service.entity.Order;

public class PaidOrderState extends AbstractOrderState {

    @Override
    public void transitionToPreparing(Order order) {
        order.setLifecycleState(new PreparingOrderState());
    }

    @Override
    public String getStatusName() {
        return "PAID";
    }
}
