package za.co.ernest.quickbite.AuthenticationService.DTO;

import lombok.Builder;
import za.co.ernest.quickbite.AuthenticationService.Enum.UserRole;

@Builder
public record UserDTO(String username, String password, String email, String firstName, String lastName, UserRole role,
                      String licensePlate, String vehicleType) {
}
