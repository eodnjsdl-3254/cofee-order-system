package com.sparta.tdd.cofeeshop.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor; // Lombok 사용 예정
import lombok.Getter;
import lombok.NoArgsConstructor;
//import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PointChargeRequest {
	@NotBlank(message = "사용자 ID는 필수입니다.") // null, 빈 문자열, 공백만 있는 문자열 불가
    private String userId;

    @NotNull(message = "충전 금액은 필수입니다.") // null 불가
    @Min(value = 1, message = "충전 금액은 1원 이상이어야 합니다.") // 최소 1
    private long amount;
}