package com.sparta.tdd.coffeeshop.domain.menu.service;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sparta.tdd.coffeeshop.cmmn.exception.CustomException;
import com.sparta.tdd.coffeeshop.cmmn.exception.ErrorCode;
import com.sparta.tdd.coffeeshop.domain.menu.Menu;
import com.sparta.tdd.coffeeshop.domain.menu.dto.MenuResponse;
import com.sparta.tdd.coffeeshop.domain.menu.dto.PopularMenuResponse;
import com.sparta.tdd.coffeeshop.domain.menu.repo.MenuRepository;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // final 필드를 사용하는 생성자를 자동으로 생성
@Transactional(readOnly = true) // 읽기 전용 트랜잭션으로 설정 (성능 최적화)
public class MenuService {

    private final MenuRepository menuRepository;    
    
    // initMenuData() 메서드 추가
    @Transactional // 데이터 변경이 발생하므로 @Transactional 어노테이션 필요
    public void initMenuData() {
        if (menuRepository.count() == 0) { // 메뉴가 없을 때만 초기화
            menuRepository.save(new Menu(1L, "아메리카노", 3000));
            menuRepository.save(new Menu(2L, "카페라떼", 4000));
            menuRepository.save(new Menu(3L, "카푸치노", 4000));
            menuRepository.save(new Menu(4L, "바닐라라떼", 4500));
            menuRepository.save(new Menu(5L, "에스프레소", 2500));
        }
    }


    // 메뉴 전체 조회 API
    public List<MenuResponse> getAllMenus() {
        // Repository에서 모든 Menu 엔티티를 조회하고, MenuResponse DTO로 변환하여 반환
    	List<Menu> menus = menuRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        return menus.stream()
                    .map(MenuResponse::from)
                    .collect(Collectors.toList());
    }
    
    public MenuResponse getMenuById(Long id) {
        // menuRepository.findById(id)는 Optional<Menu>를 반환합니다.
        // orElseThrow를 사용하여 메뉴가 존재하지 않을 경우 예외를 발생시킵니다.
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND)); // Assuming you have MENU_NOT_FOUND in your ErrorCode enum

        return MenuResponse.from(menu); // Menu 엔티티를 MenuResponse DTO로 변환하여 반환
    }
    
    // 인기 메뉴 조회 API
    public List<PopularMenuResponse> getPopularMenus() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        // 이 리포지토리 호출은 PageRequest.of(0, 3)으로 인해
        // 이미 상위 3개만 반환하도록 설정되어 있어야 합니다.
        // 만약 리포지토리 쿼리가 더 많이 반환한다면, 쿼리 자체가 Pageable을 올바르게 사용하지 않는 것입니다.
        List<PopularMenuResponse.PopularMenuProjection> projections =
        		menuRepository.findPopularMenuProjectionsInLast7Days(
                        sevenDaysAgo,
                        PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "orderCount"))
                );

        return projections.stream()
                .map(PopularMenuResponse::from)
                .collect(Collectors.toList());
    }
}
