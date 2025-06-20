package com.sparta.tdd.test.cofeeshop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/*import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sparta.tdd.cofeeshop.exception.CustomException;
import com.sparta.tdd.cofeeshop.exception.ErrorCode;
import com.sparta.tdd.cofeeshop.model.User;
import com.sparta.tdd.cofeeshop.repository.UserRepository;
import com.sparta.tdd.cofeeshop.service.PointService;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;*/


import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sparta.tdd.cofeeshop.exception.CustomException;
import com.sparta.tdd.cofeeshop.exception.ErrorCode;
import com.sparta.tdd.cofeeshop.model.User;
import com.sparta.tdd.cofeeshop.repository.UserRepository;
import com.sparta.tdd.cofeeshop.service.PointService;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

//import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.jupiter.api.Assertions.assertDoesNotThrow; // 예외가 발생하지 않음을 검증
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString; // userId가 String 타입이라면 anyString() 사용
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;


@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PointService pointService;

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

        // Mocking: userRepository.findById가 testUser를 반환하도록 설정
        given(userRepository.findById(testUser.getUserId())).willReturn(Optional.of(testUser));
        // Mocking: userRepository.save가 호출될 때 전달된 User 객체를 그대로 반환하도록 설정
        // (실제 save 동작을 흉내내어 user 객체의 상태 변화가 유지되도록)
        given(userRepository.save(any(User.class))).willReturn(testUser);


        // When
        pointService.chargePoint(testUser.getUserId(), chargeAmount);

        // Then
        // 1. userRepository.findById가 호출되었는지 검증
        verify(userRepository, times(1)).findById(testUser.getUserId());
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

        // Mocking (사용자가 존재한다고 가정)
        //given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

        // When & Then
        CustomException negativeException = assertThrows(CustomException.class,
                () -> pointService.chargePoint(userId, negativeAmount));
        assertEquals(ErrorCode.INVALID_INPUT, negativeException.getErrorCode());
        assertEquals("충전 금액은 0보다 커야 합니다.", negativeException.getMessage());

        CustomException zeroException = assertThrows(CustomException.class,
                () -> pointService.chargePoint(userId, zeroAmount));
        assertEquals(ErrorCode.INVALID_INPUT, zeroException.getErrorCode());
        assertEquals("충전 금액은 0보다 커야 합니다.", zeroException.getMessage());

        // findById와 save 메서드가 호출되지 않았는지 검증
        verify(userRepository, never()).findById(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("포인트 충전 시 사용자가 존재하지 않으면 USER_NOT_FOUND 예외가 발생해야 한다.")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        String nonExistentUserId = "nonExistent";
        long chargeAmount = 1000L;

        // Mocking: userRepository.findById가 Optional.empty()를 반환하도록 설정
        given(userRepository.findById(nonExistentUserId)).willReturn(Optional.empty());

        // When & Then
        CustomException exception = assertThrows(CustomException.class,
                () -> pointService.chargePoint(nonExistentUserId, chargeAmount));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        // save 메서드가 호출되지 않았는지 검증
        verify(userRepository, times(0)).save(any(User.class));
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

        // Mocking: findById가 호출될 때마다 동일한 User 객체를 반환
        // 이 Mocking은 실제 DB 락을 흉내내지 못하므로, 이 테스트는 실패할 가능성이 높습니다.
        // 이것이 단위 테스트의 한계이며, 동시성 문제는 통합 테스트에서 DB 락으로 해결해야 함을 보여줍니다.
        given(userRepository.findById(userId)).willReturn(Optional.of(concurrentUser));
        given(userRepository.save(any(User.class))).willReturn(concurrentUser);

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        // When
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    pointService.chargePoint(userId, chargeAmountPerThread);
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
        // 이 단위 테스트는 Mockito의 한계를 보여주기 위함입니다.
        // 실제 DB 락이 없다면 race condition으로 인해 예상 값과 다르게 나올 수 있습니다.
        // 따라서 현재 이 단위 테스트는 실패할 가능성이 높고, 그것이 의도된 동작입니다.
        // 이 실패를 통해 동시성 처리에 대한 추가 구현이 필요함을 인지합니다.
        // 최종 검증은 통합 테스트에서 DB 락을 적용한 후 수행합니다.
        System.out.println("Concurrent Test: Final point for " + userId + ": " + concurrentUser.getPoint());
        // 이 시점에서 assert는 주석 처리하거나, 실제 나올 수 있는 '잘못된' 값으로 맞춰서 통과시키고,
        // 통합 테스트에서 '올바른' 값으로 통과시키는 방식으로 TDD를 진행할 수 있습니다.
        // 여기서는 명확히 'Red' 상태로 남기기 위해 실패를 유도합니다.
        assertEquals(expectedFinalPoint, concurrentUser.getPoint(),
                "Mockito 단위 테스트에서는 동시성 문제가 시뮬레이션되지 않아 최종 포인트가 일치할 수 있습니다."); // <-- 여기!

	    // Mockito verify 추가:
	    verify(userRepository, times(numberOfThreads)).findById(userId);
	    verify(userRepository, times(numberOfThreads)).save(concurrentUser);
    }
}
