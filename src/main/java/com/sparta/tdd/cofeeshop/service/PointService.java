package com.sparta.tdd.cofeeshop.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sparta.tdd.cofeeshop.exception.CustomException;
import com.sparta.tdd.cofeeshop.exception.ErrorCode;
import com.sparta.tdd.cofeeshop.model.User;
import com.sparta.tdd.cofeeshop.repository.UserRepository;
import com.sparta.tdd.cofeeshop.util.PointChargeResponse;


@Service
@RequiredArgsConstructor
public class PointService {

    private final UserRepository userRepository;

    @Transactional // 포인트 충전은 데이터 변경이므로 @Transactional 필수
    public PointChargeResponse chargePoint(String userId, long amount) {
        // 1. 금액 유효성 검증 (User 조회보다 먼저 수행하여 불필요한 DB 접근 방지)
        if (amount <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "충전 금액은 0보다 커야 합니다.");
        }

        // 2. 사용자 조회 (UserRepository에 @Lock(PESSIMISTIC_WRITE)가 적용되어 락이 걸릴 것임)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 3. 포인트 충전 (User 엔티티의 비즈니스 로직 호출)
        user.chargePoint(amount); // User 엔티티 내부에서 포인트 증가
        userRepository.save(user); // 변경된 User 엔티티 저장 (JPA dirty checking으로 자동 저장되지만 명시적으로)

        // 4. 응답 DTO 생성 및 반환
        return PointChargeResponse.from(user);
    }
	
}
