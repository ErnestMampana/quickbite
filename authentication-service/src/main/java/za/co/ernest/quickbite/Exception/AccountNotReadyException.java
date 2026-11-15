package za.co.ernest.quickbite.Exception;

public class AccountNotReadyException extends RuntimeException {
    public AccountNotReadyException(String message) {
        super(message);
    }
}
