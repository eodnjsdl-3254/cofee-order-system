package com.sparta.tdd.cofeeshop.util;

import com.sparta.tdd.cofeeshop.model.User;
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
