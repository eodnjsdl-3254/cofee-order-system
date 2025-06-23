package com.sparta.tdd.test.coffeeshop.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.sparta.tdd.coffeeshop.domain.menu.Menu;
import com.sparta.tdd.coffeeshop.domain.menu.repo.MenuRepository;
import com.sparta.tdd.coffeeshop.domain.order.Order;
import com.sparta.tdd.coffeeshop.domain.order.repo.OrderRepository;

@SpringBootTest // 스프링 컨텍스트 전체를 로드합니다.
@AutoConfigureMockMvc // MockMvc 객체를 자동으로 구성합니다.
@Transactional // 각 테스트 메서드가 끝난 후 트랜잭션을 롤백하여 DB 상태를 초기화합니다.
@ActiveProfiles("test") // 테스트용 프로필 (H2 DB 설정)을 활성화합니다.
public class MenuControllerTest {
	@Autowired
    private MockMvc mockMvc; // HTTP 요청을 시뮬레이션하는 객체

    @Autowired
    private MenuRepository menuRepository; // 실제 DB에 데이터를 넣기 위해 Repository 주입 (통합 테스트이므로 실제 객체 사용)

    @Autowired
    private OrderRepository orderRepository; // 인기 메뉴 테스트를 위해 OrderRepository 주입
    
    // 클래스 레벨에서 사용할 메뉴 ID 필드 선언
    private Long americanoId;
    private Long latteId;
    private Long cappuccinoId;
    private Long espressoId;
    private Long mochaId;
    private Long vanillaLatteId;
    
    // 각 테스트 메서드 실행 전에 호출되어 DB 상태를 초기화하고 공통 메뉴 데이터를 삽입
    @BeforeEach
    void setUp() {
        // 중요한 순서: Order가 Menu를 참조할 수 있으므로, Order 삭제 후 Menu 삭제
        orderRepository.deleteAll(); // 모든 주문 데이터 삭제
        menuRepository.deleteAll(); // 모든 메뉴 데이터 삭제

        // 공통적으로 사용될 메뉴 데이터 미리 삽입.
        // ID를 명시하지 않고, DB가 자동으로 할당하도록 변경
        // Menu 객체를 저장하고, 반환된 객체에서 ID를 추출하여 필드에 할당
        americanoId = menuRepository.save(new Menu("아메리카노", 4000)).getId();
        latteId = menuRepository.save(new Menu("라떼", 5000)).getId();
        cappuccinoId = menuRepository.save(new Menu("카푸치노", 5500)).getId();
        espressoId = menuRepository.save(new Menu("에스프레소", 3000)).getId();
        mochaId = menuRepository.save(new Menu("모카", 6000)).getId();
        vanillaLatteId = menuRepository.save(new Menu("바닐라라떼", 5500)).getId();
    }
    
