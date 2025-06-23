package com.sparta.tdd.test.coffeeshop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.tdd.coffeeshop.domain.menu.Menu;
import com.sparta.tdd.coffeeshop.domain.menu.repo.MenuRepository;
import com.sparta.tdd.coffeeshop.domain.order.Order;
import com.sparta.tdd.coffeeshop.domain.order.dto.OrderRequest;
import com.sparta.tdd.coffeeshop.domain.user.User;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat; // 추가

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // 각 테스트 후 DB 롤백
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MenuRepository menuRepository; // 메뉴를 DB에 저장하기 위해 추가

    private User testUser;
    private Menu testMenu;

    @BeforeEach
    void setUp() {
        // 매 테스트마다 DB 초기화 및 테스트 데이터 설정
        userRepository.deleteAll();
        menuRepository.deleteAll();

        // 테스트 유저 생성 (포인트 충분히)
        testUser = new User("user123", 10000L); // 넉넉한 포인트
        userRepository.save(testUser);

        // 테스트 메뉴 생성
        testMenu = new Menu("아메리카노", 4000);
        menuRepository.save(testMenu);
    }

    @Test
    @DisplayName("POST /api/orders: 유효한 요청으로 커피 주문 및 결제에 성공한다.")
    void createOrder_Success() throws Exception {
        // Given
        String userId = testUser.getUserId();
        Long menuId = testMenu.getId();
        int quantity = 2; // 2개 주문

        // 총 결제 금액 예상: 4000 * 2 = 8000
        long expectedTotalPrice = (long) testMenu.getPrice() * quantity;
        // 예상 남은 포인트: 10000 - 8000 = 2000
        long expectedRemainingPoints = testUser.getPoint() - expectedTotalPrice;

        OrderRequest request = new OrderRequest(userId, menuId, quantity, expectedTotalPrice);

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()) // 200 OK 예상
                .andExpect(jsonPath("$.orderId").exists()) // orderId가 존재하는지 확인
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.menuId").value(menuId))
                .andExpect(jsonPath("$.menuName").value(testMenu.getName())) // 메뉴 이름 검증
                .andExpect(jsonPath("$.quantity").value(quantity))
                .andExpect(jsonPath("$.totalPrice").value(expectedTotalPrice)) // 총 결제 금액 검증
                .andExpect(jsonPath("$.remainingPoints").value(expectedRemainingPoints)) // 남은 포인트 검증
                .andExpect(jsonPath("$.orderDate").exists())
                .andExpect(jsonPath("$.status").value(Order.OrderStatus.COMPLETED.name())); // 주문 상태 검증

        // DB에서 유저 포인트가 실제로 감소했는지 확인
        User updatedUser = userRepository.findById(userId).orElseThrow();
        assertThat(updatedUser.getPoint()).isEqualTo(expectedRemainingPoints);
    }
}