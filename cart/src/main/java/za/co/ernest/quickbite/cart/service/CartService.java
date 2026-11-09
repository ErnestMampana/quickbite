package za.co.ernest.quickbite.cart.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import za.co.ernest.quickbite.cart.client.OrderClient;
import za.co.ernest.quickbite.cart.client.OrderRequest;
import za.co.ernest.quickbite.cart.client.OrderResponse;
import za.co.ernest.quickbite.cart.dto.AddToCartRequest;
import za.co.ernest.quickbite.cart.dto.CartItemResponse;
import za.co.ernest.quickbite.cart.dto.CartResponse;
import za.co.ernest.quickbite.cart.dto.OrderItemRequest;
import za.co.ernest.quickbite.cart.model.CartItem;
import za.co.ernest.quickbite.cart.model.ShoppingCart;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartService {

    private final Map<Long, ShoppingCart> cartStorageCache = new ConcurrentHashMap<>();
    private final OrderClient orderClient;

    public CartResponse getCart(Long customerId) {
        ShoppingCart cart = cartStorageCache.computeIfAbsent(customerId, ShoppingCart::new);
        return mapToCartResponse(cart);
    }

    public CartResponse addProductToCart(Long customerId, AddToCartRequest request) {
        ShoppingCart cart = cartStorageCache.computeIfAbsent(customerId, ShoppingCart::new);

        CartItem item = new CartItem(
                request.productId(),
                request.productName(),
                request.unitPrice(),
                request.quantity()
        );

        cart.addItem(request.restaurantId(), item);
        log.info("Customer [{}] added item [{}] to cart runtime context safely", customerId, request.productName());

        return mapToCartResponse(cart);
    }

    public void clearCartCache(Long customerId) {
        cartStorageCache.remove(customerId);
        log.info("Evicted volatile cart storage allocations for user identity [{}]", customerId);
    }

    public OrderResponse checkout(Long customerId) {
        log.info("Received checkout request for client identity context: [{}]", customerId);
        // 3. Fetch the current cart or throw an exception if empty
        ShoppingCart cart = cartStorageCache.get(customerId);
        if (cart == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot checkout an empty or non-existent cart for customer: " + customerId);
        }

        log.info("Initiating checkout sequence for customer [{}]", customerId);


        OrderRequest orderRequest = new OrderRequest(
                cart.getRestaurantId(),
                cart.calculateTotalCartValuation(),
                cart.getItems().values().stream()
                        .map(item -> new OrderItemRequest(
                                item.getProductId(),
                                item.getQuantity(),
                                item.getUnitPrice()
                        ))
                        .toList()
        );

        try {
            // 5. Fire the Feign client network call using the customerId as the path variable
            OrderResponse response = orderClient.placeOrder(customerId, orderRequest);
            log.info("Order successfully created.");

            // 6. Evict the user's cart from cache since the transaction is finalized
            clearCartCache(customerId);

            return response;
        } catch (Exception e) {
            log.error("Failed to process checkout transaction for customer [{}] due to remote service error", customerId, e);
            throw new RuntimeException("Checkout failed. Please try again later.", e);
        }
    }

    private CartResponse mapToCartResponse(ShoppingCart cart) {
        var itemsList = cart.getItems().values().stream()
                .map(item -> new CartItemResponse(item.getProductId(), item.getProductName(), item.getUnitPrice(), item.getQuantity(), item.calculateSubTotal()))
                .toList();

        return new CartResponse(
                cart.getCustomerId(),
                cart.getRestaurantId(),
                itemsList,
                cart.calculateTotalCartValuation()
        );
    }
}
