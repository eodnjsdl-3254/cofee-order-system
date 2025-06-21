package com.sparta.tdd.test.coffeeshop.service;


import static org.junit.jupiter.api.Assertions.assertAll;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat; // assertThat() 메서드를 사용하기 위해 필요

import static org.mockito.BDDMockito.given; // given() 메서드를 사용하기 위해 필요
//import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

//import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sparta.tdd.coffeeshop.domain.menu.Menu;
import com.sparta.tdd.coffeeshop.domain.menu.dto.MenuResponse;
import com.sparta.tdd.coffeeshop.domain.menu.repo.MenuRepository;
import com.sparta.tdd.coffeeshop.domain.menu.service.MenuService;

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
        Menu americano = new Menu("아메리카노", 3000);
        Menu latte = new Menu("카페라떼", 4000);
        List<Menu> mockMenus = Arrays.asList(americano, latte);

        given(menuRepository.findAll()).willReturn(mockMenus); // Mockito를 사용하여 findAll() 호출 시 mockMenus 반환 설정

        // When (실행)
        List<MenuResponse> result = menuService.getAllMenus();

        // Then (검증)
        // 1. menuRepository.findAll()이 한 번 호출되었는지 확인
        verify(menuRepository, times(1)).findAll();

        // 2. 반환된 리스트의 크기가 예상과 일치하는지 확인
        assertThat(result).hasSize(2);

        // 3. 반환된 DTO의 내용이 정확한지 확인
        assertAll(
                () -> assertThat(result.get(0).getName()).isEqualTo("아메리카노"),
                () -> assertThat(result.get(0).getPrice()).isEqualTo(3000),
                () -> assertThat(result.get(1).getName()).isEqualTo("카페라떼"),
                () -> assertThat(result.get(1).getPrice()).isEqualTo(4000)
        );
    }

    @Test
    @DisplayName("메뉴 목록 조회: 메뉴가 하나도 없을 때 빈 리스트를 반환한다.")
    void getAllMenus_ShouldReturnEmptyListWhenNoMenusExist() {
        // Given (준비)
        given(menuRepository.findAll()).willReturn(Collections.emptyList()); // 빈 리스트 반환 설정

        // When (실행)
        List<MenuResponse> result = menuService.getAllMenus();

        // Then (검증)
        verify(menuRepository, times(1)).findAll(); // findAll()이 한 번 호출되었는지 확인
        assertThat(result).isEmpty(); // 반환된 리스트가 비어있는지 확인
    }

    // --- initMenuData() 테스트 케이스 ---

    @Test
    @DisplayName("초기 메뉴 데이터 초기화: 데이터베이스에 메뉴가 없을 때 5개의 초기 메뉴를 저장한다.")
    void initMenuData_ShouldSaveInitialMenusWhenNoMenusExist() {
        // Given (준비)
        given(menuRepository.count()).willReturn(0L); // menuRepository.count()가 0을 반환하도록 설정

        // When (실행)
        menuService.initMenuData();

        // Then (검증)
        verify(menuRepository, times(1)).count(); // count() 메서드가 한 번 호출되었는지 확인
        // save() 메서드가 총 5번 호출되었고, 각 호출에 Menu.class 타입의 어떤 객체든 인자로 전달되었는지 확인
        verify(menuRepository, times(5)).save(org.mockito.ArgumentMatchers.any(Menu.class));
    }

    @Test
    @DisplayName("초기 메뉴 데이터 초기화: 데이터베이스에 메뉴가 이미 존재할 때 새로운 메뉴를 저장하지 않는다.")
    void initMenuData_ShouldNotSaveNewMenusWhenMenusAlreadyExist() {
        // Given (준비)
        given(menuRepository.count()).willReturn(1L); // menuRepository.count()가 1 (0이 아닌 값)을 반환하도록 설정

        // When (실행)
        menuService.initMenuData();

        // Then (검증)
        verify(menuRepository, times(1)).count(); // count() 메서드가 한 번 호출되었는지 확인
        // save() 메서드가 전혀 호출되지 않았는지 확인
        verify(menuRepository, never()).save(org.mockito.ArgumentMatchers.any(Menu.class));
    }
}
