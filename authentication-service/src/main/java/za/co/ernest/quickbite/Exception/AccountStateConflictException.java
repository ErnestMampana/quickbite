package za.co.ernest.quickbite.Exception;

public class AccountStateConflictException extends RuntimeException {
    public AccountStateConflictException(String message) {
        super(message);
    }
}
