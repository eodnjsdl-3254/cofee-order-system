package com.sparta.tdd.coffeeshop.domain.user.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sparta.tdd.coffeeshop.cmmn.exception.CustomException;
import com.sparta.tdd.coffeeshop.cmmn.exception.ErrorCode;
import com.sparta.tdd.coffeeshop.domain.menu.Menu;
import com.sparta.tdd.coffeeshop.domain.menu.repo.MenuRepository;
import com.sparta.tdd.coffeeshop.domain.order.repo.OrderRepository;
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
    private final OrderRepository orderRepository; // 주문 초기화를 위해 주입
    private final MenuRepository menuRepository; // 메뉴 초기화를 위해 주입
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
    
    /**
     * 동시 주문 테스트를 위해 모든 사용자, 주문, 메뉴 데이터를 초기화하고 기본 사용자와 메뉴를 생성합니다.
     * 이 메서드는 테스트 용도로만 사용되어야 하며, 실제 운영 환경에서는 절대로 호출해서는 안 됩니다.
     */
    @Transactional
    public void resetUsersAndOrdersForConcurrentOrderTest() {
        // 1. 모든 주문 데이터 삭제
        orderRepository.deleteAll();
        // 2. 모든 사용자 데이터 삭제
        userRepository.deleteAll();
        // 3. 모든 메뉴 데이터 삭제 (만약 테스트마다 초기화가 필요하다면)
        // JpaRepository의 deleteAll()은 DELETE 문을 실행하며, MySQL에서 auto_increment를 자동으로 리셋하지 않을 수 있습니다.
        // 테스트 환경에서는 'TRUNCATE TABLE'을 사용하는 것이 가장 확실하나, 여기서는 deleteAll() 유지.
        menuRepository.deleteAll();

        // 4. 테스트를 위한 기본 사용자 재삽입 (초기 포인트 넉넉하게)
        List<User> initialUsers = Arrays.asList(
                new User("testUser1", "테스트 사용자1", 1000000L, 0L), // String, String, long, Long
                new User("testUser2", "테스트 사용자2", 1000000L, 0L),
                new User("user001", "테스트 사용자001", 1000000L, 0L),
                new User("concurrentUser", "동시성테스트용", 1000000L, 0L),
                new User("eodnjsdl", "당신_사용자", 1000000L, 0L)
        );
        userRepository.saveAll(initialUsers);

        // 5. 테스트를 위한 기본 메뉴 재삽입 (ID 1인 아메리카노가 존재하도록)
        List<Menu> initialMenus = Arrays.asList(
            new Menu("아메리카노", 4000), // ID 1
            new Menu("카페 라떼", 4500)   // ID 2
        );
        menuRepository.saveAll(initialMenus);

        // Note: Menu 엔티티의 ID가 @GeneratedValue가 아니라면, 직접 ID를 설정해야 합니다.
        // 위 예시에서는 Menu 엔티티에 @AllArgsConstructor가 있고, ID를 직접 지정할 수 있다고 가정합니다.
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
