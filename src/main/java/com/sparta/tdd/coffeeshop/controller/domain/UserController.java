package com.sparta.tdd.coffeeshop.controller.domain;

import com.sparta.tdd.coffeeshop.domain.user.dto.PointChargeRequest;
import com.sparta.tdd.coffeeshop.domain.user.dto.PointChargeResponse;
import com.sparta.tdd.coffeeshop.domain.user.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/points/charge") 
    public ResponseEntity<PointChargeResponse> chargePoint(@RequestBody PointChargeRequest request) {
        PointChargeResponse response = userService.chargePoint(request.getUserId(), request.getAmount());
        return ResponseEntity.ok(response);
    }
}