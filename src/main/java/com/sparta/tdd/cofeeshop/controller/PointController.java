package com.sparta.tdd.cofeeshop.controller;

import com.sparta.tdd.cofeeshop.service.PointService;
import com.sparta.tdd.cofeeshop.controller.dto.PointChargeRequest;
import com.sparta.tdd.cofeeshop.util.PointChargeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @PostMapping("/charge") // POST /api/points/charge
    public ResponseEntity<PointChargeResponse> chargePoint(@RequestBody PointChargeRequest request) {
        PointChargeResponse response = pointService.chargePoint(request.getUserId(), request.getAmount());
        return ResponseEntity.ok(response);
    }
}