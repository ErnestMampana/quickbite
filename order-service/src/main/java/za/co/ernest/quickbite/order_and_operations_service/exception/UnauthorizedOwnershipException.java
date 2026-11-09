package za.co.ernest.quickbite.order_and_operations_service.exception;

public class UnauthorizedOwnershipException extends RuntimeException {
    public UnauthorizedOwnershipException(String message) {
        super(message);
    }
}
