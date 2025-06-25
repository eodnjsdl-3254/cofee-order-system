package com.sparta.tdd.coffeeshop.controller.domain;

import com.sparta.tdd.coffeeshop.domain.menu.Menu; // Menu 엔티티 임포트 추가
import com.sparta.tdd.coffeeshop.domain.menu.dto.MenuResponse;
import com.sparta.tdd.coffeeshop.domain.menu.dto.PopularMenuResponse;
import com.sparta.tdd.coffeeshop.domain.menu.service.MenuService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "메뉴 API", description = "커피 메뉴 관련 API를 제공합니다.")
@RestController // RESTful API 컨트롤러임을 나타냅니다.
@RequestMapping("/api") // 기본 요청 경로 설정
@RequiredArgsConstructor // final 필드를 사용하는 생성자를 자동으로 생성하여 의존성 주입
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "메뉴 목록 조회", description = "현재 판매 중인 모든 커피 메뉴를 조회합니다.")
    @GetMapping("/menus") // GET /api/menus 요청을 처리합니다.
    public ResponseEntity<List<MenuResponse>> getMenus() {
        List<MenuResponse> menus = menuService.getAllMenus();
        return ResponseEntity.ok(menus); // 200 OK 상태 코드와 함께 메뉴 목록 반환
    }
    
    @Operation(summary = "단일 메뉴 조회", description = "메뉴 ID를 통해 특정 커피 메뉴의 상세 정보를 조회합니다.")
    @GetMapping("/menus/{id}")
    public ResponseEntity<Menu> getMenuById(@PathVariable Long id) { // ❗ 반환 타입을 MenuResponse에서 Menu 엔티티로 변경
        Menu menu = menuService.getMenuById(id); // MenuService는 Menu 엔티티를 반환하도록 이미 수정됨
        return ResponseEntity.ok(menu); 
    }
    
    @Operation(summary = "인기 메뉴 목록 조회", description = "최근 7일간 가장 많이 주문된 커피 메뉴 3개를 조회합니다.")
    @GetMapping("/menus/popular")
    public ResponseEntity<List<PopularMenuResponse>> getPopularMenus() {
        List<PopularMenuResponse> popularMenus = menuService.getPopularMenus();
        return ResponseEntity.ok(popularMenus);
    }
}