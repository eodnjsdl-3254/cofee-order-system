package com.sparta.tdd.cofeeshop.model;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;

import lombok.Getter;
import lombok.NoArgsConstructor;



@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA는 기본 생성자를 필요로 합니다.
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ID 자동 생성 전략
    private Long id;
    private String name;
    private int price;

    public Menu(String name, int price) {    	
        this.name = name;
        this.price = price;
    }
    
    public Menu(Long id, String name, int price) {
    	this.id = id;
        this.name = name;
        this.price = price;
    }
}
