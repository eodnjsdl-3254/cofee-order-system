package com.sparta.tdd.coffeeshop.domain.menu.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sparta.tdd.coffeeshop.domain.menu.Menu;

// 이 인터페이스는 JpaRepository를 상속받아 DB 접근 메서드를 제공할 것 (지금은 Mocking으로 충분)
// 실제 구현 시 extends JpaRepository<Menu, Long>
public interface MenuRepository extends JpaRepository<Menu, Long>{	
}
