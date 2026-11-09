package za.co.ernest.quickbite.restaurant_service.entity;

import jakarta.persistence.*;
import lombok.*;
import za.co.ernest.quickbite.restaurant_service.enums.RestaurantStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "restaurants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    private RestaurantStatus status = RestaurantStatus.ACTIVE;


    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<MenuItem> menu = new ArrayList<>();

    @Builder
    public Restaurant(String name, String address, String phoneNumber, Long ownerId) {
        this.name = name;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.ownerId = ownerId;
        this.isOpen = false;
    }

    public void toggleOperatingStatus() {
        this.isOpen = !this.isOpen;
    }


    public void addMenuItem(MenuItem item) {
        ensureOperationalState();
        this.menu.add(item);
        item.assignToRestaurant(this);
    }


    public boolean isOwnedBy(Long userId) {
        return this.ownerId != null && this.ownerId.equals(userId);
    }


    public List<MenuItem> getMenu() {
        return Collections.unmodifiableList(this.menu);
    }

    public void updateMenuItemPrice(Long itemId, BigDecimal newPrice) {
        ensureOperationalState();
        MenuItem item = findItemOrThrow(itemId);
        item.updatePrice(newPrice);
    }

    public void updateMenuItemAvailability(Long itemId, boolean status) {
        ensureOperationalState();
        MenuItem item = findItemOrThrow(itemId);
        item.updateAvailability(status);
    }

    public void removeMenuItem(Long itemId) {
        ensureOperationalState();
        MenuItem item = findItemOrThrow(itemId);
        this.menu.remove(item);
    }

    private void ensureOperationalState() {
        if (this.status == RestaurantStatus.SUSPENDED) {
            throw new IllegalStateException("Operation denied: Modification blocked on a suspended restaurant.");
        }
    }

    private MenuItem findItemOrThrow(Long itemId) {
        return this.menu.stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new za.co.ernest.quickbite.restaurant_service.exception.ResourceNotFoundException(
                        "Product catalog update rejected: Item ID " + itemId + " does not belong to this restaurant."));
    }

    public MenuItem findNestedItemOrThrow(Long itemId) {
        return this.menu.stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new za.co.ernest.quickbite.restaurant_service.exception.ResourceNotFoundException(
                        "Product Catalog Mismatch: Item ID " + itemId + " does not belong to Restaurant ID " + this.id));
    }

    public MenuItem findNestedItemByNameOrThrow(String productName) {
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("Product lookup failed: A valid product name must be provided.");
        }

        String searchName = productName.trim().toLowerCase();

        return this.menu.stream()
                .filter(item -> item.getName().trim().toLowerCase().equals(searchName))
                .findFirst()
                .orElseThrow(() -> new za.co.ernest.quickbite.restaurant_service.exception.ResourceNotFoundException(
                        "Product Mismatch: No item named '" + productName + "' could be found under this restaurant profile."));
    }

}
