package com.sparta.tdd.coffeeshop.controller.domain;

import com.sparta.tdd.coffeeshop.domain.menu.dto.MenuResponse;
import com.sparta.tdd.coffeeshop.domain.menu.service.MenuService;

//import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*; // Spring Web 관련 어노테이션 import

import java.util.List; // List 타입 import


import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController // RESTful API 컨트롤러임을 나타냅니다.
@RequestMapping("/api") // 기본 요청 경로 설정
@RequiredArgsConstructor // final 필드를 사용하는 생성자를 자동으로 생성하여 의존성 주입
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/menus") // GET /api/menus 요청을 처리합니다.
    public ResponseEntity<List<MenuResponse>> getMenus() {
        List<MenuResponse> menus = menuService.getAllMenus();
        return ResponseEntity.ok(menus); // 200 OK 상태 코드와 함께 메뉴 목록 반환
    }
}
