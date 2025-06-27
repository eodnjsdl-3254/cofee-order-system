package com.sparta.tdd.coffeeshop.domain.user.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.sparta.tdd.coffeeshop.domain.user.User;

import jakarta.persistence.LockModeType;

import java.util.Optional;


public interface UserRepository extends JpaRepository<User, String> {
	// 비관적 락을 적용한 findById (동시성 문제 해결을 위해 나중에 추가)
	@Lock(LockModeType.PESSIMISTIC_WRITE) // 해당 유저를 잠금
    @Query("SELECT u FROM User u WHERE u.userId = :userId")  
	Optional<User> findByUserIdWithPessimisticLock(String userId);
	
	Optional<User> findById(String id);    
}
