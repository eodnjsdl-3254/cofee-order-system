package com.sparta.tdd.coffeeshop.domain.order.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sparta.tdd.coffeeshop.domain.order.Order;


// 실제 구현 시 extends JpaRepository<Order, String>
@Repository
public interface OrderRepository extends JpaRepository<Order, String>{ // <-- String으로 변경!
    // JpaRepository가 기본 CRUD 메서드를 제공합니다 (save, findById, findAll 등)	
}
