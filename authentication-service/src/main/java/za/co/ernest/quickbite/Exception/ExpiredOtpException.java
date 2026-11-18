package za.co.ernest.quickbite.Exception;

public class ExpiredOtpException extends RuntimeException {
  public ExpiredOtpException(String message) {
    super(message);
  }
}
