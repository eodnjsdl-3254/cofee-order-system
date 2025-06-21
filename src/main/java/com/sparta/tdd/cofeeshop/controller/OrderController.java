package com.sparta.tdd.cofeeshop.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sparta.tdd.cofeeshop.controller.dto.OrderRequest;
import com.sparta.tdd.cofeeshop.service.OrderService;
import com.sparta.tdd.cofeeshop.util.OrderResponse;

@RestController // ⭐ 이 어노테이션이 있어야 컨트롤러로 인식됩니다.
@RequestMapping("/api") // ⭐ 요청 경로의 시작점을 정의합니다.
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/orders") // ⭐ POST 요청, 
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody OrderRequest request) {
        OrderResponse response = orderService.placeOrder(request);
        return ResponseEntity.ok(response);
    }
}