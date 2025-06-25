# TDD 기반 커피숍 주문/포인트 시스템 백엔드 API

## 🚀 프로젝트 소개

이 프로젝트는 TDD(Test-Driven Development) 방법론을 적용하여 개발된 커피숍 주문 및 포인트 관리 시스템의 백엔드 API입니다. 사용자가 커피 메뉴를 조회하고, 주문 및 결제를 하며, 포인트를 충전할 수 있는 기능을 제공합니다. 낙관적 락을 통한 동시성 제어와 인기 메뉴 조회 기능이 구현되었습니다.

## ✨ 주요 기능

* **메뉴 조회**: 현재 판매 중인 커피 메뉴 목록을 조회합니다.
* **단일 메뉴 조회**: 특정 메뉴의 상세 정보를 조회합니다.
* **인기 메뉴 조회**: 최근 7일간 가장 많이 주문된 메뉴 목록을 조회합니다.
* **주문 및 결제**: 보유 포인트로 커피를 주문하고 결제합니다. (낙관적 락을 통한 동시성 제어 포함)
* **포인트 충전**: 사용자의 포인트를 충전합니다. (낙관적 락을 통한 동시성 제어 포함)
* **동시성 테스트**: 여러 사용자가 동시에 주문 및 포인트 충전 시 시스템의 동시성 처리 능력을 검증합니다.

## 🛠️ 기술 스택

* **언어**: Java 17
* **프레임워크**: Spring Boot 3.3.13
* **빌드 도구**: Maven
* **데이터베이스**: H2 Database (인메모리)
* **ORM**: Spring Data JPA, Hibernate
* **테스트**: JUnit5, Mockito
* **동시성 제어**: 낙관적 락 (`@Version`)

---

## 📝 API 명세

### 1. 메뉴 목록 조회

* **GET** `/api/menus`
* **응답 예시:**
    ```json
    [
      { "id": 1, "menuName": "아메리카노", "price": 3000 },
      { "id": 2, "menuName": "카페 라떼", "price": 4000 }
    ]
    ```

### 2. 단일 메뉴 조회

* **GET** `/api/menus/{id}`
* **응답 예시:**
    ```json
    { "id": 1, "name": "아메리카노", "price": 3000 }
    ```
    *(참고: `MenuController`에서 `Menu` 엔티티를 직접 반환합니다.)*

### 3. 인기 메뉴 목록 조회

* **GET** `/api/menus/popular`
* **응답 예시:**
    ```json
    [
      { "id": 16, "menuName": "아메리카노", "price": 3000, "orderCount": 6 }
      // 실제 ID와 주문 횟수는 테스트 결과에 따라 달라질 수 있습니다.
    ]
    ```

### 4. 커피 주문 및 결제

* **POST** `/api/orders`
* **요청 예시:**
    ```json
    {
      "userId": "user001",
      "menuId": 1,
      "quantity": 2
    }
    ```
* **응답 예시:**
    ```json
    {
      "orderId": "c73b1767-dfb4-4777-bd47-2a0602801a81",
      "userId": "user001",
      "menuId": 1,
      "menuName": "아메리카노",
      "quantity": 2,
      "totalPrice": 8000,
      "remainingPoints": 12000,
      "status": "COMPLETED",
      "orderDate": "2025-06-25T11:00:00.123456"
    }
    ```

### 5. 포인트 충전

* **POST** `/api/user/points/charge`
* **요청 예시:**
    ```json
    {
      "userId": "testUser1",
      "amount": 10000
    }
    ```
* **응답 예시:**
    ```json
    {
      "userId": "testUser1",
      "point": 22000
    }
    ```

### 6. 동시성 테스트 API

이 API들은 개발 및 테스트 환경에서 시스템의 동시성 처리 로직을 검증하기 위한 것입니다. 실제 서비스에서는 사용되지 않습니다.

#### a) 동시 포인트 충전 테스트

* **POST** `/api/test/concurrent-charge`
* **Query Parameters (URL 뒤에 `?key=value&key2=value2` 형태로 추가):**
    * `numberOfThreads`: (Integer, 기본값: 5) 동시에 실행할 스레드(요청) 수
    * `userId`: (String, 기본값: "concurrentUser") 충전 대상 사용자 ID
    * `amount`: (Long, 기본값: 200) 각 요청당 충전 금액
    * `resetUserPoint`: (Boolean, 기본값: true) 테스트 전 사용자 포인트 초기화 여부
