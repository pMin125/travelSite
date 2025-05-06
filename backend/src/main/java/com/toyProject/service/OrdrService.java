package com.toyProject.service;

import com.toyProject.dto.response.OrderResponse;
import com.toyProject.dto.OrderRequest;
import com.toyProject.entity.*;
import com.toyProject.exception.ParticipationException;
import com.toyProject.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.toyProject.exception.ErrorCode.CART_EMPTY;
import static com.toyProject.exception.ErrorCode.PRODUCT_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdrService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;

    public OrderResponse addOrder(UserEntity user, OrderRequest orderRequest) {
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ParticipationException(CART_EMPTY));

        // 2. 선택된 상품들에 대해 기존의 `PENDING` 상태 주문을 취소
        List<Long> selectedProductIds = orderRequest.getSelectedProductIds();
        for (Long productId : selectedProductIds) {
            // 해당 상품에 대한 기존 `PENDING` 주문을 취소
            Optional<Order> existingOrder = orderRepository.findTopOrderByUserAndProductAndStatus(
                    user, productRepository.findById(productId).orElseThrow(() -> new ParticipationException(PRODUCT_NOT_FOUND)),
                    Order.OrderStatus.PENDING
            );

            if (existingOrder.isPresent()) {
                Order pendingOrder = existingOrder.get();
                pendingOrder.setOrderStatus(Order.OrderStatus.CANCELED);  // 기존 주문 취소
                Payment payment = pendingOrder.getPayment();
                if (payment != null) {
                    payment.setStatus(Payment.PaymentStatus.CANCELLED);  // 결제도 취소 상태로 변경
                    paymentRepository.save(payment);
                }
                orderRepository.save(pendingOrder);
            }
        }

        Order order = Order.builder()
                .user(user)
                .orderUid(generateOrderUid())
                .orderStatus(Order.OrderStatus.PENDING)
                .build();

        Payment payment = Payment.builder()
                .price(cart.getTotalPrice())
                .build();
        order.setPayment(payment);

        if (order.getOrderItems() == null) {
            order.setOrderItems(new ArrayList<>());
        }

        for (CartItem cartItem : cart.getCartItems()) {
            if (selectedProductIds.contains(cartItem.getProduct().getId())) {
                OrderItem orderItem = OrderItem.builder()
                        .product(cartItem.getProduct())
                        .quantity(cartItem.getQuantity())
                        .order(order)
                        .build();
                order.getOrderItems().add(orderItem);
            }
        }

        orderRepository.save(order);

        // 6. 장바구니 비우기 (필요한 경우)
//        cart.getCartItems().clear();
//        cartRepository.save(cart);

        return OrderResponse.from(order);
    }


    @Transactional
    public Order createSingleOrder(UserEntity user, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ParticipationException(PRODUCT_NOT_FOUND));

        Optional<Order> existingOrder = orderRepository.findTopOrderByUserAndProductAndStatus(
                user, product, Order.OrderStatus.PENDING
        );

        if (existingOrder.isPresent()) {
            return existingOrder.get(); // 기존 주문내역 그대로
        }
        Order order = Order.builder()
                .user(user)
                .orderUid(generateOrderUid())
                .orderStatus(Order.OrderStatus.PENDING)
                .build();

        Payment payment = Payment.builder()
                .price((long) product.getPrice())
                .build();
        order.setPayment(payment);

        OrderItem orderItem = OrderItem.builder()
                .product(product)
                .quantity(1)
                .order(order)
                .build();
        order.setOrderItems(Collections.singletonList(orderItem));

        orderRepository.save(order);
        return order;
    }

    private String generateOrderUid() {
        return UUID.randomUUID().toString();
    }

    public List<Product> orderProduct(UserEntity user) {
        List<Order> orders = orderRepository.findByUser(user);
        List<Product> result = new ArrayList<>();

        for (Order order : orders) {
            if(order.getOrderStatus() == Order.OrderStatus.CANCELED){
                continue;
            }
            List<OrderItem> orderItems = order.getOrderItems();
            for (OrderItem item : orderItems) {
                result.add(item.getProduct());
            }
        }
        return result;
    }
}
