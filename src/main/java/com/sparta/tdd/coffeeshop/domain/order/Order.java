package com.sparta.tdd.coffeeshop.domain.order;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;

@Entity
@Table(name = "orders") // 'order'는 SQL 예약어일 수 있으므로 'orders'로 지정
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // UUID로 ID 자동 생성
    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId; // 주문 ID

    @Column(name = "user_id", nullable = false)
    private String userId; // 사용자 ID

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
    private Order(Builder builder) {
        this.userId = builder.userId;
        this.menuId = builder.menuId;
        this.quantity = builder.quantity;
        this.totalPrice = builder.totalPrice;
        this.orderDate = builder.orderDate;
        this.status = builder.status;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String userId;
        private Long menuId;
        private int quantity;
        private long totalPrice;
        private LocalDateTime orderDate = LocalDateTime.now(); // 기본값 설정
        private OrderStatus status = OrderStatus.COMPLETED; // 기본값 설정

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }
        public Builder menuId(Long menuId) {
            this.menuId = menuId;
            return this;
        }
        public Builder quantity(int quantity) {
            this.quantity = quantity;
            return this;
        }
        public Builder totalPrice(long totalPrice) {
            this.totalPrice = totalPrice;
            return this;
        }
        public Builder orderDate(LocalDateTime orderDate) {
            this.orderDate = orderDate;
            return this;
        }
        public Builder status(OrderStatus status) {
            this.status = status;
            return this;
        }

        public Order build() {
            // 필수 필드에 대한 null 체크 로직 추가 가능
            return new Order(this);
        }
    }
    
    // 주문 상태 Enum
    public enum OrderStatus {
        PENDING,     // 보류 중
        COMPLETED,   // 완료
        CANCELLED,   // 취소됨
        REFUNDED     // 환불됨
    }
}