package com.sparta.tdd.cofeeshop.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PopularMenuItem {
    private String menuId;
    private String menuName;
    private long price;
    private long orderCount;
}
