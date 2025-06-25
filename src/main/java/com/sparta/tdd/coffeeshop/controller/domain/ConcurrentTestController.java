package com.sparta.tdd.coffeeshop.controller.domain;

import com.sparta.tdd.coffeeshop.domain.menu.Menu;
import com.sparta.tdd.coffeeshop.domain.menu.dto.MenuResponse;
import com.sparta.tdd.coffeeshop.domain.menu.dto.PopularMenuResponse;
import com.sparta.tdd.coffeeshop.domain.order.dto.OrderRequest;
import com.sparta.tdd.coffeeshop.domain.order.dto.OrderResponse;
import com.sparta.tdd.coffeeshop.domain.user.User;
import com.sparta.tdd.coffeeshop.domain.user.dto.PointChargeRequest;
import com.sparta.tdd.coffeeshop.domain.user.repo.UserRepository;
import com.sparta.tdd.coffeeshop.domain.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class ConcurrentTestController {

    private final UserRepository userRepository;
    private final UserService userService;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 동시성 포인트 충전 테스트 API
     */
    @PostMapping("/concurrent-charge")
    public String runConcurrentPointCharge(
            @RequestParam(defaultValue = "5") int numberOfThreads,
            @RequestParam(defaultValue = "concurrentUser") String userId,
            @RequestParam(defaultValue = "200") long amount,
            @RequestParam(defaultValue = "true") boolean resetUserPoint
    ) throws InterruptedException {

        if (resetUserPoint) {
            log.info("테스트 전 사용자 {}의 포인트와 버전을 초기화합니다.", userId);
            userService.resetUserPointAndVersion(userId);
            
            TimeUnit.MILLISECONDS.sleep(100);
        }

        log.info("동시성 포인트 충전 테스트 시작 - 스레드 수: {}, 사용자: {}, 충전 금액: {}", numberOfThreads, userId, amount);

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            Callable<String> task = () -> {
                startLatch.await();

                try {
                    String url = "http://localhost:8080/api/user/points/charge";
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    PointChargeRequest chargeRequest = new PointChargeRequest();
                    chargeRequest.setUserId(userId);
                    chargeRequest.setAmount(amount);

                    HttpEntity<PointChargeRequest> requestEntity = new HttpEntity<>(chargeRequest, headers);

                    ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

                    if (response.getStatusCode().is2xxSuccessful()) {
                        successCount.incrementAndGet();
                        log.debug("스레드 성공 - userId: {}, amount: {}, status: {}", userId, amount, response.getStatusCode());
                        return "Success: " + response.getBody();
                    } else {
                        failCount.incrementAndGet();
                        log.warn("스레드 실패 - userId: {}, amount: {}, status: {}, body: {}", userId, amount, response.getStatusCode(), response.getBody());
                        return "Failed: " + response.getStatusCode() + " - " + response.getBody();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.error("스레드 예외 발생 - userId: {}, amount: {}, error: {}", userId, amount, e.getMessage());
                    return "Exception: " + e.getMessage();
                } finally {
                    endLatch.countDown();
                }
            };
            executorService.submit(task);
        }

        startLatch.countDown();
        log.info("모든 스레드 시작 신호 발송. 작업 완료 대기 중...");

        endLatch.await(60, TimeUnit.SECONDS);

        executorService.shutdown();
        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
            log.warn("스레드 풀이 5초 내에 종료되지 못했습니다.");
        }

        log.info("동시성 포인트 충전 테스트 완료. 성공: {}개, 실패: {}개", successCount.get(), failCount.get());

        long finalPoint = userRepository.findById(userId)
                .map(User::getPoint)
                .orElse(-1L);
        log.info("테스트 후 사용자 {}의 최종 포인트: {}", userId, finalPoint);


        String resultMessage = String.format("동시성 테스트 완료: 총 %d회 요청, 성공 %d회, 실패 %d회. 최종 %s 포인트: %d",
                numberOfThreads, successCount.get(), failCount.get(), userId, finalPoint);
        return resultMessage;
    }
    
    /**
     * 동시 주문 및 인기 메뉴 집계 테스트 API
     */
    @PostMapping("/concurrent-order")
    public String runConcurrentOrderAndCheckPopularMenu(
            @RequestParam(defaultValue = "50") int numberOfThreads,
            @RequestParam(defaultValue = "user001") String userId,
            @RequestParam(defaultValue = "아메리카노") String menuName,
            @RequestParam(defaultValue = "1") int quantity,
            @RequestParam(defaultValue = "true") boolean resetOrdersBeforeTest
    ) throws InterruptedException {

        // 1. 테스트 전 데이터 초기화 (사용자, 주문 내역, 메뉴 등)
        if (resetOrdersBeforeTest) {
            log.info("테스트 전 모든 사용자 및 주문 내역을 초기화하고 기본 데이터를 생성합니다.");
            try {
                userService.resetUsersAndOrdersForConcurrentOrderTest();
                log.info("모든 사용자 및 주문 내역 초기화 완료.");
                TimeUnit.MILLISECONDS.sleep(500); // DB 반영 시간 확보 (충분히 대기)
            } catch (Exception e) {
                log.error("주문 초기화 중 오류 발생: {}", e.getMessage(), e);
                return "초기화 실패: " + e.getMessage();
            }
        }

        // 2. 주문할 메뉴의 실제 ID와 가격 조회 (DB에서 동적으로 가져옴)
        Long tempActualMenuId = null; // ❗ 임시 변수
        long tempActualMenuPrice = 0; // ❗ 임시 변수

        try {
            log.info("메뉴 '{}'의 실제 ID 및 가격 조회를 위해 /api/menus 호출", menuName);
            ResponseEntity<List<MenuResponse>> allMenusResponse = restTemplate.exchange(
                "http://localhost:8080/api/menus",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<MenuResponse>>() {}
            );
            
            if (allMenusResponse.getStatusCode().is2xxSuccessful() && allMenusResponse.getBody() != null) {
                log.info("모든 메뉴 목록 조회 결과: {}", allMenusResponse.getBody());
                allMenusResponse.getBody().forEach(m -> log.info("  - 메뉴: ID={}, 이름={}, 가격={}", m.getId(), m.getMenuName(), m.getPrice()));

                Optional<MenuResponse> targetMenuResponse = allMenusResponse.getBody().stream()
                    .filter(m -> m.getMenuName().equals(menuName))
                    .findFirst();

                if (targetMenuResponse.isPresent()) {
                    tempActualMenuId = targetMenuResponse.get().getId(); // 임시 변수에 할당
                    tempActualMenuPrice = targetMenuResponse.get().getPrice(); // 임시 변수에 할당
                    log.info("메뉴 '{}'의 실제 ID: {}, 가격: {}", menuName, tempActualMenuId, tempActualMenuPrice);
                } else {
                    return "메뉴 '" + menuName + "'를 찾을 수 없습니다. 테스트를 진행할 수 없습니다.";
                }
            } else {
                return "메뉴 목록 조회 실패 (상태: " + allMenusResponse.getStatusCode() + "). 테스트를 진행할 수 없습니다.";
            }
        } catch (Exception e) {
            log.error("메뉴 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            return "메뉴 정보 조회 중 오류 발생: " + e.getMessage();
        }
        
        // 최종적으로 람다에서 사용할 final 변수 선언 및 할당
        final Long actualMenuId = tempActualMenuId; // ❗ final 변수로 선언하고 임시 변수 값 할당
        final long actualMenuPrice = tempActualMenuPrice; // ❗ final 변수로 선언하고 임시 변수 값 할당

        // 실제 메뉴 ID가 null이면 오류 반환 (null 체크는 final 변수 할당 전에 하는 것이 안전)
        if (actualMenuId == null) {
            return "오류: '" + menuName + "' 메뉴의 실제 ID를 가져올 수 없습니다. 테스트 중단.";
        }
        
        long totalOrderPricePerRequest = actualMenuPrice * quantity;

        log.info("동시 주문 테스트 시작 - 스레드 수: {}, 사용자: {}, 메뉴 이름: '{}' (ID:{}), 수량: {}, 각 주문 금액: {}",
                numberOfThreads, userId, menuName, actualMenuId, quantity, totalOrderPricePerRequest);

        // 3. 병렬 주문 실행
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successOrderCount = new AtomicInteger(0);
        AtomicInteger failOrderCount = new AtomicInteger(0);
        List<String> failedMessages = new ArrayList<>();

        for (int i = 0; i < numberOfThreads; i++) {
            Callable<String> task = () -> {
                startLatch.await();

                try {
                    String url = "http://localhost:8080/api/orders";
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    OrderRequest orderRequest = new OrderRequest();
                    orderRequest.setUserId(userId);
                    orderRequest.setMenuId(actualMenuId); // ❗ final 변수 사용
                    orderRequest.setQuantity(quantity);

                    HttpEntity<OrderRequest> requestEntity = new HttpEntity<>(orderRequest, headers);

                    ResponseEntity<OrderResponse> response = restTemplate.postForEntity(url, requestEntity, OrderResponse.class);

                    if (response.getStatusCode().is2xxSuccessful()) {
                        successOrderCount.incrementAndGet();
                        return "Success: " + response.getBody();
                    } else {
                        failOrderCount.incrementAndGet();
                        String errorBody = (response.getBody() != null) ? response.getBody().toString() : "No body";
                        log.warn("주문 실패 - userId: {}, menuId: {}, status: {}, body: {}", userId, actualMenuId, response.getStatusCode(), errorBody);
                        failedMessages.add("Failed: " + response.getStatusCode() + " - " + errorBody);
                        return "Failed: " + response.getStatusCode() + " - " + errorBody;
                    }
                } catch (Exception e) {
                    failOrderCount.incrementAndGet();
                    log.error("주문 스레드 예외 발생 - userId: {}, menuId: {}, error: {}", userId, actualMenuId, e.getMessage(), e);
                    failedMessages.add("Exception: " + e.getMessage());
                    return "Exception: " + e.getMessage();
                } finally {
                    endLatch.countDown();
                }
            };
            executorService.submit(task);
        }

        startLatch.countDown();
        log.info("모든 주문 스레드 시작 신호 발송. 작업 완료 대기 중...");

        endLatch.await(120, TimeUnit.SECONDS);
        executorService.shutdown();
        if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
            log.warn("주문 스레드 풀이 10초 내에 종료되지 못했습니다.");
        }

        log.info("동시 주문 테스트 완료. 성공: {}개, 실패: {}개", successOrderCount.get(), failOrderCount.get());

        // 4. 동시 주문 완료 후 인기 메뉴 조회 (집계 정확성 확인)
        List<PopularMenuResponse> popularMenus = new ArrayList<>();
        try {
            log.info("동시 주문 완료 후 인기 메뉴 조회 API 호출: /api/menus/popular");
            ResponseEntity<List<PopularMenuResponse>> popularMenuResponse = restTemplate.exchange(
                    "http://localhost:8080/api/menus/popular",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<PopularMenuResponse>>() {}
            );

            if (popularMenuResponse.getStatusCode().is2xxSuccessful() && popularMenuResponse.getBody() != null) {
                popularMenus = popularMenuResponse.getBody();
                log.info("인기 메뉴 조회 성공: {}개", popularMenus.size());
            } else {
                log.warn("인기 메뉴 조회 실패: status={}, body={}", popularMenuResponse.getStatusCode(), popularMenuResponse.getBody());
            }
        } catch (Exception e) {
            log.error("인기 메뉴 조회 중 예외 발생: {}", e.getMessage(), e);
        }

        // 5. 최종 결과 메시지 구성
        StringBuilder resultBuilder = new StringBuilder();
        resultBuilder.append(String.format("=== 동시 주문 테스트 결과 ===\n"));
        resultBuilder.append(String.format("총 요청: %d회, 성공: %d회, 실패: %d회.\n",
                numberOfThreads, successOrderCount.get(), failOrderCount.get()));
        resultBuilder.append("실패 상세 메시지 (최대 5개):\n");
        failedMessages.stream().limit(5).forEach(msg -> resultBuilder.append("  - ").append(msg).append("\n"));
        if (failedMessages.size() > 5) {
            resultBuilder.append(String.format("  ...외 %d개 실패 메시지.\n", failedMessages.size() - 5));
        }

        resultBuilder.append("\n=== 최종 인기 메뉴 목록 (최근 7일간 주문 기준) ===\n");
        if (popularMenus.isEmpty()) {
            resultBuilder.append("인기 메뉴 없음 (최근 7일간 주문 내역 부족).\n");
        } else {
            for (PopularMenuResponse pm : popularMenus) {
                resultBuilder.append(String.format("- ID: %d, 이름: %s, 주문 횟수: %d회\n",
                        pm.getId(), pm.getMenuName(), pm.getOrderCount()));
            }
        }

        return resultBuilder.toString();
    }
}