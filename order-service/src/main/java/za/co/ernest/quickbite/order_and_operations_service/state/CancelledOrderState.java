package za.co.ernest.quickbite.order_and_operations_service.state;

public class CancelledOrderState extends AbstractOrderState{
    @Override
    public String getStatusName() {
        return "CANCELLED";
    }
}
