package com.sparta.tdd.coffeeshop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.tdd.coffeeshop.domain.user.User;
import com.sparta.tdd.coffeeshop.domain.user.dto.PointChargeRequest;
import com.sparta.tdd.coffeeshop.domain.user.repo.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post; // POST 요청을 위해 import
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // 각 테스트 후 DB 롤백
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // JSON 직렬화/역직렬화를 위해 필요

    @Autowired
    private UserRepository userRepository; // User 엔티티를 DB에 저장하기 위해 필요

    private User testUser; // 각 테스트마다 사용될 사용자 객체

    @BeforeEach
    void setUp() {
        // 테스트 전에 사용자 데이터 초기화 (매 테스트마다 새 사용자 생성)
        testUser = new User("testUser123", 1000L);
        userRepository.deleteAll();
        userRepository.save(testUser); // 실제 DB에 사용자 저장
    }

    @Test
    @DisplayName("POST /api/user/points/charge: 유효한 금액으로 포인트 충전에 성공한다.")
    void chargePoint_Success() throws Exception {
        // Given
        long chargeAmount = 1000L;
        PointChargeRequest request = new PointChargeRequest(testUser.getUserId(), chargeAmount); // userId 사용

        // When & Then
        mockMvc.perform(post("/api/user/points/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))) // 요청 본문에 JSON 데이터 설정
                .andExpect(status().isOk()) // 200 OK 예상
                .andExpect(jsonPath("$.userId").value(testUser.getUserId()))
                // ⭐ Assertion 수정: 응답으로 받은 currentPoint가 기대하는 값과 일치하는지 확인합니다.
                // 이전에 testUser.getPoint() + chargeAmount를 사용했었는데,
                // 이는 testUser 객체가 실제 DB 업데이트를 반영하지 않기 때문에 잘못된 방식입니다.
                // HTTP 응답 본문에 있는 currentPoint 값을 그대로 사용해야 합니다.
                // DB에서 1000L이 2000L로 업데이트 되었으므로, 응답은 2000L이 될 것입니다.
                // 따라서 기대값은 testUser의 초기값(1000L) + chargeAmount(1000L) 이 되어야 합니다.
		        // 따라서, 초기 포인트 + 충전 금액이 기대값이 됩니다.
		        .andExpect(jsonPath("$.currentPoint").value(1000L + chargeAmount)); // 1000L은 testUser의 초기값
    }

    @Test
    @DisplayName("POST /api/user/points/charge: 음수 금액으로 포인트 충전 시 실패하고 INVALID_INPUT 에러를 반환한다.")
    void chargePoint_NegativeAmount_Failure() throws Exception {
        // Given
        int chargeAmount = -500; // 음수 금액
        PointChargeRequest request = new PointChargeRequest(testUser.getUserId(), chargeAmount);

        // When & Then
        mockMvc.perform(post("/api/user/points/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()) // 400 Bad Request 예상
                .andExpect(jsonPath("$.code").value("INVALID_INPUT")); // 커스텀 에러 코드 검증
    }

    @Test
    @DisplayName("POST /api/user/points/charge: 존재하지 않는 사용자 ID로 포인트 충전 시 실패하고 USER_NOT_FOUND 에러를 반환한다.")
    void chargePoint_UserNotFound_Failure() throws Exception {
        // Given
        String nonExistentUserId = "nonExistent"; // 존재하지 않는 사용자 ID
        long chargeAmount = 1000L;
        PointChargeRequest request = new PointChargeRequest(nonExistentUserId, chargeAmount);

        // When & Then
        mockMvc.perform(post("/api/user/points/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound()) // 404 Not Found 예상
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND")); // 커스텀 에러 코드 검증
    }
}
