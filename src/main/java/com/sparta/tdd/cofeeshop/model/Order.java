package com.sparta.tdd.cofeeshop.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders") // 'order'는 SQL 예약어일 수 있으므로 'orders'로 지정
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // UUID로 ID 자동 생성
    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "menu_id", nullable = false)
    private Long menuId; // Menu 엔티티의 ID와 연결

    @Column(nullable = false)
    private int quantity; // 주문 수량

    @Column(name = "total_price", nullable = false)
    private long totalPrice; // 총 결제 금액

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate; // 주문 일시

    @Enumerated(EnumType.ORDINAL) // Enum의 순서값 (0, 1, 2...)으로 저장
    @Column(nullable = false)
    private OrderStatus status; // 주문 상태 (예: COMPLETED, CANCELLED 등)

    // 생성자
    public Order(String userId, Long menuId, int quantity, long totalPrice) {
        this.userId = userId;
        this.menuId = menuId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.orderDate = LocalDateTime.now();
        this.status = OrderStatus.COMPLETED; // 초기 상태는 완료로 가정
    }

    // 주문 상태 Enum
    public enum OrderStatus {
        PENDING,     // 보류 중
        COMPLETED,   // 완료
        CANCELLED,   // 취소됨
        REFUNDED     // 환불됨
    }
}