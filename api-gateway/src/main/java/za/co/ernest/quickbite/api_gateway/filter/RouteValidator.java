package za.co.ernest.quickbite.api_gateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    public static final List<String> OPEN_API_ENDPOINTS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/confirm-email",
            "/api/v1/auth/password-reset/request",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/refresh-otp",
            "/v3/api-docs",
            "/swagger-ui",
            "/webjars"
    );

    public Predicate<ServerHttpRequest> isSecured = request ->
            OPEN_API_ENDPOINTS.stream()
                    .noneMatch(uri -> request.getURI().getPath().startsWith(uri) ||
                            request.getURI().getPath().contains("/v3/api-docs"));
}
