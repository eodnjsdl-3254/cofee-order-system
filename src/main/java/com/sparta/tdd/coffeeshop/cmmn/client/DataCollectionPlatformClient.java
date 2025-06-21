package com.sparta.tdd.coffeeshop.cmmn.client;

import java.util.Map;

// Mock API 인터페이스: 실제 외부 데이터 수집 플랫폼과 연동하는 로직
// Kafka Producer 등으로 대체될 수 있음
public interface DataCollectionPlatformClient {
    void sendOrderData(Map<String, Object> orderData);
}