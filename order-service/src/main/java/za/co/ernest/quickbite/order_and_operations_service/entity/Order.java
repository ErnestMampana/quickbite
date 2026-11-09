package za.co.ernest.quickbite.order_and_operations_service.entity;

import jakarta.persistence.*;
import lombok.*;
import za.co.ernest.quickbite.order_and_operations_service.state.CreatedOrderState;
import za.co.ernest.quickbite.order_and_operations_service.state.OrderState;
import za.co.ernest.quickbite.order_and_operations_service.state.OrderStateConverter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "customer_id")
    private Long customerId;

    @Column(nullable = false, name = "restaurant_id")
    private Long restaurantId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Setter
    @Convert(converter = OrderStateConverter.class)
    @Column(nullable = false, name = "status", length = 30)
    private OrderState lifecycleState;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Builder
    public Order(Long customerId, Long restaurantId, BigDecimal totalAmount) {
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.totalAmount = totalAmount;
        this.lifecycleState = new CreatedOrderState();
    }

    public void setItems(List<OrderItem> orderItems) {
        this.items.clear();
        if (orderItems != null) {
            orderItems.forEach(item -> {
                this.items.add(item);
                item.setOrder(this);
            });
        }
    }

    public void processPaymentSuccess() {
        this.lifecycleState.transitionToPaid(this);
    }

    public void acceptToKitchen() {
        this.lifecycleState.transitionToPreparing(this);
    }

    public void dispatchForDelivery() {
        this.lifecycleState.transitionToDispatched(this);
    }

    public void completeOrderDelivery() {
        this.lifecycleState.transitionToDelivered(this);
    }

    public void abortOrder() {
        this.lifecycleState.cancelOrder(this);
    }
}
