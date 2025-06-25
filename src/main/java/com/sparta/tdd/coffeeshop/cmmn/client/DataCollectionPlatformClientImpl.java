package com.sparta.tdd.coffeeshop.cmmn.client; // 인터페이스와 동일한 패키지 사용

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map; // Map 타입을 사용하기 위해 임포트

@Component // Spring Bean으로 등록
@Slf4j // Lombok을 이용한 로그 객체 자동 생성
public class DataCollectionPlatformClientImpl implements DataCollectionPlatformClient { // 인터페이스 구현

    /**
     * 주문 데이터를 외부 데이터 수집 플랫폼으로 비동기적으로 전송합니다.
     * 이 메서드는 @Async 어노테이션이 있으므로, 호출자는 즉시 제어권을 돌려받고
     * 실제 전송 작업은 별도의 스레드에서 백그라운드로 처리됩니다.
     * (메인 애플리케이션 클래스에 @EnableAsync 추가 필수)
     *
     * @param orderData 주문 데이터를 담은 Map (userId, menuId, paymentAmount 등 포함)
     */
    @Override // 인터페이스 메서드 구현임을 명시
    @Async // 이 메서드를 비동기적으로 실행하도록 설정
    public void sendOrderData(Map<String, Object> orderData) {
        // Map에서 필요한 데이터 추출
        String userId = (String) orderData.get("userId");
        Long menuId = (Long) orderData.get("menuId");
        Long paymentAmount = (Long) orderData.get("paymentAmount");

        log.info("[데이터 수집 플랫폼] 주문 데이터 전송 시작: userId={}, menuId={}, paymentAmount={}",
                userId, menuId, paymentAmount);
        try {
            // --- 실제 외부 API 호출 로직 (Mocking) ---
            // 여기서는 네트워크 지연을 시뮬레이션하기 위해 잠시 대기합니다.
            Thread.sleep(100); // 100ms 지연 시뮬레이션

            log.info("[데이터 수집 플랫폼] 주문 데이터 전송 성공: userId={}", userId);
        } catch (InterruptedException e) {
            // 스레드 인터럽트 시 예외 처리
            Thread.currentThread().interrupt(); // 인터럽트 상태 복원
            log.error("[데이터 수집 플랫폼] 주문 데이터 전송 중단됨 (스레드 인터럽트): {}", e.getMessage());
        } catch (Exception e) {
            // 그 외 전송 중 발생할 수 있는 모든 예외 처리
            log.error("[데이터 수집 플랫폼] 주문 데이터 전송 실패: userId={}, 에러 메시지={}", userId, e.getMessage(), e);
            // 실제 시스템에서는 전송 실패 시 재시도 로직, 데드레터 큐(DLQ) 저장 등을 고려해야 합니다.
        }
    }
}