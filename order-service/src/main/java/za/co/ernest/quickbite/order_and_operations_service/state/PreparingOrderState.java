package za.co.ernest.quickbite.order_and_operations_service.state;

import za.co.ernest.quickbite.order_and_operations_service.entity.Order;

public class PreparingOrderState extends AbstractOrderState{
    @Override
    public void transitionToDispatched(Order order) {
        order.setLifecycleState(new DispatchedOrderState());
    }

    @Override
    public String getStatusName() {
        return "PREPARING";
    }
}
