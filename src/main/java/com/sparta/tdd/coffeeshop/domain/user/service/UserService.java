package com.sparta.tdd.coffeeshop.domain.user.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sparta.tdd.coffeeshop.cmmn.exception.CustomException;
import com.sparta.tdd.coffeeshop.cmmn.exception.ErrorCode;
import com.sparta.tdd.coffeeshop.domain.user.User;
import com.sparta.tdd.coffeeshop.domain.user.dto.PointChargeResponse;
import com.sparta.tdd.coffeeshop.domain.user.repo.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final EntityManager entityManager; 

    @Transactional(readOnly = false, propagation = Propagation.REQUIRED) // 포인트 충전은 데이터 변경이므로 @Transactional 필수
    public PointChargeResponse chargePoint(String userId, long amount) {
        // 1. 금액 유효성 검증 (User 조회보다 먼저 수행하여 불필요한 DB 접근 방지)
        if (amount <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "충전 금액은 0보다 커야 합니다.");
        }

        // 2. 사용자 조회 (UserRepository에 @Lock(PESSIMISTIC_WRITE)가 적용되어 락이 걸릴 것임)
        //User user = userRepository.findById(userId)
        //        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        //User user = entityManager.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE);
        
        // LockModeType을 힌트 맵에 명시적으로 추가하여 find 메서드 호출
        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.lock.scope", LockModeType.PESSIMISTIC_WRITE);
        User user = entityManager.find(User.class, userId, properties);

        // properties.put("javax.persistence.lock.scope", LockModeType.PESSIMISTIC_WRITE); // JPA 2.1 이전 버전 (javax.*)
                
        if (user == null) { // user가 null이면
            throw new CustomException(ErrorCode.USER_NOT_FOUND); // CustomException을 던져야 합니다.
        }
        
        // 3. 포인트 충전 (User 엔티티의 비즈니스 로직 호출)
        user.chargePoint(amount); // User 엔티티 내부에서 포인트 증가
        userRepository.save(user); // 변경된 User 엔티티 저장 (JPA dirty checking으로 자동 저장되지만 명시적으로)

        // 4. 응답 DTO 생성 및 반환
        return PointChargeResponse.from(user);
    }
    
    @Transactional(readOnly = false, propagation = Propagation.REQUIRED) // readOnly = false 가 기본값입니다.
    public void resetUserPointAndVersion(String userId) {
        log.info("resetUserPointAndVersion 실행: 사용자 ID {}", userId);
        userRepository.findById(userId) // PESSIMISTIC_WRITE 락이 여기서 걸림
                .ifPresentOrElse(user -> {
                    user.setPoint(0L);
                    user.setVersion(0L);
                    userRepository.save(user); // 변경 사항 저장 (트랜잭션 커밋 시 DB에 반영)
                    log.info("사용자 {}의 포인트와 버전이 초기화되었습니다. 최종 포인트: {}", userId, user.getPoint());
                }, () -> {
                    log.warn("사용자 {}를 찾을 수 없어 포인트를 초기화하지 못했습니다.", userId);
                });
    }
	
}