    @Test
    @DisplayName("GET /api/menus: 모든 메뉴를 성공적으로 조회한다.")
    void getAllMenus_Success() throws Exception {
        // Given (준비): 테스트를 위한 데이터 삽입
        // 초기에는 메뉴가 없다고 가정하고, 이 테스트를 통과시키기 위해 MenuController에서 데이터를 넣어주는 로직이 필요하게 됩니다.
        // 하지만 TDD 관점에서 이 테스트는 "메뉴가 있을 때"를 상정하고 작성합니다.
        // 따라서, 테스트 환경에서 메뉴를 미리 저장해둡니다.
    	mockMvc.perform(get("/api/menus"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(6)); // 총 6개 메뉴를 기대        
    }

    @Test
    @DisplayName("GET /api/menus: 메뉴가 없을 때 빈 배열을 반환한다.")
    void getAllMenus_NoContent() throws Exception {
        // Given (준비): 데이터베이스에 아무 메뉴도 없음 (Transactional로 자동 롤백되므로 별도 처리 불필요)
    	menuRepository.deleteAll(); // 이 테스트만을 위해 메뉴를 비움
        // When (실행) & Then (검증):
        mockMvc.perform(get("/api/menus")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // 메뉴가 없어도 200 OK와 빈 배열을 반환하는 것이 일반적입니다.
                .andExpect(jsonPath("$.length()").value(0)); // 응답 배열의 크기가 0인지 검증
    }
    
    // --- 새로운 인기 메뉴 조회 테스트 ---

    @Test
    @DisplayName("GET /api/menus/popular: 최근 7일간 가장 많이 주문된 상위 3개 메뉴를 정확히 반환한다.")
    void getPopularMenus_Success() throws Exception {
        // Given (준비): 특정 메뉴들의 주문 데이터를 삽입하여 인기 메뉴 시나리오 구성
        // @BeforeEach에서 삽입된 메뉴 ID (1L ~ 5L)를 활용합니다.

    	// @BeforeEach에서 메뉴는 이미 삽입되었으므로, 해당 ID를 사용합니다.
    	
        // 주문 날짜를 현재 시점으로부터 약간 과거로 설정하여 항상 7일 이내에 포함되도록 함
        LocalDateTime now = LocalDateTime.now();
        
    	// 메뉴 1: 라떼 (가장 인기) - 10회 주문
        for (int i = 0; i < 10; i++) {
            orderRepository.save(Order.builder()
                    .userId("user1-" + i)
                    .menuId(latteId)
                    .quantity(1)
                    .totalPrice(5000)
                    .orderDate(now.minus(Duration.ofMinutes(10 + i))) // 현재 시간보다 조금 전으로 설정
                    .status(Order.OrderStatus.COMPLETED)
                    .build());
        }
        // 메뉴 2: 아메리카노 (두 번째 인기) - 8회 주문
        for (int i = 0; i < 8; i++) {
            orderRepository.save(Order.builder()
                    .userId("user2-" + i)
                    .menuId(americanoId)
                    .quantity(1)
                    .totalPrice(4000)
                    .orderDate(now.minus(Duration.ofMinutes(30 + i))) // 현재 시간보다 조금 더 전으로 설정
                    .status(Order.OrderStatus.COMPLETED)
                    .build());
        }
        // 메뉴 3: 카푸치노 (세 번째 인기) - 6회 주문
        for (int i = 0; i < 6; i++) {
            orderRepository.save(Order.builder()
                    .userId("user3-" + i)
                    .menuId(cappuccinoId)
                    .quantity(1)
                    .totalPrice(5500)
                    .orderDate(now.minus(Duration.ofMinutes(50 + i))) // 현재 시간보다 더 전으로 설정
                    .status(Order.OrderStatus.COMPLETED)
                    .build());
        }
        
        // 메뉴 4: 에스프레소 (인기 없음) - 2회 주문 (7일 이내)
        orderRepository.save(Order.builder().userId("userEspresso1").menuId(espressoId).quantity(1).totalPrice(3000).orderDate(now.minusHours(2)).status(Order.OrderStatus.COMPLETED).build());
        orderRepository.save(Order.builder().userId("userEspresso2").menuId(espressoId).quantity(1).totalPrice(3000).orderDate(now.minusHours(3)).status(Order.OrderStatus.COMPLETED).build());


        // When & Then
        mockMvc.perform(get("/api/menus/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                // PopularMenuResponse DTO의 필드명은 'menuName'이므로, 'name' 대신 'menuName'을 사용합니다.
                .andExpect(jsonPath("$[0].menuName").value("라떼"))
                .andExpect(jsonPath("$[1].menuName").value("아메리카노"))
                .andExpect(jsonPath("$[2].menuName").value("카푸치노"));
    }

    @Test
    @DisplayName("GET /api/menus/popular: 최근 7일간 주문이 없을 때 빈 배열을 반환한다.")
    void getPopularMenus_NoRecentOrders() throws Exception {
        // Given (준비): @BeforeEach에서 메뉴는 삽입되지만, 이 테스트를 위해 모든 주문을 삭제
        orderRepository.deleteAll(); // 모든 주문 데이터 삭제

        // When (실행): 인기 메뉴 API 호출
        mockMvc.perform(get("/api/menus/popular")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0)); // 빈 배열 반환 확인
    }

    // getPopularMenus_FewerThan3Menus 테스트도 이와 유사하게 수정:
    @Test
    @DisplayName("GET /api/menus/popular: 최근 7일간 인기 메뉴가 3개 미만일 때 존재하는 메뉴만 반환한다.")
    void getPopularMenus_FewerThan3Menus() throws Exception {
        // Given: 이 테스트에 필요한 메뉴와 주문 데이터만 삽입

        // 테스트용 고정된 날짜 설정
        LocalDateTime now = LocalDateTime.now();

        // 메뉴 1: 라떼 - 5회 주문
        for (int i = 0; i < 5; i++) {
            orderRepository.save(Order.builder()
                    .userId("userA-" + i)
                    .menuId(latteId)
                    .quantity(1)
                    .totalPrice(5000)
                    .orderDate(now.minus(Duration.ofMinutes(5 + i))) // 현재 시간보다 조금 전으로 설정
                    .status(Order.OrderStatus.COMPLETED)
                    .build());
        }
        // 메뉴 2: 아메리카노 - 3회 주문
        for (int i = 0; i < 3; i++) {
            orderRepository.save(Order.builder()
                    .userId("userB-" + i)
                    .menuId(americanoId)
                    .quantity(1)
                    .totalPrice(4000)
                    .orderDate(now.minus(Duration.ofMinutes(20 + i))) // 현재 시간보다 조금 더 전으로 설정
                    .status(Order.OrderStatus.COMPLETED)
                    .build());
        }

        // When & Then
        mockMvc.perform(get("/api/menus/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].menuName").value("라떼"))
                .andExpect(jsonPath("$[1].menuName").value("아메리카노"));
    }
}
