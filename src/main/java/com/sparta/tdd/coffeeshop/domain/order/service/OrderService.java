package com.sparta.tdd.coffeeshop.domain.order.service;

import com.sparta.tdd.coffeeshop.cmmn.exception.CustomException;
import com.sparta.tdd.coffeeshop.cmmn.exception.ErrorCode;
import com.sparta.tdd.coffeeshop.domain.menu.Menu;
import com.sparta.tdd.coffeeshop.domain.menu.repo.MenuRepository;
import com.sparta.tdd.coffeeshop.domain.order.Order;
import com.sparta.tdd.coffeeshop.domain.order.dto.OrderRequest;
import com.sparta.tdd.coffeeshop.domain.order.dto.OrderResponse;
import com.sparta.tdd.coffeeshop.domain.order.repo.OrderRepository;
import com.sparta.tdd.coffeeshop.domain.user.User;
import com.sparta.tdd.coffeeshop.domain.user.repo.UserRepository;

import lombok.RequiredArgsConstructor;


import org.springframework.stereotype.Service;

// 이 서비스가 의존할 다른 서비스나 클라이언트가 있다면 여기에 import 합니다.
// 예: import com.sparta.tdd.cofeeshop.service.PointService;
// 예: import com.sparta.tdd.cofeeshop.service.client.DataCollectionPlatformClient;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // final 필드를 주입받기 위한 Lombok 어노테이션
@Transactional // 서비스 메서드에 트랜잭션 적용
public class OrderService {

    private final UserRepository userRepository;
    private final MenuRepository menuRepository;
    private final OrderRepository orderRepository;

    // TODO: 이 메서드를 구현하여 OrderServiceTest의 성공 케이스를 통과시키세요.
    public OrderResponse placeOrder(OrderRequest request) {
    	// 1. 유효성 검사: 주문 수량
        if (request.getQuantity() <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "주문 수량은 0보다 커야 합니다.");
        }

        // 2. 사용자 조회
        // 비관적 락을 적용한 findById를 사용하므로, findById(userId)로 충분합니다.
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // 3. 메뉴 조회
        Menu menu = menuRepository.findById(request.getMenuId())
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND, "메뉴를 찾을 수 없습니다."));

        // 4. 총 결제 금액 계산
        long totalPrice = (long) menu.getPrice() * request.getQuantity();

        // 5. 포인트 잔액 확인 및 차감
        if (user.getPoint() < totalPrice) {
            throw new CustomException(ErrorCode.INSUFFICIENT_POINT, "포인트가 부족합니다.");
        }
        user.deductPoint(totalPrice); // User 엔티티의 deductPoint 메서드 사용

        // 6. 업데이트된 사용자 정보 저장 (Transactional 덕분에 flush 시점에 자동 반영될 수도 있지만, 명시적으로 save)
        // 비관적 락을 사용한 조회 후 엔티티 변경은 자동으로 업데이트됩니다. 명시적 save는 필수는 아님.
        // 하지만 Mockito 테스트 시 save 호출을 verify하기 위해 명시적으로 호출하는 경우가 많습니다.
        userRepository.save(user);

        // 7. 주문 생성
        Order order = new Order(
                request.getUserId(),
                request.getMenuId(),
                request.getQuantity(),
                totalPrice
        );
        // Order 엔티티에서 ID가 @GeneratedValue(strategy = GenerationType.UUID)로 자동 생성되므로,
        // 별도로 ID를 설정할 필요는 없습니다.

        // 8. 주문 저장
        Order savedOrder = orderRepository.save(order);

        // 9. 응답 DTO 생성 및 반환
        return OrderResponse.from(savedOrder, menu.getName(), user.getPoint());
    } 
    
}