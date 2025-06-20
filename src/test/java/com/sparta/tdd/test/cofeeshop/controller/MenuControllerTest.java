package com.sparta.tdd.test.cofeeshop.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.sparta.tdd.cofeeshop.model.Menu;
import com.sparta.tdd.cofeeshop.repository.MenuRepository;

@SpringBootTest // 스프링 컨텍스트 전체를 로드합니다.
@AutoConfigureMockMvc // MockMvc 객체를 자동으로 구성합니다.
@Transactional // 각 테스트 메서드가 끝난 후 트랜잭션을 롤백하여 DB 상태를 초기화합니다.
@ActiveProfiles("test") // 테스트용 프로필 (H2 DB 설정)을 활성화합니다.
public class MenuControllerTest {
	@Autowired
    private MockMvc mockMvc; // HTTP 요청을 시뮬레이션하는 객체

    @Autowired
    private MenuRepository menuRepository; // 실제 DB에 데이터를 넣기 위해 Repository 주입 (통합 테스트이므로 실제 객체 사용)

    
    @Test
    @DisplayName("GET /api/menus: 모든 메뉴를 성공적으로 조회한다.")
    void getAllMenus_Success() throws Exception {
        // Given (준비): 테스트를 위한 데이터 삽입
        // 초기에는 메뉴가 없다고 가정하고, 이 테스트를 통과시키기 위해 MenuController에서 데이터를 넣어주는 로직이 필요하게 됩니다.
        // 하지만 TDD 관점에서 이 테스트는 "메뉴가 있을 때"를 상정하고 작성합니다.
        // 따라서, 테스트 환경에서 메뉴를 미리 저장해둡니다.
        menuRepository.save(new Menu("아메리카노", 3000));
        menuRepository.save(new Menu("카페라떼", 4000));

        // When (실행) & Then (검증): API 호출 및 응답 검증
        mockMvc.perform(get("/api/menus") // GET 요청을 /api/menus 경로로 보냄
                        .contentType(MediaType.APPLICATION_JSON)) // 요청 헤더에 Content-Type 설정
                .andExpect(status().isOk()) // HTTP 상태 코드가 200 OK인지 검증
                .andExpect(jsonPath("$[0].name").value("아메리카노")) // 첫 번째 메뉴의 이름이 "아메리카노"인지 검증
                .andExpect(jsonPath("$[0].price").value(3000)) // 첫 번째 메뉴의 가격이 3000인지 검증
                .andExpect(jsonPath("$[1].name").value("카페라떼")) // 두 번째 메뉴의 이름이 "카페라떼"인지 검증
                .andExpect(jsonPath("$[1].price").value(4000)) // 두 번째 메뉴의 가격이 4000인지 검증
                .andExpect(jsonPath("$.length()").value(2)); // 응답 배열의 크기가 2인지 검증
    }

    @Test
    @DisplayName("GET /api/menus: 메뉴가 없을 때 빈 배열을 반환한다.")
    void getAllMenus_NoContent() throws Exception {
        // Given (준비): 데이터베이스에 아무 메뉴도 없음 (Transactional로 자동 롤백되므로 별도 처리 불필요)

        // When (실행) & Then (검증):
        mockMvc.perform(get("/api/menus")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // 메뉴가 없어도 200 OK와 빈 배열을 반환하는 것이 일반적입니다.
                .andExpect(jsonPath("$.length()").value(0)); // 응답 배열의 크기가 0인지 검증
    }
}
