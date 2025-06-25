package com.sparta.tdd.test.coffeeshop.service;

import com.sparta.tdd.coffeeshop.cmmn.client.DataCollectionPlatformClient;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given; // given-when-then 패턴을 위한 BDDMockito 임포트
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
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

    @Mock
    private DataCollectionPlatformClient dataCollectionPlatformClient;

    @InjectMocks // @Mock 객체들을 이 객체에 주입
    private OrderService orderService;

    //@Spy private User testUser = new User("user123", 10000L);;
    private User testUser;    
    private Menu testMenu;

    @BeforeEach
    void setUp() {
    	testUser = new User("user123", "테스트 유저", 10000L, 0L); // ❗ 올바른 생성자 인자 순서로 초기화
        testMenu = new Menu(1L, "아메리카노", 4000); // ID를 포함하여 Menu 생성
        
        given(userRepository.findById(anyString())).willReturn(Optional.of(testUser));
        // MenuRepository.findById() 스터빙
        given(menuRepository.findById(anyLong())).willReturn(Optional.of(testMenu));
        
        // save 호출 시 User 객체의 point 필드는 이미 서비스에서 차감된 상태일 것이므로,
        // 여기서는 version만 증가시키고, 입력된 User 객체 자체를 반환하도록 합니다.
        doAnswer(invocation -> {
            User user = invocation.getArgument(0); // save 호출 시 전달된 User 객체
            user.setVersion(user.getVersion() + 1); // version 필드를 1 증가시킵니다.
            return user; // 수정된 User 객체를 반환합니다.
        }).when(userRepository).save(any(User.class));

        
        // OrderRepository.save() 스터빙: 저장 시 OrderId 자동 할당 시뮬레이션
        // Order 엔티티에 @GeneratedValue(UUID)가 있으므로, save 시점에 ID가 생성된다고 가정하고 시뮬레이션
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
            Order capturedOrder = invocation.getArgument(0);
            if (capturedOrder.getOrderId() == null) {
                capturedOrder.setOrderId(java.util.UUID.randomUUID().toString()); // 임시 UUID 부여
            }
            return capturedOrder;
        });

        // ❗ DataCollectionPlatformClient.sendOrderData() 스터빙 (void 메서드이므로 doNothing 사용)
        doNothing().when(dataCollectionPlatformClient).sendOrderData(any(java.util.Map.class));
    }

    @Test
    @DisplayName("유효한 요청으로 커피 주문 및 결제에 성공한다.")
    void placeOrder_Success() {
        // Given
    	String userId = testUser.getUserId();
        Long menuId = testMenu.getId();
        int quantity = 2; // 2개 주문
        
    	// 총 결제 금액 예상: testMenu의 가격 * 수량
    	long initialPoint = testUser.getPoint(); // 테스트 시작 전 사용자 초기 포인트
        long expectedTotalPrice = (long) testMenu.getPrice() * quantity;
        long expectedRemainingPoints = initialPoint - expectedTotalPrice; // 차감 후 예상 포인트 계산
        

        // OrderRequest 생성 (클라이언트에서 totalPrice는 보통 보내지 않으므로, 테스트에서는 0으로 보내거나 제거 고려)
        // OrderRequest에 totalPrice 필드가 없다면, request = new OrderRequest(userId, menuId, quantity);
        OrderRequest request = new OrderRequest(testUser.getUserId(), testMenu.getId(), quantity, expectedTotalPrice); // totalPrice 필드 제거 또는 0으로 보정 (OrderRequest DTO에 맞게)

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // When
        OrderResponse response = orderService.placeOrder(request);

        // Then
        // 1. 응답 검증
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isNotNull(); // OrderId가 정상적으로 할당되었는지 확인
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getMenuId()).isEqualTo(menuId);
        assertThat(response.getMenuName()).isEqualTo(testMenu.getName());
        assertThat(response.getQuantity()).isEqualTo(quantity);
        assertThat(response.getTotalPrice()).isEqualTo(expectedTotalPrice);
        assertThat(response.getRemainingPoints()).isEqualTo(expectedRemainingPoints); // 계산된 예상 잔여 포인트와 비교
        assertThat(response.getStatus()).isEqualTo(Order.OrderStatus.COMPLETED);
        
        // 2. Mock 객체 상호작용 검증
        verify(userRepository, times(1)).findById(userId); // 사용자 1회 조회
        verify(menuRepository, times(1)).findById(menuId);     // 메뉴 1회 조회
        verify(userRepository, times(1)).save(userCaptor.capture()); // userRepository.save가 1번 호출되었고, 전달된 User 객체 캡처
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(orderCaptor.capture()); // orderRepository.save가 1번 호출되었고, Order 객체 캡처
        verify(dataCollectionPlatformClient, times(1)).sendOrderData(any(java.util.Map.class)); // 데이터 전송 메서드가 1회 호출되었는지 검증

        // 3. 캡처된 User 객체의 상태 검증
        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getPoint()).isEqualTo(expectedRemainingPoints); // 캡처된 User의 포인트가 올바른지 확인
        assertThat(capturedUser.getVersion()).isEqualTo(1L); // 낙관적 락 버전이 증가했는지 확인 (저장 시 자동 증가 가정)

        // 4. 캡처된 Order 객체의 내용 검증
        Order capturedOrder = orderCaptor.getValue(); // 마지막 save 호출에서 캡처된 Order 객체
        assertThat(capturedOrder.getUserId()).isEqualTo(userId);
        assertThat(capturedOrder.getMenuId()).isEqualTo(menuId);
        assertThat(capturedOrder.getQuantity()).isEqualTo(quantity);
        assertThat(capturedOrder.getTotalPrice()).isEqualTo(expectedTotalPrice);
        assertThat(capturedOrder.getStatus()).isEqualTo(Order.OrderStatus.COMPLETED);
        assertThat(capturedOrder.getOrderId()).isNotNull(); // save 시점에 UUID가 부여되었는지 확인
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