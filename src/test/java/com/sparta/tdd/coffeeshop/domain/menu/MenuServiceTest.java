package com.sparta.tdd.coffeeshop.domain.menu;


import static org.junit.jupiter.api.Assertions.assertAll;



import static org.assertj.core.api.Assertions.assertThat; // assertThat() 메서드를 사용하기 위해 필요

import static org.mockito.BDDMockito.given; // given() 메서드를 사용하기 위해 필요
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.sparta.tdd.coffeeshop.domain.menu.dto.MenuResponse;
import com.sparta.tdd.coffeeshop.domain.menu.dto.PopularMenuResponse;
import com.sparta.tdd.coffeeshop.domain.menu.repo.MenuRepository;
import com.sparta.tdd.coffeeshop.domain.menu.service.MenuService;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class) // Mockito 어노테이션 활성화를 위한 JUnit 5 확장
class MenuServiceTest {

    @Mock // MenuRepository를 Mock 객체로 만듭니다. 실제 DB와 상호작용하지 않고 가짜 객체를 사용합니다.
    private MenuRepository menuRepository;

    @InjectMocks // MenuService 객체를 생성하고, @Mock으로 만든 menuRepository를 주입합니다.
    private MenuService menuService;
    

    // --- getAllMenus() 테스트 케이스 ---

