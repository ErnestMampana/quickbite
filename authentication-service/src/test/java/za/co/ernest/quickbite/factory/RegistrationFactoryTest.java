package za.co.ernest.quickbite.factory;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.ernest.quickbite.Exception.UnsupportedRegistrationRoleException;
import za.co.ernest.quickbite.Factory.RegistrationFactory;
import za.co.ernest.quickbite.Factory.RegistrationStrategy;
import za.co.ernest.quickbite.enums.UserRole;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationFactoryTest {

    @Mock
    private RegistrationStrategy customerStrategy;

    @Mock
    private RegistrationStrategy driverStrategy;

    @Mock
    private RegistrationStrategy duplicateCustomerStrategy;

    private RegistrationFactory registrationFactory;

    @BeforeEach
    void setUp() {
        when(customerStrategy.getSupportedRole()).thenReturn(UserRole.CUSTOMER);
        when(driverStrategy.getSupportedRole()).thenReturn(UserRole.DRIVER);

        registrationFactory = new RegistrationFactory(List.of(customerStrategy, driverStrategy));
    }

    @Test
    void getStrategy_ShouldReturnMatchingStrategy_WhenRoleIsConfigured() {
        RegistrationStrategy resolvedCustomer = registrationFactory.getStrategy(UserRole.CUSTOMER);
        RegistrationStrategy resolvedDriver = registrationFactory.getStrategy(UserRole.DRIVER);

        assertThat(resolvedCustomer).isSameAs(customerStrategy);
        assertThat(resolvedDriver).isSameAs(driverStrategy);
    }

    @Test
    void getStrategy_ShouldThrowUnsupportedRegistrationRoleException_WhenRoleIsNotConfigured() {
        assertThatThrownBy(() -> registrationFactory.getStrategy(UserRole.ADMIN))
                .isInstanceOf(UnsupportedRegistrationRoleException.class)
                .hasMessage("System error: No registration strategy configured for role: ADMIN");
    }

    @Test
    void getStrategy_ShouldThrowUnsupportedRegistrationRoleException_WhenRoleIsNull() {
        assertThatThrownBy(() -> registrationFactory.getStrategy(null))
                .isInstanceOf(UnsupportedRegistrationRoleException.class)
                .hasMessage("System error: No registration strategy configured for role: null");
    }

    @Test
    void constructor_ShouldKeepFirstStrategy_WhenDuplicateSupportedRolesAreProvided() {
        when(duplicateCustomerStrategy.getSupportedRole()).thenReturn(UserRole.CUSTOMER);

        RegistrationFactory factoryWithDuplicates = new RegistrationFactory(
                List.of(customerStrategy, duplicateCustomerStrategy)
        );

        assertThat(factoryWithDuplicates.getStrategy(UserRole.CUSTOMER)).isSameAs(customerStrategy);
    }
}