* **요청 예시:**
    `POST http://localhost:8080/api/test/concurrent-charge?numberOfThreads=10&userId=testUser&amount=100&resetUserPoint=true`
* **응답 예시:**
    ```
    동시성 테스트 완료: 총 10회 요청, 성공 1회, 실패 9회. 최종 testUser 포인트: 100
    ```
    *(참고: 낙관적 락으로 인해 성공 횟수가 낮게 나올 수 있습니다.)*

#### b) 동시 주문 및 인기 메뉴 집계 테스트

* **POST** `/api/test/concurrent-order`
* **Query Parameters:**
    * `numberOfThreads`: (Integer, 기본값: 50) 동시에 실행할 스레드(주문 요청) 수
    * `userId`: (String, 기본값: "user001") 주문할 사용자 ID
    * `menuName`: (String, 기본값: "아메리카노") 주문할 메뉴 이름 (실제 ID를 동적으로 조회하여 사용)
    * `quantity`: (Integer, 기본값: 1) 각 주문의 수량
    * `resetOrdersBeforeTest`: (Boolean, 기본값: true) 테스트 전 모든 주문/사용자/메뉴 데이터 초기화 여부
* **요청 예시:**
    `POST http://localhost:8080/api/test/concurrent-order?numberOfThreads=50&userId=user001&menuName=아메리카노&quantity=1&resetOrdersBeforeTest=true`
* **응답 예시:**
    ```text
    === 동시 주문 테스트 결과 ===
    총 요청: 50회, 성공: 6회, 실패: 44회.
    실패 상세 메시지 (최대 5개):
      - Exception: 409 : "{"code":"CONCURRENCY_FAILURE","message":"동시성 충돌이 발생했습니다. 다시 시도해주세요."}"
      - Exception: 409 : "{"code":"CONCURRENCY_FAILURE","message":"동시성 충돌이 발생했습니다. 다시 시도해주세요."}"
      ... (생략)

    === 최종 인기 메뉴 목록 (최근 7일간 주문 기준) ===
    - ID: 16, 이름: 아메리카노, 주문 횟수: 6회
    ```
    *(참고: 낙관적 락에 의해 대부분의 동시 주문이 `409 CONFLICT`로 실패하며, 최종 인기 메뉴 집계는 성공한 주문만을 정확히 반영합니다. 메뉴 ID는 `auto_increment`에 따라 달라질 수 있습니다.)*

### 7. 공통 에러 응답 형식

API 요청 처리 중 오류가 발생하면 다음과 같은 형식으로 응답합니다.

* **예시:**
    ```json
    {
      "code": "INVALID_INPUT",
      "message": "충전 금액은 0보다 커야 합니다."
    }
    ```
    *(참고: `timestamp`와 `path` 필드는 `ErrorResponse` DTO에 명시적으로 추가되어야 응답에 포함됩니다.)*

* **주요 에러 코드:**
    * `INVALID_INPUT`: 유효하지 않은 요청 데이터 (HTTP 400 Bad Request)
    * `USER_NOT_FOUND`: 요청한 사용자를 찾을 수 없음 (HTTP 404 Not Found)
    * `MENU_NOT_FOUND`: 요청한 메뉴를 찾을 수 없음 (HTTP 404 Not Found)
    * `INSUFFICIENT_POINT`: 포인트 부족 (HTTP 400 Bad Request)
    * `CONCURRENCY_FAILURE`: 동시성 충돌 발생 (HTTP 409 Conflict)
    * `INTERNAL_SERVER_ERROR`: 서버 내부 오류 (HTTP 500 Internal Server Error)

---

## 🚀 로컬 개발 환경 설정 및 실행

1.  **필수 요구 사항 설치:**
    * Java Development Kit (JDK) 17 이상
    * Maven (설치 권장, 또는 Maven Wrapper 사용)
2.  **프로젝트 클론:**
    ```bash
    git clone [https://github.com/eodnjsdl-3254/coffee-order-system.git](https://github.com/eodnjsdl-3254/coffee-order-system.git)
    cd coffee-order-system
    ```
3.  **애플리케이션 실행:**
    ```bash
    ./mvnw spring-boot:run
    ```

## 🧪 테스트 실행

프로젝트의 모든 테스트는 다음 명령어를 통해 실행할 수 있습니다:

```bash
./mvnw test