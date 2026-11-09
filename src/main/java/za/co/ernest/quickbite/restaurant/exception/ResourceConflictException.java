package za.co.ernest.quickbite.restaurant.exception;

public class ResourceConflictException extends RuntimeException {
  public ResourceConflictException(String message) {
    super(message);
  }
}
