package za.co.ernest.quickbite.restaurant.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ExceptionHandler(ResourceConflictException.class)
public ProblemDetail handleResourceConflict(ResourceConflictException ex) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problemDetail.setTitle("Resource Configuration Conflict");
    problemDetail.setProperty("timestamp", java.time.Instant.now());
    return problemDetail;
}
