package za.co.ernest.quickbite.restaurant_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import za.co.ernest.quickbite.restaurant_service.dto.CreateMenuItemRequest;
import za.co.ernest.quickbite.restaurant_service.dto.MenuItemResponse;
import za.co.ernest.quickbite.restaurant_service.dto.UpdateProductRequest;
import za.co.ernest.quickbite.restaurant_service.entity.MenuItem;
import za.co.ernest.quickbite.restaurant_service.entity.Restaurant;
import za.co.ernest.quickbite.restaurant_service.enums.ProductCategory;
import za.co.ernest.quickbite.restaurant_service.exception.ResourceNotFoundException;
import za.co.ernest.quickbite.restaurant_service.exception.UnauthorizedOwnershipException;
import za.co.ernest.quickbite.restaurant_service.repository.RestaurantRepository;
import za.co.ernest.quickbite.restaurant_service.storage.FileStorageService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ProductService productService;

    private Restaurant restaurant;
    private static final Long RESTAURANT_ID = 1L;
    private static final Long OWNER_ID = 10L;

    @BeforeEach
    void setUp() {
        restaurant = Restaurant.builder()
                .name("QuickBite Grill")
                .address("12 Main Road, Krugersdorp")
                .phoneNumber("0115551234")
                .ownerId(OWNER_ID)
                .build();
        ReflectionTestUtils.setField(restaurant, "id", RESTAURANT_ID);
    }

    @Test
    @DisplayName("uploadProduct - should upload images, save item to restaurant, and return response")
    void uploadProduct_ValidRequest_UploadsImagesAndSavesItem() {
        CreateMenuItemRequest request = new CreateMenuItemRequest(
                "Double Cheese Burger",
                "Flame-grilled double beef patty with aged cheddar and chips",
                new BigDecimal("125.00"),
                "KOTA"
        );

        List<MultipartFile> images = List.of(
                new MockMultipartFile("images", "1.jpg", "image/jpeg", "img1".getBytes()),
                new MockMultipartFile("images", "2.jpg", "image/jpeg", "img2".getBytes())
        );

        List<String> storedUrls = List.of(
                "http://localhost:8080/uploads/menu-items/1.jpg",
                "http://localhost:8080/uploads/menu-items/2.jpg"
        );

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(fileStorageService.uploadFiles(images, "menu-items")).thenReturn(storedUrls);
        when(restaurantRepository.save(restaurant)).thenReturn(restaurant);

        MenuItemResponse response = productService.uploadProduct(RESTAURANT_ID, OWNER_ID, request, images);

        assertNotNull(response);
        assertEquals("Double Cheese Burger", response.name());
        assertEquals(new BigDecimal("125.00"), response.price());
        assertTrue(response.isAvailable());
        assertEquals(2, response.imageUrls().size());
        assertEquals(storedUrls, response.imageUrls());

        verify(fileStorageService).uploadFiles(images, "menu-items");
        verify(restaurantRepository).save(restaurant);
        verify(fileStorageService, never()).deleteFile(any());
    }

    @Test
    @DisplayName("uploadProduct - should throw ResourceNotFoundException when restaurant does not exist")
    void uploadProduct_RestaurantNotFound_ThrowsException() {
        CreateMenuItemRequest request = new CreateMenuItemRequest(
                "Burger", "Desc", new BigDecimal("80.00"), "KOTA"
        );
        List<MultipartFile> images = List.of(
                new MockMultipartFile("images", "1.jpg", "image/jpeg", "img1".getBytes())
        );

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.uploadProduct(RESTAURANT_ID, OWNER_ID, request, images));

        verifyNoInteractions(fileStorageService);
    }

    @Test
    @DisplayName("uploadProduct - should throw UnauthorizedOwnershipException when owner ID does not match")
    void uploadProduct_WrongOwner_ThrowsUnauthorizedException() {
        CreateMenuItemRequest request = new CreateMenuItemRequest(
                "Burger", "Desc", new BigDecimal("80.00"), "KOTA"
        );
        List<MultipartFile> images = List.of(
                new MockMultipartFile("images", "1.jpg", "image/jpeg", "img1".getBytes())
        );

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        assertThrows(UnauthorizedOwnershipException.class,
                () -> productService.uploadProduct(RESTAURANT_ID, 999L, request, images));

        verifyNoInteractions(fileStorageService);
    }

    @Test
    @DisplayName("uploadProduct - should reject empty or >5 images before calling FileStorageService")
    void uploadProduct_InvalidImageCount_ThrowsIllegalArgumentException() {
        CreateMenuItemRequest request = new CreateMenuItemRequest(
                "Burger", "Desc", new BigDecimal("80.00"), "KOTA"
        );
        MultipartFile sample = new MockMultipartFile("images", "1.jpg", "image/jpeg", "img".getBytes());
        List<MultipartFile> sixImages = List.of(sample, sample, sample, sample, sample, sample);

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        assertThrows(IllegalArgumentException.class,
                () -> productService.uploadProduct(RESTAURANT_ID, OWNER_ID, request, Collections.emptyList()));
        assertThrows(IllegalArgumentException.class,
                () -> productService.uploadProduct(RESTAURANT_ID, OWNER_ID, request, sixImages));

        verifyNoInteractions(fileStorageService);
    }

    @Test
    @DisplayName("uploadProduct - should delete uploaded files if database save fails (rollback cleanup)")
    void uploadProduct_DatabaseSaveFails_DeletesUploadedFiles() {
        CreateMenuItemRequest request = new CreateMenuItemRequest(
                "Double Cheese Burger",
                "Flame-grilled double beef patty and chips",
                new BigDecimal("125.00"),
                "KOTA"
        );
        List<MultipartFile> images = List.of(
                new MockMultipartFile("images", "1.jpg", "image/jpeg", "img1".getBytes()),
                new MockMultipartFile("images", "2.jpg", "image/jpeg", "img2".getBytes())
        );
        List<String> storedUrls = List.of(
                "http://localhost:8080/uploads/menu-items/1.jpg",
                "http://localhost:8080/uploads/menu-items/2.jpg"
        );

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(fileStorageService.uploadFiles(images, "menu-items")).thenReturn(storedUrls);
        when(restaurantRepository.save(any(Restaurant.class))).thenThrow(new RuntimeException("DB connection lost"));

        assertThrows(RuntimeException.class,
                () -> productService.uploadProduct(RESTAURANT_ID, OWNER_ID, request, images));

        verify(fileStorageService).deleteFile("http://localhost:8080/uploads/menu-items/1.jpg");
        verify(fileStorageService).deleteFile("http://localhost:8080/uploads/menu-items/2.jpg");
    }

    @Test
    @DisplayName("updateProductDetails - should update existing item details and return updated response")
    void updateProductDetails_ValidRequest_UpdatesItem() {
        MenuItem existingItem = attachItemToRestaurant(5L, "Old Burger", "90.00");
        UpdateProductRequest updateRequest = new UpdateProductRequest(
                "New Gourmet Burger",
                "Updated description with truffle mayo and chips.",
                new BigDecimal("140.00"),
                false,
                "KOTA"
        );

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(restaurant)).thenReturn(restaurant);

        MenuItemResponse response = productService.updateProductDetails(RESTAURANT_ID, 5L, OWNER_ID, updateRequest);

        assertEquals(5L, response.id());
        assertEquals("New Gourmet Burger", response.name());
        assertEquals(new BigDecimal("140.00"), response.price());
        assertFalse(response.isAvailable());
        assertEquals(existingItem.getImageUrls(), response.imageUrls());
        verify(restaurantRepository).save(restaurant);
    }

    @Test
    @DisplayName("getProductDetails - should return item when restaurant and owner match")
    void getProductDetails_ValidOwnerAndItem_ReturnsResponse() {
        attachItemToRestaurant(7L, "Chicken Strips", "65.00");
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        MenuItemResponse response = productService.getProductDetails(RESTAURANT_ID, 7L, OWNER_ID);

        assertEquals(7L, response.id());
        assertEquals("Chicken Strips", response.name());

        assertEquals(new BigDecimal("65.00"), response.price());
    }
    @Test
    @DisplayName("getProductDetailsByName - should find item case-insensitively")
    void getProductDetailsByName_ValidName_ReturnsResponse() {
        attachItemToRestaurant(9L, "Peri-Peri Wings", "85.00");
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        MenuItemResponse response = productService.getProductDetailsByName(RESTAURANT_ID, "peri-peri wings", OWNER_ID);
        assertEquals(9L, response.id());
        assertEquals("Peri-Peri Wings", response.name());
    }
    private MenuItem attachItemToRestaurant(Long itemId, String name, String price) {
        MenuItem item = MenuItem.builder()
                .name(name)
                .description("Delicious " + name)
                .price(new BigDecimal(price))
                .category(ProductCategory.KOTA)
                .imageUrls(List.of("http://localhost:8080/uploads/menu-items/default.jpg"))
                .build();
        ReflectionTestUtils.setField(item, "id", itemId);
        restaurant.addMenuItem(item);
        return item;
    }
}