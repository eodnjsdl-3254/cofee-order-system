package com.sparta.tdd.coffeeshop.domain.menu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Logback 사용을 위한 Slf4j 임포트

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors; // Collectors 임포트

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort; // Sort 임포트
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sparta.tdd.coffeeshop.cmmn.exception.CustomException;
import com.sparta.tdd.coffeeshop.cmmn.exception.ErrorCode;
import com.sparta.tdd.coffeeshop.domain.menu.Menu;
import com.sparta.tdd.coffeeshop.domain.menu.dto.MenuResponse;
import com.sparta.tdd.coffeeshop.domain.menu.dto.PopularMenuResponse;
import com.sparta.tdd.coffeeshop.domain.menu.repo.MenuRepository;

@Service
@RequiredArgsConstructor // final 필드를 사용하는 생성자를 자동으로 생성
@Transactional(readOnly = true) // 읽기 전용 트랜잭션으로 설정 (성능 최적화)
@Slf4j // Lombok을 이용한 로그 객체 자동 생성
public class MenuService {

    private final MenuRepository menuRepository;    
    
    /**
     * 초기 메뉴 데이터를 DB에 삽입합니다.
     * 메뉴가 없을 때만 실행되어 중복 삽입을 방지합니다.
     */
    @Transactional // 데이터 변경이 발생하므로 @Transactional 어노테이션 필요
    public void initMenuData() {
        if (menuRepository.count() == 0) { // 메뉴가 없을 때만 초기화
            log.info("초기 메뉴 데이터 삽입을 시작합니다.");
            menuRepository.save(new Menu("아메리카노", 3000));
            menuRepository.save(new Menu("카페라떼", 4000));
            menuRepository.save(new Menu("카푸치노", 4000));
            menuRepository.save(new Menu("바닐라라떼", 4500));
            menuRepository.save(new Menu("에스프레소", 2500));
            log.info("초기 메뉴 데이터 삽입 완료.");
        } else {
            log.info("메뉴 데이터가 이미 존재하여 초기화 작업을 건너뜜니다.");
        }
    }


    /**
     * 모든 메뉴 목록을 조회합니다.
     * @return 메뉴 응답 DTO 목록
     */
    public List<MenuResponse> getAllMenus() {
        log.info("모든 메뉴 조회 서비스 시작.");
        List<Menu> menus = menuRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        return menus.stream()
                    .map(MenuResponse::from) // MenuResponse DTO의 from 팩토리 메서드 사용
                    .collect(Collectors.toList());
    }
    
    /**
     * 특정 ID를 가진 메뉴 엔티티를 조회합니다.
     * ConcurrentTestController에서 메뉴 가격 조회를 위해 직접 Menu 엔티티를 반환합니다.
     *
     * @param id 조회할 메뉴의 ID
     * @return 조회된 MenuResponse DTO
     * @throws CustomException 메뉴를 찾을 수 없을 경우 (ErrorCode.MENU_NOT_FOUND)
     */
    public MenuResponse getMenuById(Long id) { // ❗ 반환 타입을 MenuResponse에서 Menu 엔티티로 변경
        log.info("메뉴 ID로 조회 서비스 시작: menuId={}", id);
        // menuRepository를 사용하여 ID로 메뉴를 찾습니다.
        // Optional이 비어있으면 CustomException을 발생시킵니다.
        MenuResponse menu = menuRepository.findById(id)    
        		.map(MenuResponse::from) // 찾은 Menu 엔티티를 MenuResponse 팩토리 메서드를 사용하여 DTO로 변환
                .orElseThrow(() -> {
                    log.error("메뉴를 찾을 수 없음: menuId={}", id);
                    return new CustomException(ErrorCode.MENU_NOT_FOUND, "메뉴를 찾을 수 없습니다.");
                });
        return menu; 
    }
    
    /**
     * 최근 7일간 가장 인기 있는 메뉴 상위 3개를 조회합니다.
     * @return 인기 메뉴 목록 (PopularMenuResponse 리스트)
     */
    public List<PopularMenuResponse> getPopularMenus() {
        log.info("인기 메뉴 조회 요청 시작.");
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        // MenuRepository의 JPQL 쿼리에서 이미 정렬을 처리하므로, PageRequest에 Sort를 명시할 필요 없습니다.
        List<PopularMenuResponse.PopularMenuProjection> projections =
        		menuRepository.findPopularMenuProjectionsInLast7Days(
                        sevenDaysAgo,
                        PageRequest.of(0, 3) 
                );

        if (projections.isEmpty()) {
            log.info("최근 7일간 주문된 인기 메뉴가 없습니다.");
            return List.of(); // 빈 리스트 반환
        }

        return projections.stream()
                .map(PopularMenuResponse::from)
                .collect(Collectors.toList());
    }
}