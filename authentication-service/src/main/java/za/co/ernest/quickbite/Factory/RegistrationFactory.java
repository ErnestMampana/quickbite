package za.co.ernest.quickbite.Factory;

import org.springframework.stereotype.Component;
import za.co.ernest.quickbite.enums.UserRole;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RegistrationFactory {
    private final Map<UserRole, RegistrationStrategy> strategies;

    public RegistrationFactory(List<RegistrationStrategy> strategyList) {
        this.strategies = strategyList.stream().collect(
                Collectors.toMap(
                        RegistrationStrategy::getSupportedRole,
                        strategy -> strategy,
                        (existing, replacement) -> existing,
                        () -> new EnumMap<>(UserRole.class)
                )
        );
    }

    public RegistrationStrategy getStrategy(UserRole role) {
        RegistrationStrategy strategy = strategies.get(role);
        if (strategy == null) {
            throw new IllegalArgumentException("System error: No registration strategy configured for role: " + role);
        }
        return strategy;
    }
}


