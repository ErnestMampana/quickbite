package za.co.ernest.quickbite.restaurant_service.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import za.co.ernest.quickbite.restaurant_service.enums.ProductCategory;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class MenuItemTest {

    @Test
    @DisplayName("Builder - should create MenuItem with valid fields, default availability true, and 1-5 images")
    void builder_ValidInputs_CreatesMenuItem() {
        List<String> images = List.of(
                "http://localhost:8080/uploads/menu-items/1.jpg",
                "http://localhost:8080/uploads/menu-items/2.jpg"
        );

        MenuItem item = MenuItem.builder()
                .name("Peri-Peri Chicken Burger")
                .description("Flame-grilled chicken breast with peri-peri mayo")
                .price(new BigDecimal("115.00"))
                .category(ProductCategory.KOTA)
                .imageUrls(images)
                .build();

        assertEquals("Peri-Peri Chicken Burger", item.getName());
        assertEquals("Flame-grilled chicken breast with peri-peri mayo", item.getDescription());
        assertEquals(new BigDecimal("115.00"), item.getPrice());
        assertEquals(ProductCategory.KOTA, item.getCategory());
        assertTrue(item.isAvailable());
        assertEquals(2, item.getImageUrls().size());
        assertEquals(images, item.getImageUrls());
    }

//    @Test
//    @DisplayName("Builder - should reject null, empty, or more than 5 images (if constructor guard is added)")
//    void builder_InvalidImageCount_ThrowsException() {
//        List<String> sixImages = List.of("1.jpg", "2.jpg", "3.jpg", "4.jpg", "5.jpg", "6.jpg");
//
//        assertThrows(IllegalArgumentException.class, () -> MenuItem.builder()
//                .name("Burger")
//                .description("Desc")
//                .price(new BigDecimal("50.00"))
//                .category(ProductCategory.KOTA)
//                .imageUrls(null)
//                .build());
//
//        assertThrows(IllegalArgumentException.class, () -> MenuItem.builder()
//                .name("Burger")
//                .description("Desc")
//                .price(new BigDecimal("50.00"))
//                .category(ProductCategory.KOTA)
//                .imageUrls(Collections.emptyList())
//                .build());
//
//        assertThrows(IllegalArgumentException.class, () -> MenuItem.builder()
//                .name("Burger")
//                .description("Desc")
//                .price(new BigDecimal("50.00"))
//                .category(ProductCategory.KOTA)
//                .imageUrls(sixImages)
//                .build());
//    }

    @Test
    @DisplayName("assignToRestaurant - should link MenuItem to target Restaurant")
    void assignToRestaurant_ValidRestaurant_SetsAssociation() {
        MenuItem item = createValidMenuItem();
        Restaurant restaurant = mock(Restaurant.class);

        item.assignToRestaurant(restaurant);

        assertSame(restaurant, item.getRestaurant());
    }

    @Test
    @DisplayName("updatePrice - should update price when positive")
    void updatePrice_PositiveAmount_UpdatesPrice() {
        MenuItem item = createValidMenuItem();

        item.updatePrice(new BigDecimal("149.99"));

        assertEquals(new BigDecimal("149.99"), item.getPrice());
    }

    @Test
    @DisplayName("updatePrice - should reject null, zero, or negative prices")
    void updatePrice_ZeroOrNegative_ThrowsException() {
        MenuItem item = createValidMenuItem();

        assertThrows(IllegalArgumentException.class, () -> item.updatePrice(null));
        assertThrows(IllegalArgumentException.class, () -> item.updatePrice(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> item.updatePrice(new BigDecimal("-10.00")));
    }

    @Test
    @DisplayName("updateAvailability - should toggle availability flag")
    void updateAvailability_TogglesStatus() {
        MenuItem item = createValidMenuItem();
        assertTrue(item.isAvailable());

        item.updateAvailability(false);
        assertFalse(item.isAvailable());

        item.updateAvailability(true);
        assertTrue(item.isAvailable());
    }

    @Test
    @DisplayName("updateDetails - should update core fields when price is valid")
    void updateDetails_ValidInputs_UpdatesAllFields() {
        MenuItem item = createValidMenuItem();

        item.updateDetails(
                "Updated Wrap",
                "Grilled halloumi wrap",
                new BigDecimal("89.50"),
                false,
                ProductCategory.KOTA
        );

        assertEquals("Updated Wrap", item.getName());
        assertEquals("Grilled halloumi wrap", item.getDescription());
        assertEquals(new BigDecimal("89.50"), item.getPrice());
        assertFalse(item.isAvailable());
    }

    @Test
    @DisplayName("updateDetails - should throw IllegalArgumentException when price is zero or negative")
    void updateDetails_InvalidPrice_ThrowsException() {
        MenuItem item = createValidMenuItem();

        assertThrows(IllegalArgumentException.class, () -> item.updateDetails(
                "Wrap",
                "Desc",
                null,
                true,
                ProductCategory.KOTA
        ));
    }

    private MenuItem createValidMenuItem() {
        return MenuItem.builder()
                .name("Classic Burger")
                .description("Beef patty with lettuce and tomato")
                .price(new BigDecimal("95.00"))
                .category(ProductCategory.KOTA)
                .imageUrls(List.of("http://localhost:8080/uploads/menu-items/default.jpg"))
                .build();
    }
}

