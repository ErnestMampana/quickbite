package za.co.ernest.quickbite.restaurant_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceConflictException.class)
    public ProblemDetail handleResourceConflict(ResourceConflictException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Resource Configuration Conflict");
        problemDetail.setProperty("timestamp", java.time.Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(UnauthorizedOwnershipException.class)
    public ProblemDetail handleUnauthorizedOwnership(UnauthorizedOwnershipException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle(ex.getMessage());
        problemDetail.setProperty("timestamp", java.time.Instant.now());
        return problemDetail;
    }
}
