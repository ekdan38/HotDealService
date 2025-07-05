# HotDeal - 선착순 구매 이커머스
## 📌 프로젝트 소개

HotDeal 프로젝트는 "핫 딜" 이벤트 기간동안 한정된 수량의 상품을 "핫 딜 가격"으로 판매하는 온라인 플랫폼의 MSA 아키텍처 백엔드 API 서버 입니다.

프로젝트 진행 기간
2024.12 ~ 2025.05

##  📕  API 명세서
🔗 [API 명세서 보기](<https://documenter.getpostman.com/view/33322261/2sB34bJhnz>)
<br>


## 💻 사용한 기술 스택
**Framework & Library**
<div>

<img src="https://img.shields.io/badge/java21-007396?style=for-the-badge&logo=OpenJDK&logoColor=white" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring Boot3.4.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot 3.4.0">
  <img src="https://img.shields.io/badge/Spring Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" alt="Spring Security">
  <img src="https://img.shields.io/badge/Spring Cloud Gateway-25A162?style=for-the-badge&logo=Spring&logoColor=white" alt="Spring Cloud Gateway">
  <img src="https://img.shields.io/badge/Spring Cloud Eureka-25A162?style=for-the-badge&logo=Spring&logoColor=white" alt="Spring Cloud Eureka">
  <br>
  <img src="https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=Hibernate&logoColor=white" alt="Hibernate">
  <img src="https://img.shields.io/badge/RabbitMQ-FF6600?style=for-the-badge&logo=RabbitMQ&logoColor=white" alt="RabbitMQ">
  <img src="https://img.shields.io/badge/Resilience4J-59666C?style=for-the-badge&&logoColor=white" alt="Resilience4J">
</div>
<br>

**Database**
<div>
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=MySQL&logoColor=white" alt="MySQL">
  <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=Redis&logoColor=white" alt="Redis">

</div>

<br>

**Test Tool**
<div>
  <img src="https://img.shields.io/badge/PostMan-FF6C37?style=for-the-badge&logo=Postman&logoColor=white" alt="Postman">
  <img src="https://img.shields.io/badge/JUnit5-25A162?style=for-the-badge&logo=JUnit5&logoColor=white" alt="JUnit 5">
  <img src="https://img.shields.io/badge/k6-7D64FF?style=for-the-badge&logo=k6&logoColor=white" alt="k6">

</div>

<br>

**DevOps**
<div>
   <img src="https://img.shields.io/badge/Git-F05032?style=for-the-badge&logo=Git&logoColor=white" alt="GIT">
 <img src="https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=GitHub&logoColor=white" alt="GITHUB">
  <img src="https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white" alt="Docker">
</div>

<br>

## 🛠 ERD

![Image](https://github.com/user-attachments/assets/8169c851-18d6-459b-8546-97a030ac21f6)

<br>


## 🛠 아키텍처
![Image](https://github.com/user-attachments/assets/3e58876f-1f05-46be-a0e1-653bcb95311d)

## 🔍️ Flow Diagram
![Image](https://github.com/user-attachments/assets/1f66ccff-0f0e-45e7-9266-dd538193f5c0)


##  🎨 주요 구현 내용
- MSA 적용
  -  모놀리식 구조를 MSA로 리팩토링
- Eureka, API Gateway 적용
  - 각 서비스 관리 및 라우팅
- 동시성 처리를 통한 상품 재고 관리
- "Outbox + FeignClient + 이벤트" 를 통한 서비스간 통신
- ErrorDecoder로 서비스 통신간 발생하는 예외 처리
- Resilience4J의 CircuitBreaker, Retry를 통한 회복 탄력성


##  ❗ 성능 개선 사례
>[성능 개선 사례 바로가기](<https://github.com/ekdan38/HotDealService/wiki/%EC%84%B1%EB%8A%A5-%EA%B0%9C%EC%84%A0-%EC%82%AC%EB%A1%80-%EB%AA%A9%EB%A1%9D>)

### 테스트 환경
- CPU : Intel i5-8250U 1.6GHz
- RAM : 8GB
- OS : Window10
- Databse : MySQL 8.0
- Test Tool : K6

### 주문 API 성능 개선 요약

**1. 재고 점유 API 내부 최적화**
> 주문 API에서 재고 점유 API를 동기 호출하여, 주문 상품에 대한 재고 점유를 보장.  
> -> 재고 점유 API 성능을 개선하여 전체적인 Latency, TPS 개선 효과를 얻음.

- **1.1   재고 점유 쿼리 최적화 및 인덱싱**
  - N개의 상품에 대해 **N번 발생하던 재고 점유 조회 쿼리**를 IN + GROUP BY로 **1회로 개선**
  - **복합 인덱스(커버링)** 적용으로 쿼리 실행 시간 약 **47ms → 27ms (42%)** 단축
  - **TPS 약 6%, 평균 Latency 약 6% 개선**

- **2.2.  캐싱 적용**
  - 상품/핫딜 조회에 **Redis 캐싱** 적용
  - 주문 흐름 내 불필요한 DB 접근 최소화
  - **TPS 약 20%, 평균 Latency 약 16%  개선**

**재고 점유 API 성능 테스트**<br>
더미 데이터 : 1만개의 핫딜, 10만개의 상품<br>
시나리오 : 50개의 상품중 **1~ 3 개 랜덤 점유 요청**

<img src="https://github.com/user-attachments/assets/38bea379-0b1e-4649-8c1a-eb0e133cc420" alt="image" width="700" />

<br>
<br>

**2. 결제 생성 호출 구조 개선**
> 재고 점유 완료 후, 주문, 주문 상품을 생성하고 결제 생성 요청.  
> 기존 Feign 동기 요청에서 Outbox + 이벤트 기반 비동기 처리.  
> -> 통신 병목 제거로 Latency, TPS 개선 효과를 얻음.

- 주문 생성 **트랜잭션 내부에서 Outbox 생성**
- **트랜잭션 Commit후** 스케쥴러에 의해 **이벤트 기반 처리**

**주문 API 성능 테스트**

더미 데이터 : 1만개의 핫딜, 10만개의 상품<br>
시나리오 : 50개의 상품중 **1 ~ 3개 랜덤 주문**

<img src="https://github.com/user-attachments/assets/d7adef4e-0a85-4737-9ad8-0243526b6e8d" alt="image" width="700" />

##  🧑‍💻 트러블 슈팅 및 의사결정
- [모놀로직 구조에서 MSA 구조로 전환시 인증/인가 처리](<https://github.com/ekdan38/HotDealService/wiki/MSA-%EC%97%90%EC%84%9C%EC%9D%98-%EC%9D%B8%EC%A6%9D-%EC%9D%B8%EA%B0%80-%EC%B2%98%EB%A6%AC>)
  - **모놀로직 구조**에서는 Filter, SecurityConfig로 시스템의 **전체적인 인증/인가 관리**
  - **MSA 구조**에서는 **각 서비스가 독립적으로** Filter, SecurityConfig를 구성하여 **서비스별 인증/인가 처리 가능**
    - 서비스 개수에 따라 인증/인가 중복 처리 및 유지보수 부담 증가
  - **API Gateway에서 인증/인가 처리 후 각 서비스로 라우팅 적용**
    -  API Gateway의 **Filter에서 JWT Token의 유효성 검증**
    - 서비스별 엔드포인트가 요구하는 **`requiredRole`을 비교하여 접근 제어**
    - 인증된 **사용자 정보는 헤더에 담아 각 서비스**로 전달
    - `X-User-Id` : userId
    - `X-User-Role` : userRole
***

- [재고 관리 방식 및 동시성 제어](<https://github.com/ekdan38/HotDealService/wiki/%EC%9E%AC%EA%B3%A0-%EC%B2%98%EB%A6%AC-%EB%B0%A9%EB%B2%95(%EB%B0%A9%EC%8B%9D-%EB%B0%8F-%EB%8F%99%EC%8B%9C%EC%84%B1-%EC%A0%9C%EC%96%B4)>)
  - **재고 관리 구조**
    -  상품의 원본 재고 수량은 RDB 테이블에 저장
    - **실시간 재고 변동**에 대응하기 위해 **Redis를 사용한 재고 캐싱**
    - 빠른 재고 조회 및 재고 감소 처리
    - **RDB 병목 완화**
    - 스케쥴러를 통해 **기간이 종료된 핫딜**의 Redis 재고 RDB에 **동기화 처리**

  - **재고 점유 방식**
    - Redis는 조회용 캐시로만 사용, **실제 점유는 DB에 기록하여 결제 이전까지 사용자의 재고 점유 보장**
      - **Redis**는 장애시 **재고 데이터 유실 위험**
    - 재고 점유 테이블로 **상품 점유 추적 가능**
    - 결제 실패에 따른 재고 복구 용이 `RESERVED -> CANCELED`
    - Redis + 점유 테이블 사용으로 안정적인 재고 관리

  - **동시성 문제 및 해결**
    - 여러 트랜잭션이 동일한 재고에 대해 **점유 가능 여부 판단 + 재고 점유 테이블에 INSERT** 처리하는 과정에서 **동시성 문제 발생**
    - MSA 환경에 적합한 **Redis 기반 분산 락**을 사용하여 동시성 해결
      - **다중 인스턴스에서도 락 공유 가능**
    - "재고 점유 판단 및 INSERT" 전체 과정을 **임계 구역** 처리

*** 

- [회복 탄력성을 위한 CircuitBreaker, Retry 도입](<https://github.com/ekdan38/HotDealService/wiki/%ED%9A%8C%EB%B3%B5-%ED%83%84%EB%A0%A5%EC%84%B1%EC%9D%84-%EC%9C%84%ED%95%9C-CuircuitBreaker,-Retry-%EB%8F%84%EC%9E%85>)
  - MSA 구조에서는 각 서비스가 네트워크를 통해 통신하기 때문에, **하나의 서비스 장애가 다른 서비스로 전파될 위험** 존재
  -  다른 서비스의 **일시적인 장애나 느린 응답**으로 인해 호출 실패가 발생하면, **사용자 경험에 큰 영향**을 줄 수 있음
  - `Resilience4J`의 **CircuitBreaker, Retry 도입으로 회복 탄력성 적용**
  - 테스트 코드로 Retry & CircuitBreaker **작동 검증**

*** 

- [서비스 간 통신 방법 고민](<https://github.com/ekdan38/HotDealService/wiki/%EC%84%9C%EB%B9%84%EC%8A%A4-%EA%B0%84-%ED%86%B5%EC%8B%A0-%EB%B0%A9%EC%8B%9D-%EA%B3%A0%EB%AF%BC>)
  - RestTemplate vs FeignClient 중 **인터페이스 기반인 FeignClient 선택**
  - 비동기 처리시 Kafka vs FeignClient 중 FeignClient 선택
    - **"Outbox + FeignClient + 이벤트"** 방식 사용
    -  CircuitBreaker + Retry 로 회복 탄력성이 존재하지만, **비동기 처리에 대한 실패건 재시도 환경 구성**

***

- [여러 인스턴스가 스케쥴러를 중복 처리하는 문제 발생](<https://github.com/ekdan38/HotDealService/wiki/%EC%84%9C%EB%B9%84%EC%8A%A4%EC%9D%98-%EC%9D%B8%EC%8A%A4%ED%84%B4%EC%8A%A4%EC%97%90-%EB%94%B0%EB%A5%B8-%EC%8A%A4%EC%BC%80%EC%A5%B4%EB%9F%AC-%EC%A4%91%EB%B3%B5-%EC%8B%A4%ED%96%89>)
  - 서비스의 **인스턴스가 N개**라면 **동일한 스케쥴러가 N개의 인스턴스에서 실행**
  - `ShedLock`을 사용하여 **한개의 인스턴스만 스케쥴러를 실행**