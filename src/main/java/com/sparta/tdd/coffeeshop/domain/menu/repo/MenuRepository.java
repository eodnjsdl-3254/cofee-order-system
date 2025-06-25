package com.sparta.tdd.coffeeshop.domain.menu.repo;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sparta.tdd.coffeeshop.domain.menu.Menu;
import com.sparta.tdd.coffeeshop.domain.menu.dto.PopularMenuResponse.PopularMenuProjection;

// 이 인터페이스는 JpaRepository를 상속받아 DB 접근 메서드를 제공할 것 (지금은 Mocking으로 충분)
// 실제 구현 시 extends JpaRepository<Menu, Long>
public interface MenuRepository extends JpaRepository<Menu, Long>{	
	
	/**
	 * 최근 7일간 가장 인기 있는 메뉴 상위 N개를 조회합니다.
	 * 메뉴별 주문 횟수, 이름, 가격을 PopularMenuProjection 형태로 반환합니다.
	 *
	 * @param sevenDaysAgo 현재로부터 7일 전의 기준 시간
	 * @param pageable 상위 N개 (예: 3개)를 제한하기 위한 Pageable 객체
	 * @return PopularMenuProjection 객체의 리스트
	 */
	@Query(value = "SELECT m.id AS id, m.name AS menuName, m.price AS price, COUNT(o.order_id) AS orderCount " + // SQL 컬럼명 사용
            "FROM orders o JOIN menu m ON o.menu_id = m.id " + // 실제 테이블명과 컬럼명 사용
            "WHERE o.order_date >= :sevenDaysAgo " +
            "GROUP BY m.id, m.name, m.price " +
            "ORDER BY COUNT(o.order_id) DESC, m.id ASC", nativeQuery = true)  // JPQL 대신 네이티브 SQL 쿼리 사용
	List<PopularMenuProjection> findPopularMenuProjectionsInLast7Days(
	     @Param("sevenDaysAgo") LocalDateTime sevenDaysAgo,
	     Pageable pageable
	);
}
