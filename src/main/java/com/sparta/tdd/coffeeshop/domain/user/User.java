package com.sparta.tdd.coffeeshop.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users") // 테이블 이름을 "user" 대신 "users"로 변경
@AllArgsConstructor
public class User {

    @Id // String 타입 ID는 자동 생성이 아니므로 @GeneratedValue 제거
    @Column(name = "user_id") // 컬럼명 명시
    private String userId; // 사용자 식별값은 String 타입으로 명세됨

    @Column(name = "user_name", nullable = false)
    private String userName; // 사용자 이름
    private long point;

    @Version // 낙관적 락을 위한 버전 필드
    @Column(name = "version")
    private Long version;

    public User(String userId, long point) {
        this.userId = userId;
        this.point = point;
        this.userName = userId; // 임시로 userId를 username으로 사용
        this.version = 0L;
    }
    
    @PrePersist
    public void prePersist() {
        if (this.version == null) {
            this.version = 0L; // null이면 0으로 초기화
        }
    }
    
    // 이 메서드는 외부에서의 직접적인 버전 조작을 막으면서, 테스트와 JPA 프레임워크가 필요할 때 사용할 수 있도록 합니다.
    void setVersion(Long version) {
        this.version = version;
    }
    
    // 이 메서드는 주로 관리 목적(예: 테스트 환경 초기화, 특정 사용자 데이터 리셋)으로 사용될 수 있습니다.
    public void resetPointAndVersionState() {
        this.point = 0L;
        this.version = 0L; // 버전도 0으로 명시적 초기화
    }

    // 포인트 충전 로직
    public void chargePoint(long amount) {
        // 비즈니스 로직: 음수 금액 방지는 Service 계층에서 먼저 처리되지만, 도메인에서도 방어 로직을 두는 것이 좋습니다.
        if (amount <= 0) {
            // 여기서는 예외를 던지기보다 Service 계층에서 먼저 처리되도록 합니다.
        	throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");        	 
        }
        this.point += amount;
    }
    
    // 포인트 차감 메서드 추가
    public void deductPoint(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("차감 금액은 0보다 커야 합니다.");
        }
        if (this.point < amount) {
            throw new IllegalArgumentException("잔액이 부족합니다."); // 또는 별도의 CustomException
        }
        this.point -= amount;
    }
}