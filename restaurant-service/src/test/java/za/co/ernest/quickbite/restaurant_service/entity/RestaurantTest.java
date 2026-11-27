package za.co.ernest.quickbite.restaurant_service.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import za.co.ernest.quickbite.restaurant_service.enums.ProductCategory;
import za.co.ernest.quickbite.restaurant_service.enums.RestaurantStatus;
import za.co.ernest.quickbite.restaurant_service.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RestaurantTest {

    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        restaurant = Restaurant.builder()
                .name("QuickBite Grill")
                .address("12 Main Road, Krugersdorp")
                .phoneNumber("0115551234")
                .ownerId(100L)
                .build();
    }

    @Test
    @DisplayName("Builder - should initialize restaurant closed by default with ACTIVE status")
    void builder_ValidInputs_InitializesDefaults() {
        assertEquals("QuickBite Grill", restaurant.getName());
        assertEquals("12 Main Road, Krugersdorp", restaurant.getAddress());
        assertEquals("0115551234", restaurant.getPhoneNumber());
        assertEquals(100L, restaurant.getOwnerId());
        assertFalse(restaurant.isOpen());
        assertEquals(RestaurantStatus.ACTIVE, restaurant.getStatus());
        assertTrue(restaurant.getMenu().isEmpty());
    }

    @Test
    @DisplayName("toggleOperatingStatus - should switch isOpen between false and true")
    void toggleOperatingStatus_SwitchesOpenState() {
        assertFalse(restaurant.isOpen());

        restaurant.toggleOperatingStatus();
        assertTrue(restaurant.isOpen());

        restaurant.toggleOperatingStatus();
        assertFalse(restaurant.isOpen());
    }

    @Test
    @DisplayName("isOwnedBy - should return true only for matching ownerId")
    void isOwnedBy_ChecksOwnerIdAccurately() {
        assertTrue(restaurant.isOwnedBy(100L));
        assertFalse(restaurant.isOwnedBy(999L));
        assertFalse(restaurant.isOwnedBy(null));
    }

    @Test
    @DisplayName("getMenu - should return unmodifiable list to protect aggregate encapsulation")
    void getMenu_ReturnsUnmodifiableList() {
        MenuItem item = createMenuItem(1L, "Burger", "95.00");
        List<MenuItem> menuView = restaurant.getMenu();

        assertThrows(UnsupportedOperationException.class, () -> menuView.add(item));
    }

    @Test
    @DisplayName("addMenuItem - should add item to menu and set bidirectional restaurant reference")
    void addMenuItem_ActiveRestaurant_AddsAndLinksItem() {
        MenuItem item = createMenuItem(1L, "Classic Burger", "95.00");

        restaurant.addMenuItem(item);

        assertEquals(1, restaurant.getMenu().size());
        assertSame(item, restaurant.getMenu().get(0));
        assertSame(restaurant, item.getRestaurant());
    }

    @Test
    @DisplayName("Suspended restaurant - should block adding, updating, and removing menu items")
    void suspendedRestaurant_BlocksAllMenuModifications() {
        MenuItem item = createMenuItem(1L, "Classic Burger", "95.00");
        restaurant.addMenuItem(item);

        // Simulate SUSPENDED state
        ReflectionTestUtils.setField(restaurant, "status", RestaurantStatus.SUSPENDED);

        MenuItem anotherItem = createMenuItem(2L, "Fries", "35.00");

        assertThrows(IllegalStateException.class, () -> restaurant.addMenuItem(anotherItem));
        assertThrows(IllegalStateException.class, () -> restaurant.updateMenuItemPrice(1L, new BigDecimal("110.00")));
        assertThrows(IllegalStateException.class, () -> restaurant.updateMenuItemAvailability(1L, false));
        assertThrows(IllegalStateException.class, () -> restaurant.removeMenuItem(1L));
    }

    @Test
    @DisplayName("updateMenuItemPrice & updateMenuItemAvailability - should mutate matching item in menu")
    void updateMenuItemMutations_ValidItem_UpdatesItemState() {
        MenuItem item = createMenuItem(10L, "Ribs", "180.00");
        restaurant.addMenuItem(item);

        restaurant.updateMenuItemPrice(10L, new BigDecimal("195.00"));
        restaurant.updateMenuItemAvailability(10L, false);

        assertEquals(new BigDecimal("195.00"), item.getPrice());
        assertFalse(item.isAvailable());
    }

    @Test
    @DisplayName("removeMenuItem - should remove item when ID exists, or throw ResourceNotFoundException when missing")
    void removeMenuItem_ExistingAndMissingId() {
        MenuItem item = createMenuItem(5L, "Wings", "85.00");
        restaurant.addMenuItem(item);

        assertThrows(ResourceNotFoundException.class, () -> restaurant.removeMenuItem(999L));

        restaurant.removeMenuItem(5L);
        assertTrue(restaurant.getMenu().isEmpty());
    }

    @Test
    @DisplayName("findNestedItemOrThrow - should find item by ID or throw ResourceNotFoundException")
    void findNestedItemOrThrow_ById() {
        MenuItem item = createMenuItem(7L, "Steak", "210.00");
        restaurant.addMenuItem(item);

        MenuItem found = restaurant.findNestedItemOrThrow(7L);
        assertSame(item, found);

        assertThrows(ResourceNotFoundException.class, () -> restaurant.findNestedItemOrThrow(404L));
    }

    @Test
    @DisplayName("findNestedItemByNameOrThrow - should match case-insensitively with whitespace trimming")
    void findNestedItemByNameOrThrow_CaseInsensitiveAndValidation() {
        MenuItem item = createMenuItem(8L, "Chicken Wrap", "75.00");
        restaurant.addMenuItem(item);

        MenuItem found = restaurant.findNestedItemByNameOrThrow("  chicken WRAP ");
        assertSame(item, found);

        assertThrows(IllegalArgumentException.class, () -> restaurant.findNestedItemByNameOrThrow(null));
        assertThrows(IllegalArgumentException.class, () -> restaurant.findNestedItemByNameOrThrow("   "));
        assertThrows(ResourceNotFoundException.class, () -> restaurant.findNestedItemByNameOrThrow("Pizza"));
    }

    private MenuItem createMenuItem(Long id, String name, String price) {
        MenuItem item = MenuItem.builder()
                .name(name)
                .description("Delicious " + name)
                .price(new BigDecimal(price))
                .category(ProductCategory.KOTA)
                .imageUrls(List.of("http://localhost:8080/uploads/menu-items/sample.jpg"))
                .build();
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }
}

