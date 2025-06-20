package com.sparta.tdd.cofeeshop.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sparta.tdd.cofeeshop.model.Menu;

import java.util.List;
import java.util.Optional;

// 이 인터페이스는 JpaRepository를 상속받아 DB 접근 메서드를 제공할 것 (지금은 Mocking으로 충분)
// 실제 구현 시 extends JpaRepository<Menu, String>
public interface MenuRepository extends JpaRepository<Menu, Long>{	
    // JpaRepository provides common CRUD operations like save(), findById(), findAll(), delete(), etc.
    // You don't need to declare save() here explicitly.
}
