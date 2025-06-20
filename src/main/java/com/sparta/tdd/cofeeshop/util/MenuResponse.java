package com.sparta.tdd.cofeeshop.util;

import com.sparta.tdd.cofeeshop.model.Menu;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder // Builder 패턴으로 객체 생성
public class MenuResponse {
    private Long id;
    private String name;
    private int price;

    // Menu 엔티티로부터 MenuResponse 객체를 생성하는 팩토리 메서드
    public static MenuResponse from(Menu menu) {
        return MenuResponse.builder()
                .id(menu.getId())
                .name(menu.getName())
                .price(menu.getPrice())
                .build();
    }
}