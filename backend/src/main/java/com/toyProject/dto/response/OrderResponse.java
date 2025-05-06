package com.toyProject.dto.response;

import com.toyProject.entity.Order;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    private Long orderId;
    private String orderUid;
    private String status;
    private long totalPrice;
    private LocalDateTime createdDate;
    private List<OrderItemResponse> items;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .orderUid(order.getOrderUid())
                .status(order.getOrderStatus().name())
                .totalPrice(order.getTotalPrice())
                .createdDate(order.getCreatedDate())
                .items(order.getOrderItems().stream()
                        .map(OrderItemResponse::from)
                        .toList())
                .build();
    }
}
