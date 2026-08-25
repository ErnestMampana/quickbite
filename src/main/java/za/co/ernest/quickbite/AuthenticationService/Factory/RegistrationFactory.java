package za.co.ernest.quickbite.AuthenticationService.Factory;

import org.springframework.stereotype.Component;
import za.co.ernest.quickbite.AuthenticationService.Enum.UserRole;

import java.util.Map;

@Component
public class RegistrationFactory {
    private final Map<String, RegistrationStrategy> strategies;

    public RegistrationFactory(Map<String, RegistrationStrategy> strategies) {
        this.strategies = strategies;
    }

    public RegistrationStrategy getStrategy(UserRole role) {
        String beanName = role.name().toLowerCase() + "Registration";
        RegistrationStrategy strategy = strategies.get(beanName);

        if (strategy == null) {
            throw new IllegalArgumentException("No registration process found for role: " + role);
        }
        return strategy;
    }
}
