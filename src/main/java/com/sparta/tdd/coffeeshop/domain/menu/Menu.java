package com.sparta.tdd.coffeeshop.domain.menu;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;



@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA는 기본 생성자를 필요로 합니다.
@AllArgsConstructor // 모든 필드 포함 생성자 (Lombok이 자동으로 생성)
@Builder // 빌더 패턴 활성화
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ID 자동 생성 전략
    private Long id;
    @Column(name = "name")
    private String name;
    @Column(nullable = false)
    private int price;

    public Menu(String name, int price) {    	
        this.name = name;
        this.price = price;
    }
}
