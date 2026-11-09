package za.co.ernest.quickbite.resturantService.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "restaurants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Enforces JPA specification compliance while blocking manual invalid instantiation
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(nullable = false, name = "is_open")
    private boolean isOpen;

    @Column(nullable = false, name = "owner_id")
    private Long ownerId; // Uniform connection to identity contexts in your UserService

    // Defensive mapping with orphan removal ensures strict parent lifecycle cascading
    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<MenuItem> menu = new ArrayList<>();

    @Builder
    public Restaurant(String name, String address, String phoneNumber, Long ownerId) {
        this.name = name;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.ownerId = ownerId;
        this.isOpen = false; // System defaults state profile to closed on creation
    }

    /**
     * Business Domain Method: Handles operational status transitions.
     * Prevents raw structural field leaks.
     */
    public void toggleOperatingStatus() {
        this.isOpen = !this.isOpen;
    }

    /**
     * Domain Integrity Method: Ensures bidirectional association consistency.
     * Establishes transactional tracking through the parent aggregate context root.
     */
    public void addMenuItem(MenuItem item) {
        this.menu.add(item);
        item.assignToRestaurant(this);
    }

    /**
     * Tenant Identification Domain Logic.
     * Protects business resources from unauthenticated ownership mutations.
     */
    public boolean isOwnedBy(Long userId) {
        return this.ownerId != null && this.ownerId.equals(userId);
    }

    /**
     * Defensive Collection Encapsulation.
     * Prevents external runtime contexts from modifying the list directly without tracking.
     */
    public List<MenuItem> getMenu() {
        return Collections.unmodifiableList(this.menu);
    }
}
