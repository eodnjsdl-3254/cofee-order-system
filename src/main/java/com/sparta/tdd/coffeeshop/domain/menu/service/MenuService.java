package com.sparta.tdd.coffeeshop.domain.menu.service;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sparta.tdd.coffeeshop.domain.menu.Menu;
import com.sparta.tdd.coffeeshop.domain.menu.dto.MenuResponse;
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
            menuRepository.save(new Menu("아메리카노", 3000));
            menuRepository.save(new Menu("카페라떼", 4000));
            menuRepository.save(new Menu("카푸치노", 4000));
            menuRepository.save(new Menu("바닐라라떼", 4500));
            menuRepository.save(new Menu("에스프레소", 2500));
        }
    }


    public List<MenuResponse> getAllMenus() {
        // Repository에서 모든 Menu 엔티티를 조회하고, MenuResponse DTO로 변환하여 반환
        return menuRepository.findAll().stream()
                .map(MenuResponse::from)
                .collect(Collectors.toList());
    }
}
