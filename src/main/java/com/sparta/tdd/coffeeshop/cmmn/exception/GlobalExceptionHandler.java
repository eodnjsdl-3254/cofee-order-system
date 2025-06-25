package com.sparta.tdd.coffeeshop.cmmn.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException; // ❗ 이 임포트 추가
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.sparta.tdd.coffeeshop.cmmn.ErrorResponse;

@RestControllerAdvice
@Slf4j // 로깅을 위해 Lombok의 @Slf4j 사용
public class GlobalExceptionHandler {

    /**
     * 애플리케이션 정의 CustomException 처리
     * @param e 발생한 CustomException
     * @return 적절한 HTTP 상태 코드와 ErrorResponse DTO
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        log.error("CustomException: {}", e.getErrorCode().getMessage(), e);
        ErrorResponse errorResponse = new ErrorResponse(e.getErrorCode().getCode(), e.getMessage());
        return new ResponseEntity<>(errorResponse, e.getErrorCode().getHttpStatus());
    }

    /**
     * 낙관적 락 충돌 시 발생하는 OptimisticLockingFailureException 처리
     * 동시성 충돌 시 409 CONFLICT 상태 코드를 반환합니다.
     * @param e 발생한 OptimisticLockingFailureException
     * @return 409 CONFLICT 상태 코드와 동시성 충돌 메시지를 담은 ErrorResponse DTO
     */
    @ExceptionHandler(OptimisticLockingFailureException.class) // ❗ 이 핸들러를 추가
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailureException(OptimisticLockingFailureException e) {
        log.error("Optimistic Locking Failure: {}", e.getMessage(), e);
        // ErrorCode.CONCURRENCY_FAILURE를 정의했다고 가정하고 사용합니다.
        ErrorResponse errorResponse = new ErrorResponse(ErrorCode.CONCURRENCY_FAILURE.getCode(), ErrorCode.CONCURRENCY_FAILURE.getMessage());
        return new ResponseEntity<>(errorResponse, ErrorCode.CONCURRENCY_FAILURE.getHttpStatus());
    }

    /**
     * 그 외의 예상치 못한 모든 예외 처리
     * @param e 발생한 일반 Exception
     * @return 500 INTERNAL_SERVER_ERROR 상태 코드와 일반 오류 메시지를 담은 ErrorResponse DTO
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        ErrorResponse errorResponse = new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
        return new ResponseEntity<>(errorResponse, ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus());
    }
}