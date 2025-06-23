package com.sparta.tdd.coffeeshop.controller.domain;

import com.sparta.tdd.coffeeshop.domain.user.User;
import com.sparta.tdd.coffeeshop.domain.user.dto.PointChargeRequest;
import com.sparta.tdd.coffeeshop.domain.user.repo.UserRepository; // UserRepository import (테스트용 사용자 초기화용)
import com.sparta.tdd.coffeeshop.domain.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate; // RestTemplate import
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/test") // 테스트 전용 API 경로
@RequiredArgsConstructor
@Slf4j
public class ConcurrentTestController {

    private final UserRepository userRepository; // 사용자 초기화 등을 위해 필요
    private final UserService userTestService; 

    // RestTemplate 빈을 직접 주입받거나, 여기서 생성하여 사용
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/concurrent-charge")
    public String runConcurrentPointCharge(
            @RequestParam(defaultValue = "5") int numberOfThreads, // 실행할 스레드 수
            @RequestParam(defaultValue = "concurrentUser") String userId, // 충전할 사용자 ID
            @RequestParam(defaultValue = "200") long amount, // 각 스레드가 충전할 금액
            @RequestParam(defaultValue = "true") boolean resetUserPoint // 테스트 전 사용자 포인트 초기화 여부
    ) throws InterruptedException {

        // 1. 테스트 사용자 포인트 초기화 (선택 사항이지만 동시성 테스트에 매우 중요)
        if (resetUserPoint) {
            log.info("테스트 전 사용자 {}의 포인트와 버전을 초기화합니다.", userId);
            // 이 초기화 블록에만 트랜잭션을 적용합니다.
            // **여기서 주입받은 서비스를 통해 호출합니다.**
            userTestService.resetUserPointAndVersion(userId);
            
            // 초기화 후 잠시 대기하여 DB 반영 시간 확보 (필요하다면)
            TimeUnit.MILLISECONDS.sleep(100);
        }

        log.info("동시성 포인트 충전 테스트 시작 - 스레드 수: {}, 사용자: {}, 충전 금액: {}", numberOfThreads, userId, amount);

        // 스레드 풀 생성 (지정된 수의 스레드를 동시에 실행)
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        // 모든 스레드가 동시에 시작하도록 제어하는 CountDownLatch
        CountDownLatch startLatch = new CountDownLatch(1); // 모든 스레드가 이 래치가 0이 될 때까지 대기
        // 모든 스레드가 완료될 때까지 메인 스레드가 대기하는 CountDownLatch
        CountDownLatch endLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfThreads; i++) {
            Callable<String> task = () -> {
                // 모든 스레드가 startLatch.await()에 도달할 때까지 대기
                startLatch.await(); // 래치가 0이 될 때까지 기다림

                try {
                    // 실제 포인트 충전 API 호출
                    String url = "http://localhost:8080/api/user/points/charge"; // 실제 애플리케이션의 URL
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    PointChargeRequest chargeRequest = new PointChargeRequest();
                    chargeRequest.setUserId(userId);
                    chargeRequest.setAmount(amount);

                    HttpEntity<PointChargeRequest> requestEntity = new HttpEntity<>(chargeRequest, headers);

                    // API 호출 및 응답 받기
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
                    endLatch.countDown(); // 작업 완료 시 래치 카운트 감소
                }
            };
            futures.add(executorService.submit(task));
        }

        // 모든 스레드가 준비되면 동시에 시작 신호 보냄
        startLatch.countDown(); // 이 순간 모든 대기 중이던 스레드가 동시에 실행 시작
        log.info("모든 스레드 시작 신호 발송. 작업 완료 대기 중...");

        // 모든 스레드가 완료될 때까지 메인 스레드 대기
        endLatch.await(60, TimeUnit.SECONDS); // 최대 60초 대기

        executorService.shutdown(); // 스레드 풀 종료 요청
        // 스레드 풀이 완전히 종료될 때까지 대기 (선택 사항)
        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
            log.warn("스레드 풀이 5초 내에 종료되지 못했습니다.");
        }

        log.info("동시성 포인트 충전 테스트 완료. 성공: {}개, 실패: {}개", successCount.get(), failCount.get());

        // 최종 사용자 포인트 확인을 위한 추가 쿼리 (DB 상태 확인용)
        long finalPoint = userRepository.findById(userId)
                .map(User::getPoint) // User::getPoint 메서드를 사용 (User 클래스 import 필요)
                .orElse(-1L); // 사용자를 찾을 수 없으면 -1 반환
        log.info("테스트 후 사용자 {}의 최종 포인트: {}", userId, finalPoint);


        String resultMessage = String.format("동시성 테스트 완료: 총 %d회 요청, 성공 %d회, 실패 %d회. 최종 %s 포인트: %d",
                numberOfThreads, successCount.get(), failCount.get(), userId, finalPoint);
        return resultMessage;
    }
}