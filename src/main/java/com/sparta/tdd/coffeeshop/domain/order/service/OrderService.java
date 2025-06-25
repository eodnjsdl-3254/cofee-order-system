package com.sparta.tdd.coffeeshop.domain.order.service;

import com.sparta.tdd.coffeeshop.cmmn.client.DataCollectionPlatformClient;
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
import lombok.extern.slf4j.Slf4j; // log 객체를 위한 Slf4j import

import java.util.HashMap;
import java.util.Map;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // final 필드를 주입받기 위한 Lombok 어노테이션
@Transactional // 서비스 메서드에 트랜잭션 적용
@Slf4j // log 객체 자동 생성
public class OrderService {

    private final UserRepository userRepository;
    private final MenuRepository menuRepository;
    private final OrderRepository orderRepository;
    private final DataCollectionPlatformClient dataCollectionPlatformClient; // 인터페이스 타입으로 주입

    /**
     * 커피 주문 및 결제를 처리합니다.
     * 사용자 포인트 차감 시 낙관적 락(Optimistic Locking)을 사용하여 동시성 문제를 방지합니다.
     * 주문 내역은 비동기적으로 외부 데이터 수집 플랫폼으로 전송됩니다.
     *
     * @param request 주문 요청 정보 (userId, menuId, quantity)
     * @return 주문 처리 결과 DTO (OrderResponse)
     * @throws CustomException 사용자/메뉴를 찾을 수 없거나 포인트 부족, 동시성 충돌 시 발생
     */    
    public OrderResponse placeOrder(OrderRequest request) {

        // 0. 주문 요청 초기 로그 (기존 메시지 유지)
        log.info("주문 요청 시작: userId={}, menuId={}, quantity={}",
                request.getUserId(), request.getMenuId(), request.getQuantity());

        // 1. 유효성 검사: 주문 수량
        log.debug("주문 수량 유효성 검사 시작: quantity={}", request.getQuantity());
        if (request.getQuantity() <= 0) {
            log.warn("주문 실패: 주문 수량이 유효하지 않음. quantity={}", request.getQuantity());
            throw new CustomException(ErrorCode.INVALID_INPUT, "주문 수량은 0보다 커야 합니다.");
        }
        log.debug("주문 수량 유효성 검사 통과.");

        // --- 동시성 충돌 처리를 위한 try-catch 블록 (낙관적 락을 가정) ---
        try {
	        // 2. 사용자 조회
	        log.debug("사용자 조회 시도: userId={}", request.getUserId());
	        // 비관적 락을 적용한 findById를 사용하므로, findById(userId)로 충분합니다.
	        User user = userRepository.findById(request.getUserId())
	                .orElseThrow(() -> {
	                    log.error("주문 실패: 사용자를 찾을 수 없음. userId={}", request.getUserId()); // 사용자를 못 찾으면 치명적 오류로 간주하여 ERROR
	                    return new CustomException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다.");
	                });
	        log.info("사용자 조회 성공: userId={}, currentPoint={}", user.getUserId(), user.getPoint());
	        log.debug("사용자 버전 확인: version={}", user.getVersion()); // 낙관적 락을 위해 버전 정보도 로그로 남김
	
	        // 3. 메뉴 조회
	        log.debug("메뉴 조회 시도: menuId={}", request.getMenuId());
	        Menu menu = menuRepository.findById(request.getMenuId())
	                .orElseThrow(() -> {
	                    log.error("주문 실패: 메뉴를 찾을 수 없음. menuId={}", request.getMenuId()); // 메뉴를 못 찾으면 치명적 오류로 간주하여 ERROR
	                    return new CustomException(ErrorCode.MENU_NOT_FOUND, "메뉴를 찾을 수 없습니다.");
	                });
	        log.info("메뉴 조회 성공: menuId={}, menuName={}, menuPrice={}",
	                 menu.getId(), menu.getName(), menu.getPrice());
	
	        // 4. 총 결제 금액 계산 및 클라이언트 요청 금액과의 비교
	        log.debug("총 결제 금액 계산 시작: menuPrice={}, quantity={}", menu.getPrice(), request.getQuantity());
	        long calculatedTotalPrice = (long) menu.getPrice() * request.getQuantity();
	
	        // 클라이언트에서 넘겨준 totalPrice가 있다면, 서버에서 계산한 값과 비교하여 검증
	        // request.getTotalPrice()가 클라이언트가 보낸 값이라고 가정합니다.
	        if (request.getTotalPrice() != 0 && calculatedTotalPrice != request.getTotalPrice()) {
	            log.warn("주문 경고: 클라이언트 제공 총 가격({})과 서버 계산 총 가격({})이 일치하지 않습니다.",
	                     request.getTotalPrice(), calculatedTotalPrice);
	            // 이 경우 예외를 발생시킬지, 서버 계산 가격으로 강제할지는 정책에 따라 다릅니다.
	            // 여기서는 서버 계산 가격을 따르거나, 필요시 예외를 발생시킬 수 있습니다.
	            // throw new CustomException(ErrorCode.INVALID_INPUT, "요청된 총 가격이 올바르지 않습니다.");
	        }
	        log.info("최종 결제 금액 결정: {}원", calculatedTotalPrice);
	
	
	        // 5. 포인트 잔액 확인 및 차감
	        log.debug("포인트 잔액 확인: userPoint={}, requiredPrice={}", user.getPoint(), calculatedTotalPrice);
	        if (user.getPoint() < calculatedTotalPrice) {
	            log.warn("주문 실패: 포인트 부족. userId={}, 현재 포인트={}, 필요 포인트={}",
	                     user.getUserId(), user.getPoint(), calculatedTotalPrice);
	            throw new CustomException(ErrorCode.INSUFFICIENT_POINT, "포인트가 부족합니다.");
	        }
	        user.deductPoint(calculatedTotalPrice); // User 엔티티의 deductPoint 메서드 사용
	        log.info("포인트 차감 완료: userId={}, 차감 후 잔액={}", user.getUserId(), user.getPoint());
	
	        // 6. 업데이트된 사용자 정보 저장 (낙관적 락의 핵심: 버전 필드를 통한 동시성 검증)
	        // findById로 조회된 user 엔티티의 변경은 Transactional 덕분에 flush 시점에 업데이트 됩니다.
            // 여기서는 낙관적 락의 버전 체크와 update를 위해 명시적으로 save를 호출하는 것이 좋습니다.
            // ObjectOptimisticLockingFailureException은 이 save 호출 또는 flush 시점에서 발생합니다.
	        userRepository.save(user); // 변경된 User 엔티티를 명시적으로 저장
	        log.debug("업데이트된 사용자 정보 저장 호출 완료.");
	
	
	        // 7. 주문 엔티티 생성
	        log.debug("주문 엔티티 생성 시작...");
	        Order order = Order.builder()
	                .userId(request.getUserId())
	                .menu(menu)
	                .quantity(request.getQuantity())
	                .totalPrice(calculatedTotalPrice) // 계산된 최종 가격 사용
	                // orderDate와 status는 @Builder.Default로 자동 설정
	                .build();
	        log.debug("주문 엔티티 생성 완료: 임시 Order ID={}", order.getOrderId()); // ID가 아직 DB에 저장 전이라면 null일 수 있음
	
	        // 8. 주문 저장
	        Order savedOrder = orderRepository.save(order);
	        log.info("주문 엔티티 최종 저장 완료: orderId={}", savedOrder.getOrderId()); // DB 저장 후 실제 ID 확인
	
	        // 9. 데이터 수집 플랫폼으로 실시간 전송
	        // 현재는 동기 호출이지만, "실시간 전송" 요구사항에 따라 메시지 큐를 통한 비동기 처리 고려 가능
	        log.info("데이터 수집 플랫폼으로 주문 내역 전송 시작: userId={}, menuId={}, totalPrice={}",
	                 savedOrder.getUserId(), savedOrder.getMenu().getId(), savedOrder.getTotalPrice());
	        
	        // Map<String, Object> 형태로 데이터 구성
            Map<String, Object> orderDataForCollection = new HashMap<>();
            orderDataForCollection.put("userId", savedOrder.getUserId());
            orderDataForCollection.put("menuId", savedOrder.getMenu().getId());
            orderDataForCollection.put("paymentAmount", savedOrder.getTotalPrice());
            orderDataForCollection.put("orderId", savedOrder.getOrderId());
            orderDataForCollection.put("quantity", savedOrder.getQuantity());
            orderDataForCollection.put("orderDate", savedOrder.getOrderDate().toString()); // LocalDateTime을 String으로 변환
            orderDataForCollection.put("menuName", savedOrder.getMenu().getName());
            orderDataForCollection.put("userName", user.getUserName()); 
            dataCollectionPlatformClient.sendOrderData(orderDataForCollection); // Map 형태로 전달
            log.info("데이터 수집 플랫폼 전송 로직 호출 완료.");

	        
	        // 10. 응답 DTO 생성 및 반환
            // OrderResponse.from() 메서드도 Order 엔티티의 변화에 맞게 수정 필요 
            // (OrderResponse.from 메서드가 savedOrder.getMenu().getName()으로 직접 가져올 수 있습니다.
            OrderResponse response = OrderResponse.from(savedOrder, user.getPoint());
            log.info("주문 처리 최종 완료: orderId={}", response.getOrderId());
            return response;
            
	    } catch (ObjectOptimisticLockingFailureException e) {
	        // 낙관적 락 충돌 발생 시
	        log.warn("주문 실패: 낙관적 락 충돌 발생. userId={}, errorMessage={}", request.getUserId(), e.getMessage());
	        throw new CustomException(ErrorCode.CONCURRENCY_FAILURE, "주문 처리 중 동시성 충돌이 발생했습니다. 다시 시도해주세요.");
	    } catch (CustomException e) {
	        // 정의된 CustomException은 그대로 다시 던짐
	        throw e;
	    } catch (Exception e) {
	        // 그 외 예상치 못한 모든 예외 처리
	        log.error("주문 처리 중 예상치 못한 오류 발생: userId={}, errorMessage={}", request.getUserId(), e.getMessage(), e);
	        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "주문 처리 중 예상치 못한 오류가 발생했습니다.");
	    }
    }
}