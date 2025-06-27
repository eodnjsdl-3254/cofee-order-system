package com.sparta.tdd.coffeeshop.controller.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sparta.tdd.coffeeshop.domain.order.dto.OrderRequest;
import com.sparta.tdd.coffeeshop.domain.order.dto.OrderResponse;
import com.sparta.tdd.coffeeshop.domain.order.service.OrderService;

@RestController // 이 어노테이션이 있어야 컨트롤러로 인식됩니다.
@RequestMapping("/api") // 요청 경로의 시작점을 정의합니다.
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/orders") // POST 요청, 
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody OrderRequest request) {
        OrderResponse response = orderService.placeOrder(request);
        return ResponseEntity.ok(response);
    }
}