package com.sparta.tdd.coffeeshop.domain.order;

import com.sparta.tdd.coffeeshop.cmmn.client.DataCollectionPlatformClient;
import com.sparta.tdd.coffeeshop.cmmn.exception.CustomException;
import com.sparta.tdd.coffeeshop.cmmn.exception.ErrorCode;
import com.sparta.tdd.coffeeshop.domain.menu.Menu;
import com.sparta.tdd.coffeeshop.domain.menu.repo.MenuRepository;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given; // given-when-then 패턴을 위한 BDDMockito 임포트
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class) // Mockito를 JUnit 5와 함께 사용하도록 설정.
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTest {

    // Mock 객체 선언: 실제 객체 대신 가짜 객체를 사용하여 의존성을 격리합니다.
    @Mock
    private UserRepository userRepository;
    @Mock
    private MenuRepository menuRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private DataCollectionPlatformClient dataCollectionPlatformClient;

    // @Mock으로 선언된 객체들을 이 객체(orderService)에 자동으로 주입합니다.
    @InjectMocks
    private OrderService orderService;

    // 테스트에 사용될 엔티티 객체들. @BeforeEach에서 초기화됩니다.
    private User testUser;
    private Menu testMenu;

    // 각 테스트 메서드 실행 전에 호출되는 초기화 메서드.
    @BeforeEach
    void setUp() {
        // 테스트 사용자 및 메뉴 객체를 초기화합니다.
        // Mockito 스터빙은 여기에 두지 않고 각 @Test 메서드 내에서 정의함으로써
        // 불필요한 스터빙 예외(UnnecessaryStubbingException)를 피하는 전략을 사용합니다.
        testUser = new User("user123", "테스트 유저", 10000L, 0L);
        testMenu = new Menu(1L, "아메리카노", 4000); // ID를 포함하여 Menu 생성
    }

    @Test
    @DisplayName("유효한 요청으로 커피 주문 및 결제에 성공한다.")
    void placeOrder_Success() {
        // Given (테스트를 위한 준비 단계: 입력 데이터 및 Mock 객체 동작 정의)

        // 1. Mock 객체 스터빙: 성공 시나리오에 필요한 모든 Mock 객체의 동작을 정의합니다.

        // userRepository.findById 호출 시 testUser 객체를 반환하도록 스터빙
        given(userRepository.findById(anyString())).willReturn(Optional.of(testUser));
        // menuRepository.findById 호출 시 testMenu 객체를 반환하도록 스터빙
        given(menuRepository.findById(anyLong())).willReturn(Optional.of(testMenu));

        // userRepository.save() 호출 시 User 객체의 version 필드를 리플렉션으로 업데이트하도록 스터빙
        // 이 doAnswer 블록은 save() 메서드가 호출될 때 실제 DB 저장 로직 대신 커스텀 동작을 수행합니다.
        doAnswer(invocation -> {
            User user = invocation.getArgument(0); // save 메서드에 전달된 User 객체를 캡처
            try {
                // User 클래스에 선언된 private/package-private setVersion 메서드를 찾아 접근 가능하게 설정
                Method setVersionMethod = User.class.getDeclaredMethod("setVersion", Long.class);
                setVersionMethod.setAccessible(true);
                // 캡처된 user 객체의 version을 1 증가시킴 (낙관적 락 시뮬레이션)
                setVersionMethod.invoke(user, user.getVersion() + 1L);
            } catch (Exception e) {
                // 리플렉션 실패 시 런타임 예외 발생 (테스트 실패 유도)
                throw new RuntimeException("Failed to set user version via reflection in test", e);
            }
            return user; // 수정된 User 객체를 반환 (실제 save 동작처럼)
        }).when(userRepository).save(any(User.class)); // userRepository의 save 메서드에 대해 위 동작을 적용

        // orderRepository.save() 호출 시 OrderId와 Menu 필드를 리플렉션으로 설정하도록 스터빙
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
            Order capturedOrder = invocation.getArgument(0); // save 메서드에 전달된 Order 객체를 캡처

            // OrderId가 아직 할당되지 않았다면 (예: @GeneratedValue가 DB에서 동작하는 경우) UUID를 할당
            if (capturedOrder.getOrderId() == null) {
                try {
                    // Order 클래스의 setOrderId 메서드를 찾아 접근 가능하게 설정
                    Method setOrderIdMethod = Order.class.getDeclaredMethod("setOrderId", String.class);
                    setOrderIdMethod.setAccessible(true);
                    // 캡처된 Order 객체에 무작위 UUID를 OrderId로 설정
                    setOrderIdMethod.invoke(capturedOrder, UUID.randomUUID().toString());
                } catch (Exception e) {
                    throw new RuntimeException("Failed to set orderId via reflection in test", e);
                }
            }

            // Order 엔티티의 Menu 필드가 null인 경우 (OrderService의 빌더/생성자에서 주입 안 되거나 Mock 동작 문제 등)
            // 미리 정의된 testMenu 객체를 리플렉션을 통해 주입합니다.
            try {
                // Order 클래스에 setMenu 메서드가 있는지 확인하고 있다면 사용
                Method setMenuMethod = Order.class.getDeclaredMethod("setMenu", Menu.class);
                setMenuMethod.setAccessible(true);
                setMenuMethod.invoke(capturedOrder, testMenu);
            } catch (NoSuchMethodException e) {
                // 만약 setMenu 메서드가 없다면 (예: Lombok @Setter를 안 쓰고 필드 직접 주입만 하는 경우),
                // menu 필드에 직접 접근하여 값을 설정합니다.
                try {
                    Field menuField = Order.class.getDeclaredField("menu"); // "menu"라는 필드 이름을 가정
                    menuField.setAccessible(true); // private 필드에 접근 가능하게 설정
                    menuField.set(capturedOrder, testMenu); // 캡처된 Order 객체의 menu 필드에 testMenu 할당
                } catch (Exception fieldEx) {
                    // 필드 접근 실패 시 런타임 예외 발생
                    throw new RuntimeException("Failed to set menu field via reflection in test", fieldEx);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to set menu via reflection in test", e);
            }
            return capturedOrder; // 수정된 Order 객체를 반환
        });

        // DataCollectionPlatformClient.sendOrderData() 호출 시 아무것도 하지 않도록 스터빙 (void 메서드 처리)
        doNothing().when(dataCollectionPlatformClient).sendOrderData(anyMap());


        // 2. 테스트에 필요한 변수 설정
        String userId = testUser.getUserId();
        Long menuId = testMenu.getId();
        int quantity = 2; // 2개 주문

        // 총 결제 금액 예상 및 남은 포인트 계산
        long initialPoint = testUser.getPoint();
        long expectedTotalPrice = (long) testMenu.getPrice() * quantity;
        long expectedRemainingPoints = initialPoint - expectedTotalPrice;

        // 주문 요청 DTO 생성
        OrderRequest request = new OrderRequest(testUser.getUserId(), testMenu.getId(), quantity);

        // Mockito ArgumentCaptor: Mock 객체에 전달된 인자를 캡처하여 검증할 때 사용
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // When (테스트 대상 메서드 실행)
        OrderResponse response = orderService.placeOrder(request); // OrderService의 placeOrder 메서드를 호출

        // Then (결과 검증)

        // 1. 서비스 응답 (OrderResponse DTO) 검증
        assertThat(response).isNotNull(); // 응답이 null이 아닌지 확인
        assertThat(response.getOrderId()).isNotNull(); // OrderId가 정상적으로 할당되었는지 확인
        assertThat(response.getUserId()).isEqualTo(userId); // 사용자 ID 일치 여부
        assertThat(response.getMenuId()).isEqualTo(menuId); // 메뉴 ID 일치 여부
        assertThat(response.getMenuName()).isEqualTo(testMenu.getName()); // 메뉴 이름 일치 여부
        assertThat(response.getQuantity()).isEqualTo(quantity); // 수량 일치 여부
        assertThat(response.getTotalPrice()).isEqualTo(expectedTotalPrice); // 총 가격 일치 여부
        assertThat(response.getRemainingPoints()).isEqualTo(expectedRemainingPoints); // 계산된 예상 잔여 포인트와 비교
        assertThat(response.getStatus()).isEqualTo(Order.OrderStatus.COMPLETED); // 주문 상태가 COMPLETED인지 확인

        // 2. Mock 객체 상호작용 검증: 특정 Mock 메서드가 예상대로 호출되었는지 확인
        verify(userRepository, times(1)).findById(userId); // userRepository.findById가 userId로 1번 호출되었는지
        verify(menuRepository, times(1)).findById(menuId);     // menuRepository.findById가 menuId로 1번 호출되었는지
        verify(userRepository, times(1)).save(userCaptor.capture()); // userRepository.save가 1번 호출되었고, 전달된 User 객체 캡처
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(orderCaptor.capture()); // orderRepository.save가 1번 호출되었고, Order 객체 캡처
        verify(dataCollectionPlatformClient, times(1)).sendOrderData(anyMap()); // dataCollectionPlatformClient.sendOrderData가 1회 호출되었는지

        // 3. 캡처된 User 객체의 상태 검증: 서비스 로직에 의해 변경된 User 객체의 상태를 확인
        User capturedUser = userCaptor.getValue(); // Mockito가 캡처한 User 객체
        assertThat(capturedUser.getPoint()).isEqualTo(expectedRemainingPoints); // 캡처된 User의 포인트가 올바르게 차감되었는지 확인
        assertThat(capturedUser.getVersion()).isEqualTo(1L); // 낙관적 락 버전이 1 증가했는지 확인 (setUp의 doAnswer에 의해)

        // 4. 캡처된 Order 객체의 내용 검증: 서비스 로직에 의해 생성/수정된 Order 객체의 상태를 확인
        Order capturedOrder = orderCaptor.getValue(); // Mockito가 캡처한 Order 객체
        assertThat(capturedOrder.getUserId()).isEqualTo(userId); // Order의 userId가 올바른지
        assertThat(capturedOrder.getMenu()).isNotNull(); // Order의 Menu 객체가 null이 아닌지 (setUp의 doAnswer에 의해 설정)
        assertThat(capturedOrder.getMenu().getId()).isEqualTo(menuId); // Order의 Menu ID가 올바른지
        assertThat(capturedOrder.getQuantity()).isEqualTo(quantity); // Order의 수량이 올바른지
        assertThat(capturedOrder.getTotalPrice()).isEqualTo(expectedTotalPrice); // Order의 총 가격이 올바른지
        assertThat(capturedOrder.getStatus()).isEqualTo(Order.OrderStatus.COMPLETED); // Order의 상태가 완료되었는지
        assertThat(capturedOrder.getOrderId()).isNotNull(); // save 시점에 UUID가 부여되었는지 확인 (setUp의 doAnswer에 의해)
    }

    // --- 실패 케이스 테스트들 ---

    @Test
    @DisplayName("포인트가 부족하면 CustomException 발생")
    void placeOrder_InsufficientPoints_Failure() {
        // Given (이 테스트에만 특화된 Mock 객체 동작 정의)

        // 테스트용 사용자 객체를 재정의 (초기 포인트가 적게 설정)
        testUser = new User("user123", "테스트 유저", 1000L, 0L); // 1000포인트만 가짐

        // userRepository.findById 호출 시 재정의된 testUser 객체를 반환하도록 스터빙
        // 이 스터빙은 이 테스트 메서드 내에서만 유효하며, setUp의 스터빙을 오버라이드합니다.
        given(userRepository.findById(anyString())).willReturn(Optional.of(testUser));
        // 메뉴 조회는 여전히 성공해야 하므로 스터빙
        given(menuRepository.findById(anyLong())).willReturn(Optional.of(testMenu));

        // 이 실패 케이스에서는 OrderService의 placeOrder 메서드가 중간에 예외를 던지므로,
        // userRepository.save()나 orderRepository.save(), dataCollectionPlatformClient.sendOrderData()는
        // 호출되지 않습니다. 따라서 이 메서드들에 대한 스터빙은 불필요합니다 (UnnecessaryStubbingException 방지).
        // 만약 setUp에 이 스터빙들이 있었다면, 이 테스트에서 UnnecessaryStubbingException이 발생했을 것입니다.

        OrderRequest request = new OrderRequest(testUser.getUserId(), testMenu.getId(), 2);

        // When & Then (예외 발생 확인)
        // assertThrows를 사용하여 특정 예외가 발생하는지 확인하고, 예외 객체를 캡처합니다.
        CustomException exception = assertThrows(CustomException.class, () -> {
            orderService.placeOrder(request); // 부족한 포인트로 주문 시도
        });

        // 예외의 에러 코드와 메시지를 검증합니다.
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_POINT);
        assertThat(exception.getMessage()).isEqualTo("포인트가 부족합니다.");

        // Mock 객체 상호작용 검증: 특정 메서드가 호출되지 않았음을 검증 (times(0))
        verify(userRepository, times(1)).findById(testUser.getUserId()); // 사용자 조회는 1번 발생
        verify(menuRepository, times(1)).findById(testMenu.getId()); // 메뉴 조회도 1번 발생
        verify(userRepository, times(0)).save(any(User.class)); // 포인트 부족으로 save는 호출 안 됨
        verify(orderRepository, times(0)).save(any(Order.class)); // 주문 저장도 호출 안 됨
        verify(dataCollectionPlatformClient, times(0)).sendOrderData(anyMap()); // 데이터 전송도 호출 안 됨
    }

    @Test
    @DisplayName("주문 수량이 0 이하면 CustomException 발생")
    void placeOrder_InvalidQuantity_Failure() {
        // Given (이 테스트에만 특화된 Mock 객체 동작 정의)

        // 이 테스트는 OrderService 진입 시 가장 먼저 수량 유효성 검사에서 실패합니다.
        // 따라서 userRepository, menuRepository 등의 Mock 객체는 전혀 호출되지 않습니다.
        // 이 테스트 메서드 내에서는 Mockito 스터빙이 필요 없습니다.

        OrderRequest request = new OrderRequest(testUser.getUserId(), testMenu.getId(), 0); // 수량 0으로 요청

        // When & Then (예외 발생 확인)
        CustomException exception = assertThrows(CustomException.class, () -> {
            orderService.placeOrder(request); // 유효하지 않은 수량으로 주문 시도
        });

        // 예외의 에러 코드와 메시지를 검증합니다.
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
        assertThat(exception.getMessage()).isEqualTo("주문 수량은 0보다 커야 합니다.");

        // Mock 객체 상호작용 검증: 어떤 Mock 메서드도 호출되지 않았는지 확인
        verify(userRepository, times(0)).findById(anyString());
        verify(menuRepository, times(0)).findById(anyLong());
        verify(userRepository, times(0)).save(any(User.class));
        verify(orderRepository, times(0)).save(any(Order.class));
        verify(dataCollectionPlatformClient, times(0)).sendOrderData(anyMap());
    }

    @Test
    @DisplayName("존재하지 않는 유저로 주문 시 CustomException 발생")
    void placeOrder_UserNotFound_Failure() {
        // Given (이 테스트에만 특화된 Mock 객체 동작 정의)

        // userRepository.findById 호출 시 Optional.empty()를 반환하여 사용자 없음 시뮬레이션
        given(userRepository.findById(anyString())).willReturn(Optional.empty());

        // 이 시나리오에서는 사용자를 찾지 못해 서비스가 종료되므로,
        // menuRepository, orderRepository, dataCollectionPlatformClient는 호출되지 않습니다.
        // 따라서 이들에 대한 스터빙은 불필요합니다.

        OrderRequest request = new OrderRequest("nonExistentUser", testMenu.getId(), 1);

        // When & Then (예외 발생 확인)
        CustomException exception = assertThrows(CustomException.class, () -> {
            orderService.placeOrder(request); // 존재하지 않는 사용자로 주문 시도
        });

        // 예외의 에러 코드와 메시지를 검증합니다.
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo("사용자를 찾을 수 없습니다.");

        // Mock 객체 상호작용 검증
        verify(userRepository, times(1)).findById("nonExistentUser"); // 사용자 조회는 1번 발생
        verify(menuRepository, times(0)).findById(anyLong()); // 메뉴 조회는 호출 안 됨
        verify(userRepository, times(0)).save(any(User.class));
        verify(orderRepository, times(0)).save(any(Order.class));
        verify(dataCollectionPlatformClient, times(0)).sendOrderData(anyMap());
    }

    @Test
    @DisplayName("존재하지 않는 메뉴로 주문 시 CustomException 발생")
    void placeOrder_MenuNotFound_Failure() {
        // Given (이 테스트에만 특화된 Mock 객체 동작 정의)

        // userRepository.findById 호출 시 testUser를 반환 (사용자 조회는 성공)
        given(userRepository.findById(anyString())).willReturn(Optional.of(testUser));
        // menuRepository.findById 호출 시 Optional.empty()를 반환하여 메뉴 없음 시뮬레이션
        given(menuRepository.findById(anyLong())).willReturn(Optional.empty());

        // 이 시나리오에서는 메뉴를 찾지 못해 서비스가 종료되므로,
        // orderRepository, dataCollectionPlatformClient는 호출되지 않습니다.
        // userRepository.save()도 포인트 차감 로직까지 도달하지 못해 호출되지 않습니다.

        OrderRequest request = new OrderRequest(testUser.getUserId(), 999L, 1); // 존재하지 않는 메뉴 ID

        // When & Then (예외 발생 확인)
        CustomException exception = assertThrows(CustomException.class, () -> {
            orderService.placeOrder(request); // 존재하지 않는 메뉴로 주문 시도
        });

        // 예외의 에러 코드와 메시지를 검증합니다.
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MENU_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo("메뉴를 찾을 수 없습니다.");

        // Mock 객체 상호작용 검증
        verify(userRepository, times(1)).findById(testUser.getUserId()); // 사용자 조회는 1번 발생
        verify(menuRepository, times(1)).findById(999L); // 메뉴 조회는 1번 발생
        verify(userRepository, times(0)).save(any(User.class));
        verify(orderRepository, times(0)).save(any(Order.class));
        verify(dataCollectionPlatformClient, times(0)).sendOrderData(anyMap());
    }
}