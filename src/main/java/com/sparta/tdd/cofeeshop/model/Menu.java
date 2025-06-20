package com.sparta.tdd.cofeeshop.model;

//import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
//import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
//import lombok.Setter;


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


/*
 * @Entity
 * 
 * @Getter
 * 
 * @Setter
 * 
 * @NoArgsConstructor
 * 
 * @AllArgsConstructor public class Menu {
 * 
 * @Id private String menuId; private String name; private long price;
 * 
 * 
 * // 테스트를 위해 Getter 필요 (Lombok 사용 예정이므로 실제 코드에는 @Getter 붙일 것) public String
 * getMenuId() { return menuId; } public String getName() { return name; }
 * public long getPrice() { return price; }
 * 
 * @Override public boolean equals(Object o) { // 테스트의 assertEquals를 위해
 * equals/hashCode 오버라이드 (Lombok @EqualsAndHashCode 대체) if (this == o) return
 * true; if (o == null || getClass() != o.getClass()) return false; Menu menu =
 * (Menu) o; return price == menu.price && menuId.equals(menu.menuId) &&
 * name.equals(menu.name); }
 * 
 * @Override public int hashCode() { return Objects.hash(menuId, name, price); }
 * }
 */