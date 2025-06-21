package com.sparta.tdd.coffeeshop.domain.user.dto;

import com.sparta.tdd.coffeeshop.domain.user.User;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PointChargeResponse {
	private String userId;
    private long currentPoint;

    public static PointChargeResponse from(User user) {
        return PointChargeResponse.builder()
                .userId(user.getUserId())
                .currentPoint(user.getPoint())
                .build();
    }
}
