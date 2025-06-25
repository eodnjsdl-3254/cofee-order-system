package com.sparta.tdd.coffeeshop.domain.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sparta.tdd.coffeeshop.cmmn.exception.CustomException;
import com.sparta.tdd.coffeeshop.cmmn.exception.ErrorCode;
import com.sparta.tdd.coffeeshop.domain.user.User;
import com.sparta.tdd.coffeeshop.domain.user.repo.UserRepository;
import com.sparta.tdd.coffeeshop.domain.user.service.UserService;

import jakarta.persistence.EntityManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.BDDMockito.given;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock 
    private EntityManager entityManager; // EntityManager Nock 추가

    @InjectMocks
    private UserService userPointService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Mocking을 위한 테스트 사용자 초기화
        testUser = new User("testUser001", 5000);
    }

    @Test
    @DisplayName("유효한 금액으로 사용자의 포인트가 성공적으로 증가해야 한다.")
    void shouldIncreaseUserPointWithValidAmount() {
        // Given
        long chargeAmount = 2000L;
        long expectedPoint = testUser.getPoint() + chargeAmount;

        // Mocking: entityManager.find가 testUser를 반환하도록 설정
        when(entityManager.find(eq(User.class), eq(testUser.getUserId()), anyMap()))
        .thenReturn(testUser);
        
        // Mocking: userRepository.save가 호출될 때 전달된 User 객체를 그대로 반환하도록 설정
        when(userRepository.save(any(User.class))).thenReturn(testUser); // testUser의 참조를 반환하여 상태 변경 반영

        // When
        userPointService.chargePoint(testUser.getUserId(), chargeAmount);

        // Then
        // 1. entityManager.find가 호출되었는지 검증 (findById 대신)
        verify(entityManager, times(1)).find(eq(User.class), eq(testUser.getUserId()), anyMap());
                
        // 2. userRepository.save가 호출되었는지 검증
        verify(userRepository, times(1)).save(testUser);

        // 3. User 객체의 포인트가 올바르게 증가했는지 검증
        assertEquals(expectedPoint, testUser.getPoint());
    }

    @Test
    @DisplayName("포인트 충전 시 음수 또는 0 금액인 경우 INVALID_INPUT 예외가 발생해야 한다.")
    void shouldThrowExceptionWhenChargingWithInvalidAmount() {
        // Given
        String userId = testUser.getUserId();
        long negativeAmount = -100L;
        long zeroAmount = 0L;

        // When & Then
        CustomException negativeException = assertThrows(CustomException.class,
                () -> userPointService.chargePoint(userId, negativeAmount));
        assertEquals(ErrorCode.INVALID_INPUT, negativeException.getErrorCode());
        assertEquals("충전 금액은 0보다 커야 합니다.", negativeException.getMessage());

        CustomException zeroException = assertThrows(CustomException.class,
                () -> userPointService.chargePoint(userId, zeroAmount));
        assertEquals(ErrorCode.INVALID_INPUT, zeroException.getErrorCode());
        assertEquals("충전 금액은 0보다 커야 합니다.", zeroException.getMessage());

        // find 메서드 (EntityManager)와 save 메서드 (UserRepository)가 호출되지 않았는지 검증
        verify(entityManager, never()).find(eq(User.class), anyString(), anyMap()); // 어떤 find 호출도 없어야 함
        
        // findById와 save 메서드가 호출되지 않았는지 검증
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("포인트 충전 시 사용자가 존재하지 않으면 USER_NOT_FOUND 예외가 발생해야 한다.")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        String nonExistentUserId = "nonExistent";
        long chargeAmount = 1000L;

        // Mocking: entityManager.find가 null을 반환하도록 설정 (사용자 없음)   
        when(entityManager.find(eq(User.class), eq(nonExistentUserId), anyMap()))
        .thenReturn(null);

	    // When & Then
	    CustomException exception = assertThrows(CustomException.class,
	            () -> userPointService.chargePoint(nonExistentUserId, chargeAmount));
	
	    assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
	
	    // find 메서드 (EntityManager)가 호출되었는지 검증
	    verify(entityManager, times(1)).find(eq(User.class), eq(nonExistentUserId), anyMap()); 	    
	    // save 메서드가 호출되지 않았는지 검증
	    verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("동시 충전 요청 시 포인트 일관성이 유지되어야 한다 (통합 테스트 필요).")
    void shouldHandleConcurrentPointChargeRequestsCorrectly() throws InterruptedException {
        // Given
        String userId = "concurrentUser";
        long initialPoint = 0L;
        long chargeAmountPerThread = 100L;
        int numberOfThreads = 10;
        long expectedFinalPoint = initialPoint + (chargeAmountPerThread * numberOfThreads);

        User concurrentUser = new User(userId, initialPoint);

        // Mocking: entityManager.find가 호출될 때마다 '동일한' User 객체를 반환하도록 설정
        // 이 Mocking은 실제 DB의 낙관적 락 동작을 시뮬레이션하지 않습니다.
        // Mock 객체는 싱글턴처럼 동작하므로, 여러 스레드가 동시에 이 Mock 객체의 필드를 변경하게 되어 race condition이 발생합니다.
        when(entityManager.find(eq(User.class), eq(userId), anyMap())) // <-- 변경
        .thenReturn(concurrentUser);
        
        // Mocking: userRepository.save는 단순히 호출되는 것을 확인하는 용도로 사용
        when(userRepository.save(any(User.class))).thenReturn(concurrentUser);

        
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        // When
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                	userPointService.chargePoint(userId, chargeAmountPerThread);
                } catch (Exception e) {
                    System.err.println("Concurrent charge failed for " + userId + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS); // 모든 스레드 완료 대기
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS);

        // Then
        // 이 단위 테스트는 Mockito의 한계를 보여주기 위함이며, 실제 DB의 낙관적 락은 시뮬레이션하지 못합니다.
        // 따라서 'concurrentUser.getPoint()'는 예상 값보다 작게 나올 가능성이 높으며, 이는 동시성 문제가 여전히 있음을 시사합니다.
        // 현재 이 단위 테스트는 실패할 가능성이 높고, 그것이 의도된 동작입니다.
        // 이 실패를 통해 동시성 처리에 대한 추가 구현이 필요함을 인지합니다.
        // 최종 검증은 통합 테스트에서 DB 락을 적용한 후 수행합니다.
        System.out.println("Concurrent Test: Final point for " + userId + ": " + concurrentUser.getPoint());
        assertEquals(expectedFinalPoint, concurrentUser.getPoint(),
                "Mockito 단위 테스트에서는 동시성 문제가 시뮬레이션되지 않아 최종 포인트가 일치할 수 있습니다."); 

        // Mockito verify 추가:
        verify(entityManager, times(numberOfThreads)).find(eq(User.class), eq(userId), anyMap());
        verify(userRepository, times(numberOfThreads)).save(concurrentUser);
        
    }
}
