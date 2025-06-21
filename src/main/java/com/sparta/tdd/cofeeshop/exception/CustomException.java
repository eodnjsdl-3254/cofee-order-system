package com.sparta.tdd.cofeeshop.exception;

import lombok.Getter;
//import lombok.RequiredArgsConstructor;

@Getter
//@RequiredArgsConstructor // Lombok이 'private final ErrorCode errorCode;'에 대한 생성자를 자동으로 만들어줍니다.
public class CustomException extends RuntimeException { // RuntimeException 상속 필수!
	
	private final ErrorCode errorCode;
	
	private static final long serialVersionUID = 1L; // Serializable 인터페이스: 어떤 클래스가 직렬화될 수 있음을 나타내려면 java.io.Serializable 인터페이스를 구현(implement)해야 합니다.	
    
	public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage()); // RuntimeException 메시지로 ErrorCode의 메시지 사용
        this.errorCode = errorCode;
    }

    public CustomException(ErrorCode errorCode, String message) {
        super(message); // 상세 메시지 지정 가능
        this.errorCode = errorCode;
    }
}