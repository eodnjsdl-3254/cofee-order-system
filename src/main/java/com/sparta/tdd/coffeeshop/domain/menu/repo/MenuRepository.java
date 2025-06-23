package com.sparta.tdd.coffeeshop.domain.menu.repo;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sparta.tdd.coffeeshop.domain.menu.Menu;
import com.sparta.tdd.coffeeshop.domain.menu.dto.PopularMenuResponse;

// 이 인터페이스는 JpaRepository를 상속받아 DB 접근 메서드를 제공할 것 (지금은 Mocking으로 충분)
// 실제 구현 시 extends JpaRepository<Menu, Long>
public interface MenuRepository extends JpaRepository<Menu, Long>{	
	
	
	@Query(value = "SELECT o.menuId AS id, m.name AS menuName, m.price AS price, COUNT(o.menuId) AS orderCount " +
            "FROM Order o JOIN Menu m ON o.menuId = m.id " +
            "WHERE o.orderDate >= :sevenDaysAgo " +
            "GROUP BY o.menuId, m.name, m.price " +
            "ORDER BY COUNT(o.menuId) DESC, o.menuId ASC")
	List<PopularMenuResponse.PopularMenuProjection> findPopularMenuProjectionsInLast7Days(
	     @Param("sevenDaysAgo") LocalDateTime sevenDaysAgo,
	     Pageable pageable
	);
}
