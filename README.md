# TDD 기반 커피숍 주문/포인트 시스템 백엔드 API

## 🚀 프로젝트 소개
이 프로젝트는 TDD(Test-Driven Development) 방법론을 적용하여 개발된 커피숍 주문 및 포인트 관리 시스템의 백엔드 API입니다. 사용자가 커피 메뉴를 조회하고, 주문 및 결제를 하며, 포인트를 충전할 수 있는 기능을 제공합니다.

## ✨ 주요 기능
-   **메뉴 조회**: 현재 판매 중인 커피 메뉴 목록을 조회합니다.
-   **주문 및 결제**: 보유 포인트로 커피를 주문하고 결제합니다.
-   **포인트 충전**: 사용자의 포인트를 충전합니다.

## 🛠️ 기술 스택
-   **언어**: Java 17
-   **프레임워크**: Spring Boot 3.3.13
-   **빌드 도구**: Maven
-   **데이터베이스**: H2 Database (인메모리)
-   **ORM**: Spring Data JPA, Hibernate
-   **테스트**: JUnit5, Mockito

## 📝 API 명세

### 1. 메뉴 목록 조회
-   **GET** `/api/menus`
-   **응답 예시:**
    ```json
    [
      { "id": 1, "menuName": "아메리카노", "price": 4000 },
      { "id": 2, "menuName": "카페 라떼", "price": 4500 }
    ]
    ```

### 2. 커피 주문 및 결제
-   **POST** `/api/orders`
-   **요청 예시:**
    ```json
    {
      "userId": "testUser1",
      "menuId": 1,
      "quantity": 2
    }
    ```
-   **응답 예시:**
    ```json
    {
      "orderId": "ORD-XYZ-123",
      "userId": "testUser1",
      "menuName": "아메리카노",
      "quantity": 2,
      "totalPrice": 8000,
      "remainingPoints": 12000,
      "status": "COMPLETED"
    }
    ```

### 3. 포인트 충전
-   **POST** `/api/points/charge`
-   **요청 예시:**
    ```json
    {
      "userId": "testUser1",
      "amount": 10000
    }
    ```
-   **응답 예시:**
    ```json
    {
      "userId": "testUser1",
      "point": 22000
    }
    ```
## 🚀 로컬 개발 환경 설정 및 실행

1.  **필수 요구 사항 설치:**
    -   Java Development Kit (JDK) 17 이상
    -   Maven (설치 권장, 또는 Maven Wrapper 사용)
2.  **프로젝트 클론:**
    ```bash
    git clone [YOUR_REPOSITORY_URL]
    cd [your-project-folder]
    ```
3.  **애플리케이션 실행:**
    ```bash
    ./mvnw spring-boot:run
    ```

## 🧪 테스트 실행
프로젝트의 모든 테스트는 다음 명령어를 통해 실행할 수 있습니다:
```bash
./mvnw test
