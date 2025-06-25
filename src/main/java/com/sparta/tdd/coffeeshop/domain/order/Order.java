package com.sparta.tdd.coffeeshop.domain.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated; // Enum 타입 매핑을 위해 추가
import jakarta.persistence.EnumType; // Enum 타입 매핑을 위해 추가
import jakarta.persistence.GeneratedValue; // ID 자동 생성을 위해 추가
import jakarta.persistence.GenerationType; // ID 자동 생성을 위해 추가
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor; // 모든 필드 생성자를 위해 추가
import lombok.Builder;             // Builder 패턴을 위해 추가
import lombok.Getter;
import lombok.NoArgsConstructor;   // 기본 생성자를 위해 추가
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders") // 'order'는 SQL 예약어일 수 있으므로 'orders'로 지정
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA는 기본 생성자를 필요로 합니다. (접근 제한자 protected 권장)
@AllArgsConstructor // Lombok의 @Builder와 함께 사용될 때 모든 필드를 포함하는 생성자를 자동으로 생성합니다.
@Builder // 이 어노테이션이 빌더 패턴을 자동으로 생성해 줍니다.
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // JPA가 UUID를 자동으로 생성하여 orderId에 할당합니다.
    @Column(name = "order_id", nullable = false, unique = true, updatable = false) // ID는 생성 후 변경되지 않음
    private String orderId; // 주문 ID (VARCHAR에 매핑)

    @Column(name = "user_id", nullable = false)
    private String userId; // 사용자 ID

    @Column(name = "menu_id", nullable = false)
    private Long menuId; // Menu 엔티티의 ID와 연결 (BIGINT에 매핑)

    @Column(nullable = false)
    private int quantity; // 주문 수량 (INT에 매핑)

    @Column(name = "total_price", nullable = false)
    private Long totalPrice; // 총 결제 금액 (BIGINT에 매핑)

    @Builder.Default // Lombok 빌더 사용 시 필드 기본값 설정을 위해 추가
    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate = LocalDateTime.now();; // 주문 일시 (DATETIME(6)에 매핑)

    @Builder.Default // Lombok 빌더 사용 시 필드 기본값 설정을 위해 추가
    @Enumerated(EnumType.ORDINAL) // Enum의 순서값 (0, 1, 2...)으로 DB에 저장
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.COMPLETED; // 주문 상태 (예: COMPLETED 등)

    /**
     * 주문 상태를 나타내는 Enum
     * tinyint 값과 매핑되도록 int value를 명시적으로 가집니다.
     */
    public enum OrderStatus {
        PENDING(0),     // 보류 중
        COMPLETED(1),   // 완료
        CANCELLED(2),   // 취소됨
        REFUNDED(3);    // 환불됨

        private final int value;

        OrderStatus(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        /**
         * int 값으로 OrderStatus Enum을 찾아 반환합니다.
         * @param value tinyint에 저장된 값
         * @return 해당 OrderStatus Enum
         * @throws IllegalArgumentException 유효하지 않은 값일 경우
         */
        public static OrderStatus fromValue(int value) {
            for (OrderStatus status : OrderStatus.values()) {
                if (status.value == value) {
                    return status;
                }
            }
            throw new IllegalArgumentException("유효하지 않은 OrderStatus 값: " + value);
        }
    }
}