    @Test
    @DisplayName("메뉴 목록 조회: 여러 개의 메뉴가 있을 때 모든 메뉴를 MenuResponse DTO로 변환하여 반환한다.")
    void getAllMenus_ShouldReturnAllMenusAsDtoWhenMenusExist() {
        // Given (준비)
        // menuRepository.findAll()이 호출될 때 반환할 가짜 Menu 엔티티 리스트를 정의합니다.
    	List<Menu> mockMenus = new ArrayList<>();
        mockMenus.add(new Menu(1L, "TestMenu1", 1000));
        mockMenus.add(new Menu(2L, "TestMenu2", 2000));

        // findAll()에서 findAll(any(Sort.class))로 변경
        when(menuRepository.findAll(any(Sort.class))).thenReturn(mockMenus); // <<<<<< 수정됨

        // When
        List<MenuResponse> result = menuService.getAllMenus();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMenuName()).isEqualTo("TestMenu1");
        assertThat(result.get(1).getMenuName()).isEqualTo("TestMenu2");
    }

    @Test
    @DisplayName("메뉴 목록 조회: 메뉴가 하나도 없을 때 빈 리스트를 반환한다.")
    void getAllMenus_ShouldReturnEmptyListWhenNoMenusExist() {
        // Given (준비)
        // findAll()에서 findAll(any(Sort.class))로 변경
        when(menuRepository.findAll(any(Sort.class))).thenReturn(Collections.emptyList()); // <<<<<< 수정됨

        // When
        List<MenuResponse> result = menuService.getAllMenus();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

    }
    
    // --- initMenuData() 테스트 케이스 ---
    @Test
    @DisplayName("초기 메뉴 데이터 초기화: 데이터베이스에 메뉴가 없을 때 5개의 초기 메뉴를 저장한다.")
    void initMenuData_ShouldSaveInitialMenusWhenNoMenusExist() {
        // Given
        given(menuRepository.count()).willReturn(0L);

        // When
        menuService.initMenuData();

        // Then
        verify(menuRepository, times(1)).count();
        verify(menuRepository, times(5)).save(any(Menu.class)); // 5개의 메뉴가 저장되었는지 확인
    }

    @Test
    @DisplayName("초기 메뉴 데이터 초기화: 데이터베이스에 메뉴가 이미 존재할 때 새로운 메뉴를 저장하지 않는다.")
    void initMenuData_ShouldNotSaveNewMenusWhenMenusAlreadyExist() {
        // Given
        given(menuRepository.count()).willReturn(1L); // 이미 메뉴가 1개 있다고 가정

        // When
        menuService.initMenuData();

        // Then
        verify(menuRepository, times(1)).count();
        verify(menuRepository, never()).save(any(Menu.class)); // save 메서드는 호출되지 않아야 함
    }

    // --- 새로운 getPopularMenus() 테스트 ---

    @Test
    @DisplayName("getPopularMenus - 주문 횟수 기준 상위 3개 메뉴를 반환해야 함")
    void getPopularMenus_ShouldReturnTop3MenusByOrderCount() {
        // Given
        List<PopularMenuResponse.PopularMenuProjection> mockProjections = new ArrayList<>();
        // 서비스/리포지토리가 올바르게 제한하는지 확인하기 위해 3개 이상의 Mock 데이터 생성
        mockProjections.add(new PopularMenuProjectionImpl(1L, "Latte", 5000, 10L));
        mockProjections.add(new PopularMenuProjectionImpl(2L, "Americano", 4000, 9L));
        mockProjections.add(new PopularMenuProjectionImpl(3L, "Espresso", 3000, 8L));
        mockProjections.add(new PopularMenuProjectionImpl(4L, "Cappuccino", 5500, 7L)); // 이 항목은 필터링되어야 함

        // 리포지토리 호출을 Mock하여 Pageable이 실제 반환할 *첫 3개* 요소를 반환하도록 합니다.
        // Pageable이 사용될 때 리포지토리 자체가 제한을 처리합니다.
        // 따라서 Mock할 때는 실제 리포지토리가 *반환할* 내용을 반영해야 합니다.
        // 실제 쿼리가 Pageable 때문에 3개를 반환해야 한다면, Mock도 3개를 반환하도록 합니다.
        // 리포지토리가 Pageable 요청을 정확히 반환한다고 가정합시다.
        when(menuRepository.findPopularMenuProjectionsInLast7Days(any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(mockProjections.subList(0, 3)); // <<<<<<<<<<<<<<< 여기가 핵심 변경 사항

        // When
        List<PopularMenuResponse> result = menuService.getPopularMenus();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3); // 정확히 3개를 기대
        assertThat(result.get(0).getMenuName()).isEqualTo("Latte");
        assertThat(result.get(1).getMenuName()).isEqualTo("Americano");
        assertThat(result.get(2).getMenuName()).isEqualTo("Espresso");
    }

    // Projection 인터페이스 Mocking을 위한 헬퍼 클래스
    static class PopularMenuProjectionImpl implements PopularMenuResponse.PopularMenuProjection {
        private long id;
        private String menuName;
        private int price;
        private Long orderCount;

        public PopularMenuProjectionImpl(long id, String menuName, int price, Long orderCount) {
            this.id = id;
            this.menuName = menuName;
            this.price = price;
            this.orderCount = orderCount;
        }

        @Override public Long getId() { return id; }
        @Override public String getMenuName() { return menuName; }
        @Override public Integer getPrice() { return price; }
        @Override public Long getOrderCount() { return orderCount; }
    }

    @Test
    @DisplayName("인기 메뉴 조회: OrderRepository에서 빈 리스트를 반환할 때 MenuService도 빈 리스트를 반환한다.")
    void getPopularMenus_ShouldReturnEmptyListWhenNoPopularMenus() {
        // Given
        given(menuRepository.findPopularMenuProjectionsInLast7Days(any(LocalDateTime.class), any(Pageable.class)))
                .willReturn(Collections.emptyList()); // 빈 리스트 반환하도록 스터빙

        // When
        List<PopularMenuResponse> result = menuService.getPopularMenus();

        // Then
        verify(menuRepository, times(1)).findPopularMenuProjectionsInLast7Days(any(LocalDateTime.class), any(Pageable.class));
        assertThat(result).isEmpty(); // 결과가 빈 리스트인지 확인
    }

    @Test
    @DisplayName("인기 메뉴 조회: 인기 메뉴가 3개 미만일 경우 존재하는 메뉴만 반환한다.")
    void getPopularMenus_ShouldReturnFewerThan3MenusIfAvailable() {
        // Given (인기 메뉴가 2개인 시나리오)
        List<PopularMenuResponse.PopularMenuProjection> mockProjections = Arrays.asList(
                new PopularMenuResponse.PopularMenuProjection() {
                    @Override public Long getId() { return 1L; }
                    @Override public String getMenuName() { return "아메리카노"; }
                    @Override public Integer getPrice() { return 4000; }
                    @Override public Long getOrderCount() { return 10L; }
                },
                new PopularMenuResponse.PopularMenuProjection() {
                    @Override public Long getId() { return 2L; }
                    @Override public String getMenuName() { return "카페 라떼"; }
                    @Override public Integer getPrice() { return 4500; }
                    @Override public Long getOrderCount() { return 5L; }
                }
        );
        given(menuRepository.findPopularMenuProjectionsInLast7Days(any(LocalDateTime.class), any(Pageable.class)))
                .willReturn(mockProjections);

        // When
        List<PopularMenuResponse> result = menuService.getPopularMenus();

        // Then
        verify(menuRepository, times(1)).findPopularMenuProjectionsInLast7Days(any(LocalDateTime.class), any(Pageable.class));
        assertThat(result).hasSize(2); // 2개만 반환되었는지 확인

        assertAll(
                () -> assertThat(result.get(0).getId()).isEqualTo(1L),
                () -> assertThat(result.get(0).getMenuName()).isEqualTo("아메리카노"),
                () -> assertThat(result.get(0).getOrderCount()).isEqualTo(10L),
                () -> assertThat(result.get(1).getId()).isEqualTo(2L),
                () -> assertThat(result.get(1).getMenuName()).isEqualTo("카페 라떼"),
                () -> assertThat(result.get(1).getOrderCount()).isEqualTo(5L)
        );
    }
}
