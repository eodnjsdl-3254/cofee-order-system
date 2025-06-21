package com.sparta.tdd.test.cofeeshop.service;

import com.sparta.tdd.coffeeshop.domain.menu.Menu;
import com.sparta.tdd.coffeeshop.domain.menu.repo.MenuRepository;
import com.sparta.tdd.coffeeshop.domain.order.Order;
import com.sparta.tdd.coffeeshop.domain.order.dto.OrderRequest;
import com.sparta.tdd.coffeeshop.domain.order.dto.OrderResponse;
import com.sparta.tdd.coffeeshop.domain.order.repo.OrderRepository;
import com.sparta.tdd.coffeeshop.domain.order.service.OrderService;
import com.sparta.tdd.coffeeshop.domain.user.User;
import com.sparta.tdd.coffeeshop.domain.user.repo.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given; // given-when-then 패턴을 위한 BDDMockito 임포트
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class) // Mockito를 JUnit 5와 함께 사용
class OrderServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private MenuRepository menuRepository;
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks // @Mock 객체들을 이 객체에 주입
    private OrderService orderService;

    @Spy
    private User testUser = new User("user123", 10000L);;
    
    private Menu testMenu;

    @BeforeEach
    void setUp() {
        //testUser = new User("user123", 10000L); // 초기 포인트 10000L
        testMenu = new Menu(1L, "아메리카노", 4000); // ID를 포함하여 Menu 생성
    }

    @Test
    @DisplayName("유효한 요청으로 커피 주문 및 결제에 성공한다.")
    void placeOrder_Success() {
        // Given
        OrderRequest request = new OrderRequest(testUser.getUserId(), testMenu.getId(), 2); // 2개 주문

        // Mock 객체의 동작 정의 (Stubbing)
        given(userRepository.findById(testUser.getUserId())).willReturn(Optional.of(testUser));
        given(menuRepository.findById(testMenu.getId())).willReturn(Optional.of(testMenu));
        // orderRepository.save 호출 시 어떤 Order 객체가 저장되는지 캡처하기 위해 ArgumentCaptor 사용
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        given(orderRepository.save(orderCaptor.capture())).willAnswer(invocation -> {
            Order capturedOrder = invocation.getArgument(0);
            // UUID는 Mockito가 생성할 수 없으므로, 테스트를 위해 임시 UUID 설정 (실제 구현 시 Order 엔티티에서 자동 생성됨)
            if (capturedOrder.getOrderId() == null) {
                capturedOrder.setOrderId("test-order-id-123");
            }
            return capturedOrder;
        });

        // When
        OrderResponse response = orderService.placeOrder(request);

        // Then
        // 1. 응답 검증
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(testUser.getUserId());
        assertThat(response.getMenuId()).isEqualTo(testMenu.getId());
        assertThat(response.getMenuName()).isEqualTo(testMenu.getName());
        assertThat(response.getQuantity()).isEqualTo(request.getQuantity());

        long expectedTotalPrice = (long) testMenu.getPrice() * request.getQuantity();
        //long expectedRemainingPoints = testUser.getPoint() - expectedTotalPrice;

        assertThat(response.getTotalPrice()).isEqualTo(expectedTotalPrice);
        assertThat(response.getRemainingPoints()).isEqualTo(testUser.getPoint());
        assertThat(response.getStatus()).isEqualTo(Order.OrderStatus.COMPLETED); // Order.OrderStatus.COMPLETED);
        assertThat(response.getOrderId()).isNotNull(); // OrderId가 생성되었는지 확인

        // 2. Mock 객체 상호작용 검증
        // userRepository.findById가 1번 호출되었는지 검증
        verify(userRepository, times(1)).findById(testUser.getUserId());
        // menuRepository.findById가 1번 호출되었는지 검증
        verify(menuRepository, times(1)).findById(testMenu.getId());
        // userRepository.save가 1번 호출되었고, 업데이트된 User 객체가 저장되었는지 검증
        verify(userRepository, times(1)).save(testUser);
        // orderRepository.save가 1번 호출되었고, Order 객체가 저장되었는지 검증
        verify(orderRepository, times(1)).save(any(Order.class));

        // 저장된 Order 객체의 내용 검증 (ArgumentCaptor 사용)
        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getUserId()).isEqualTo(request.getUserId());
        assertThat(capturedOrder.getMenuId()).isEqualTo(request.getMenuId());
        assertThat(capturedOrder.getQuantity()).isEqualTo(request.getQuantity());
        assertThat(capturedOrder.getTotalPrice()).isEqualTo(expectedTotalPrice);
        assertThat(capturedOrder.getStatus()).isEqualTo(Order.OrderStatus.COMPLETED);
        assertThat(capturedOrder.getOrderId()).isEqualTo("test-order-id-123"); // 캡처된 객체의 ID도 확인
    }

    // --- 실패 케이스 (추후 추가될 내용) ---
    // @Test
    // @DisplayName("존재하지 않는 유저로 주문 시 CustomException 발생")
    // void placeOrder_UserNotFound_Failure() { /* ... */ }

    // @Test
    // @DisplayName("존재하지 않는 메뉴로 주문 시 CustomException 발생")
    // void placeOrder_MenuNotFound_Failure() { /* ... */ }

    // @Test
    // @DisplayName("포인트가 부족하면 CustomException 발생")
    // void placeOrder_InsufficientPoints_Failure() { /* ... */ }

    // @Test
    // @DisplayName("주문 수량이 0 이하면 CustomException 발생")
    // void placeOrder_InvalidQuantity_Failure() { /* ... */ }
}