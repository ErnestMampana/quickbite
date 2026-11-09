package za.co.ernest.quickbite.restaurant_service.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import za.co.ernest.quickbite.restaurant_service.enums.ProductCategory;

import java.math.BigDecimal;

@Entity
@Table(name = "menu_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean isAvailable;

    @Enumerated(EnumType.STRING) // Persisted cleanly as text
    @Column(nullable = false, name = "category")
    private ProductCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Builder
    public MenuItem(String name, String description, BigDecimal price, ProductCategory category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.isAvailable = true;
    }

    public void assignToRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public void updatePrice(BigDecimal newPrice) {
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid domain mutation: Menu item price must be positive.");
        }
        this.price = newPrice;
    }

    public void updateAvailability(boolean status) {
        this.isAvailable = status;
    }

    public void updateDetails(String name, String description, BigDecimal price, boolean isAvailable, ProductCategory category) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid Mutation: Product price must be greater than zero.");
        }

        this.name = name;
        this.description = description;
        this.price = price;
        this.isAvailable = isAvailable;
        this.category = category;
    }

}
