package com.sparta.tdd.coffeeshop.domain.menu.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PopularMenuResponse {
	private Long id;
    private String menuName;
    private int price;
    private Long orderCount; // 주문 횟수

    // Projection Interface for JPA Query Results (optional)
    // 인터페이스 기반 Projection을 사용할 경우, 이 DTO 대신 아래 인터페이스를 사용하고,
    // Service에서 이 DTO로 변환하는 팩토리 메서드를 추가할 수 있습니다.
    public interface PopularMenuProjection {
        Long getId();
        String getMenuName(); 
        Integer getPrice();
        Long getOrderCount(); 
    }

    // 팩토리 메서드 (Projection 인터페이스를 통해 DTO 생성 시)
    public static PopularMenuResponse from(PopularMenuProjection projection) {
        return PopularMenuResponse.builder()
                .id(projection.getId())
                .menuName(projection.getMenuName())
                .price(projection.getPrice())
                .orderCount(projection.getOrderCount())
                .build();
    }
}
