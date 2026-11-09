package za.co.ernest.quickbite.order_and_operations_service.state;

import za.co.ernest.quickbite.order_and_operations_service.entity.Order;

public abstract class AbstractOrderState implements OrderState {

    @Override
    public void transitionToPaid(Order order) {
        throw new IllegalStateException("Invalid status transition: Cannot pay for an order in " + getStatusName() + " state.");
    }

    @Override
    public void transitionToPreparing(Order order) {
        throw new IllegalStateException("Invalid status transition: Cannot start preparing an order in " + getStatusName() + " state.");
    }

    @Override
    public void transitionToDispatched(Order order) {
        throw new IllegalStateException("Invalid status transition: Cannot dispatch an order in " + getStatusName() + " state.");
    }

    @Override
    public void transitionToDelivered(Order order) {
        throw new IllegalStateException("Invalid status transition: Cannot deliver an order in " + getStatusName() + " state.");
    }

    @Override
    public void cancelOrder(Order order) {
        throw new IllegalStateException("Invalid status transition: Cannot cancel an order in " + getStatusName() + " state.");
    }
}
