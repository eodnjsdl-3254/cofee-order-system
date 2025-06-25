package com.sparta.tdd.coffeeshop.domain.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    @NotBlank(message = "사용자 ID는 필수입니다.")
    private String userId;

    @NotNull(message = "메뉴 ID는 필수입니다.")
    private Long menuId;    
    
    @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
    private int quantity;
    
    @Min(value = 0, message = "총 결제 금액은 0 이상이어야 합니다.")
    private long totalPrice;
}
