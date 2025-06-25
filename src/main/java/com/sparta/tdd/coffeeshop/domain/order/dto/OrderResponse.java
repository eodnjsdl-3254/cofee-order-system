package com.sparta.tdd.coffeeshop.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.sparta.tdd.coffeeshop.domain.order.Order;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private String orderId;
    private String userId;
    private Long menuId; // 메뉴 ID도 응답에 포함
    private String menuName; // 메뉴 이름도 응답에 포함
    private int quantity;
    private long totalPrice;
    private long remainingPoints; // 결제 후 남은 사용자 포인트
    private LocalDateTime orderDate;
    private Order.OrderStatus status; // 주문 상태

    // Order 엔티티로부터 응답 DTO를 생성하는 팩토리 메서드 (혹은 생성자)
    public static OrderResponse from(Order order, long remainingPoints) {
        return new OrderResponse(
                order.getOrderId(),
                order.getUserId(),
                order.getMenu().getId(),
                order.getMenu().getName(), // 메뉴 이름은 별도로 받아서 설정
                order.getQuantity(),
                order.getTotalPrice(),
                remainingPoints,
                order.getOrderDate(),
                order.getStatus()
        );
    }
}