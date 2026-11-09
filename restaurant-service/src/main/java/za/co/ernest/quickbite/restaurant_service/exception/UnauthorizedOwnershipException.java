package za.co.ernest.quickbite.restaurant_service.exception;

public class UnauthorizedOwnershipException extends RuntimeException {
    public UnauthorizedOwnershipException(String message) {
        super(message);
    }
}
