package com.sparta.tdd.cofeeshop.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode{
	// --- 공통 에러 ---
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "잘못된 입력 값입니다."),
    
    // 500 Internal Server Error (일반적인 서버 오류)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),

    // --- 사용자 관련(404 Not Found) 에러 ---
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),

    // --- 포인트/결제 관련 에러 ---
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "INSUFFICIENT_POINT", "포인트가 부족합니다."),
   
    // 409 Conflict (동시성 문제 등)
    CONCURRENCY_FAILURE(HttpStatus.CONFLICT, "CONCURRENCY_FAILURE", "동시성 충돌이 발생했습니다. 다시 시도해주세요."),

    // --- 메뉴 관련 에러 ---
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "MENU_NOT_FOUND", "메뉴를 찾을 수 없습니다.");

    // --- 필드 정의 ---
    private final HttpStatus httpStatus; // HTTP 상태 코드 (enum 타입)
    private final String code;           // 고유 에러 코드 (예: BUSINESS_CODE_001)
    private final String message;        // 사용자에게 보여줄 메시지

    // --- Enum 생성자 (enum 상수를 초기화할 때 사용) ---
    // 여기서는 @RequiredArgsConstructor를 사용하지 않고 명시적으로 생성자를 정의합니다.
    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
