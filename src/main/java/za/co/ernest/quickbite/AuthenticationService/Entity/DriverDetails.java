package za.co.ernest.quickbite.AuthenticationService.Entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "driver_details")
@Getter
@Setter
@NoArgsConstructor
public class DriverDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @OneToOne(cascade = CascadeType.ALL)
    private User userProfile; // Links directly to the Core User
    private String licensePlate;
    private String vehicleType;

    public DriverDetails(User user, String licensePlate, String vehicleType) {
        this.userProfile = user;
        this.licensePlate = licensePlate;
        this.vehicleType = vehicleType;
    }

}
