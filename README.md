# HotDeal - 선착순 구매 이커머스
## 📌 프로젝트 소개

HotDeal 프로젝트는 "핫 딜" 이벤트 기간동안 한정된 수량의 상품을 "핫 딜 가격"으로 판매하는 온라인 플랫폼의 MSA 아키텍처 백엔드 API 서버 입니다.

프로젝트 진행 기간
2024.12 ~ 2025.08

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


  <img src="https://img.shields.io/badge/Kafka-231F20?style=for-the-badge&logo=apachekafka&logoColor=#231F20" alt="Kafka">

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

## 🛠 아키텍처
![Image](https://github.com/user-attachments/assets/6617dc79-a66d-42ac-9f65-44c100381217)

## 🔍️ Flow Diagram

<img src="https://github.com/user-attachments/assets/843613bd-fc34-466a-81aa-8b636f5d15b7" height="100%">

## ⚒️ ERD
![Image](https://github.com/user-attachments/assets/9ffca3b9-08fe-4453-8f17-da73c46dd217)

<br>

##  🔧 주요 구현 내용

### MSA 적용 배경 및 목표

HotDeal 프로젝트는 "핫 딜"이라는 이벤트에서 **높은 트래픽과 순간적인 대량의 요청**을 안정적으로 처리할 수 있는 시스템 구축을 목표로 잡았습니다. 초기 모놀리식 구조는 이러한 **대규모 동시성 처리 및 확장성**에 한계가 명확했습니다. 따라서 다음과 같은 목표를 가지고 MSA를 적용했습니다.

- **독립적인 서비스**
  - 각 비즈니스 도메인(유저, 상품, 주문, 결제)을 개별 서비스로 분리
  - 특정 서비스에 부하가 집중되더라도 전체 시스템에 영향을 최소화하고, 해당 서비스만 독립적으로 스케일 아웃 가능
- **MS 간 안정적인 통신 및 회복 탄력성 강화**
  - 서비스 간 의존성을 낮추고, 회복 탄력성으로 안정적인 시스템 구성
  - 메시징 시스템 기반 비동기 통신으로 안정적인 비동기 통신 환경 구성
- **성능 최적화**
  - 쿼리 최적화 및 인덱스 개선
  - 캐싱 적용으로 조회 성능 향상

이를 통해 HotDeal 프로젝트는 **초당 수백건의 주문 요청**을 처리하며, 상황에 따라 **스케일 아웃으로 유연한 대처**가 가능하도록 시스템을 구축하고자 했습니다.

### 주요 구현 내용 및 패턴
- **모놀리식 구조를 MSA로 리팩토링**
  - **비즈니스 도메인별 서비스 분리** : 유저, 핫딜, 주문, 결제 서비스로 분리하여 유연한 확장성 확보
  - **데이터베이스 분리** : 서비스별 데이터베이스를 구축하여 독립성, 유연성, 확장성, 안정성 확보

- **Eureka & API Gateway 적용**
  - **Eureka(Service Discovery)** : Eureka에 서비스 등록 및 discovery를 통한 서비스 호출
  - **API Gateway 적용** : 클라이언트의 요청 라우팅 및 전역 인증/인가 처리

- **동시성 처리를 통한 상품 재고 관리**
  - **Redis 기반 분산락(Redisson) 적용** : 스케일 아웃을 고려한 다중 서버 환경에서도 분산락을 활용해 재고 일관성 유지
  -  **재고 점유 테이블** : 결제 완료까지 사용자 재고를 보장

- **Outbox패턴 기반 서비스 간 비동기 통신(Kafka & FeignClient)**
  - **원자성 보장** : 비즈니스 로직과 Outbox 저장을 같은 트랜잭션에서 처리 및 이벤트 발행
  - **비동기 이벤트 발행** : `@TransactionalEventListener(afterCommit)`와 `@Async`가 선언된 EventListener를 통해 트랜잭션 커밋 이후 Kafka 또는 FeignClient를 통해 전송
  - **Kafka를 통한 메시지 발행/구독** : Kafka를 사용하여 서비스 간 직접 호출 의존도를 낮추고 MS간 메시지 처리
    - `@RetryableTopic`으로 재시도/실패 메시지(DLT) 관리
  -  **FeignClient를 통한 즉시 호출** : 비동기 통신이지만, 필요에 따라 Kafka 대신 FeignClient를 사용한 호출

- **안정성 및 회복 탄력성 적용**
  - **ErrorDecoder를 통한 예외 처리** : MS간 호출시 발생하는 예외 전파 및 CircuitBreaker & Retry와 연계
  - **Resilience4J의 CircuitBreaker & Retry를 통한 회복 탄력성 적용** : 장애 발생 시 호출 차단으로 장애 전파 방지
    - **CircuitBreaker** : 장애가 발생한 MS에 대한 호출을 차단하여 장애 전파 방지
    - **Retry** : 일시적인 오류에 대해 재시도를 처리

##  ❗ 성능 개선 사례
>[성능 개선 사례 바로가기](<https://github.com/ekdan38/HotDealService/wiki/%EC%84%B1%EB%8A%A5-%EA%B0%9C%EC%84%A0-%EC%82%AC%EB%A1%80-%EB%AA%A9%EB%A1%9D>)

**목표 서비스 규모 및 성능 목표**

HotDeal 프로젝트는 핫 딜 이벤트 시 **수천 명의 동시 접속자가 발생해도 안정적으로 서비스를 제공**하고, **초당 수백 건 이상의 주문 요청을 처리**할 수 있는 시스템 구축을 목표로 했습니다. 이러한 목표 달성을 위해 시스템 전반의 병목을 해소하고 Latency,  TPS를 최적화하는 데 집중했으며, 그중 주요 성능 개선 사례는 다음과 같습니다.

### 테스트 환경
- **CPU** : Intel i5-8250U 1.6GHz
- **RAM** : 8GB
- **OS** : Windows 10
- **Database** : MySQL 8.0
- **Test Tool** : K6

### 주문 API 성능 개선 요약

**1. 재고 점유 API 내부 최적화**

주문 처리의 핵심인 재고 점유 API의 성능을 개선하여 전체 주문 API의 Latency, TPS를 향상
> 주문 API에서 재고 점유 API를 동기 호출하여, 주문 상품에 대한 재고 점유를 보장.  
> -> 재고 점유 API 성능을 개선하여 전체적인 Latency, TPS 개선 효과를 얻음.

- **1.1.   재고 점유 쿼리 최적화 및 인덱싱**
  - **개선 전** : N개의 상품에 대해 N번 발생하는 재고 점유 조회 쿼리
  - **개선 후** : `IN` 절과  `GROUP BY`를 사용하여 **1회 쿼리로 개선**
  - **복합 인덱스(커버링)** 적용으로 쿼리 실행 시간 약 **47ms → 27ms (42% 단축)**
  - **결과** : **TPS 약 6%, 평균 Latency 약 6% 개선**

- **1.2.  캐싱 적용**
  - 상품/핫딜 조회에 **Redis 캐싱**을 적용하여 주문 흐름 내 DB 접근 최소화
  - **결과** : **TPS 약 20%, 평균 Latency 약 16%  개선**

**재고 점유 API 성능 테스트 결과**<br>
더미 데이터 : 1만개의 핫딜, 10만개의 상품<br>
시나리오 : 50개의 상품중 1~ 3 개 랜덤 점유 요청

<img src="https://github.com/user-attachments/assets/38bea379-0b1e-4649-8c1a-eb0e133cc420" alt="image" width="700" />

<br>
<br>

**2. 결제 생성 호출 구조 개선**

재고 점유 완료 후 결제 생성 요청 방식을 동기 호출에서 비동기 방식으로 전환하여 통신 병목을 제거하고 Latency, TPS 개선
> 재고 점유 완료 후, 주문, 주문 상품을 생성하고 결제 생성 요청.  
> 기존 Feign 동기 요청에서 Outbox + 이벤트 기반 비동기로 전환.
> 주문 생성 트랜잭션 내부에서 Outbox 이벤트를 생성하고, 트랜잭션 커밋 후 이벤트를 비동기 처리함.  
> 이를 통해 통신 병목이 제거되어 Latency와 TPS가 개선됨.

- **개선 전** : FeignClient 동기 요청
- **개선 후** : **Outbox 패턴 + 이벤트 기반 비동기 처리**

[//]: # (> Kafka 기반 비동기 처리도 테스트 시도하였으나, 테스트 환경에서 브로커에 메시지를 발행하지 못하는 문제&#40;TimeoutException&#41;발생.  )

[//]: # (> `linger.ms`, `batch.size` 조정 후에도 동일 현상이 발생.  )

[//]: # (> `delivery.timeout.ms` 조정 후에 부하 테스트 종료 시 브로커에 메시지를 발행 함.  )

[//]: # (>  부하 테스트 시 CPU/메모리 리소스 부족으로 인해 `TimeoutException` 발생으로 유추. 따라서 실제 서비스 상황에서 이러한 문제가 생기면 결제 생성이 길어지기에 `Outbox + 이벤트 + FeignClient`기반 방식으로 최종 적용.  )

[//]: # (> &#40;다른 비동기 통신은 Kafka적용&#41;)

**주문 API 성능 테스트 결과**

더미 데이터 : 1만개의 핫딜, 10만개의 상품<br>
시나리오 : 50개의 상품중 1 ~ 3개 랜덤 주문

<img src="https://github.com/user-attachments/assets/d7adef4e-0a85-4737-9ad8-0243526b6e8d" alt="image" width="700" />

<br>

**3. 최종 성능 목표 및 결과**

**개선 전**
| 주문 생성 (3분) | 총 처리량 | Latency (mean) | Latency (P95) | TPS |
|----------------|------------|-------------------------------|------------------------------|-----|
| VU 100         | 6,028      | 3.01s                         | 3.74s                        | 32  |
| VU 200         | 5,587      | 6.56s                         | 7.86s                        | 29  |
| VU 300         | 5,657      | 9.83s                         | 18.12s                       | 29  |

**개선 후**
| 주문 생성 (3분) | 총 처리량 | Latency (mean) | Latency (P95) | TPS |
|----------------|------------|-------------------------------|------------------------------|-----|
| VU 100         | 7,507      | 2.41s                         | 3.14s                        | 41  |
| VU 200         | 7,678      | 4.75s                         | 5.58s                        | 41  |
| VU 300         | 7,925      | 6.94s                         | 11.67s                       | 42  |

HotDeal 프로젝트는 높은 트래픽과 순간적인 대량의 요청에도 동시성을 제어하며 안정적인 시스템을 구축하는것이 목표입니다. 주문API 성능 개선을 통해, **이전 대비 유의미한 TPS 향상과 Latency 감소**를 보였습니다. 특히, 결제 생성을 비동기 처리함으로써 주문 과정에서의 병목을 크게 줄였습니다.


##  🧑‍💻 트러블 슈팅 및 의사결정
### [모놀로직 구조에서 MSA 구조로 전환시 인증/인가 처리](<https://github.com/ekdan38/HotDealService/wiki/MSA-%EC%97%90%EC%84%9C%EC%9D%98-%EC%9D%B8%EC%A6%9D-%EC%9D%B8%EA%B0%80-%EC%B2%98%EB%A6%AC>)
- **문제**
  - 모놀리식에서는 `Filter`, `SecurityConfig`로 **시스템 전체 인증/인가** 관리
  - MSA에서 각 서비스가 독립적으로 인증/인가 처리 시 **코드 중복 및 유지보수 부담** 증가
- **해결**
  - **API Gateway에서 인증/인가 처리**
  -  검증된 사용자 정보를 **헤더(`X-User-Id`, `X-User-Role`)** 에 담아 각 서비스로 전달
  -   API Gateway의 Filter에서 **JWT Token 유효성 검증**
  - 서비스별 엔드포인트가 요구하는 `requiredRole`을 비교하여 **접근 제어**
- **성과**
  - 유지보수 용이성 및 확장성 향상
  - 사용자 정보 전달과 권한 기반 접근 제어 가능
***

### [재고 관리 방식 및 동시성 제어](<https://github.com/ekdan38/HotDealService/wiki/%EC%9E%AC%EA%B3%A0-%EC%B2%98%EB%A6%AC-%EB%B0%A9%EB%B2%95(%EB%B0%A9%EC%8B%9D-%EB%B0%8F-%EB%8F%99%EC%8B%9C%EC%84%B1-%EC%A0%9C%EC%96%B4)>)
- **재고 관리 구조**
  -  상품의 원본 재고 수량은 RDB 테이블에 저장
  - **실시간 재고 변동**에 대응하기 위해 **Redis를 사용한 재고 캐싱**
  - 빠른 재고 조회 및 재고 감소 처리
  - **RDB 병목 완화**
  - 스케줄러를 통해 **기간이 종료된 핫딜**의 Redis 재고 RDB에 **동기화 처리**

- **재고 점유 방식**
  - Redis는 조회용 캐시로만 사용, **실제 점유는 DB에 기록하여 결제 이전까지 사용자의 재고 점유 보장**
    - Redis 장애 시 데이터 유실 위험 방지
  - 재고 점유 테이블로 **상품 점유 추적 가능**
  - 결제 실패에 따른 재고 복구 용이 `RESERVED -> CANCELED`
  - Redis + 점유 테이블 사용으로 안정적인 재고 관리

- **동시성 문제 및 해결**
  - **문제**
    - **"점유 가능 여부 판단 + 재고 점유 테이블에 INSERT"** 과정에서 **동시성 문제 발생**
  - **해결**
    - MSA 환경에 적합한 **Redis 기반 분산 락**을 사용하여 동시성 해결

- **성과**
  - 안정적인 재고 관리 가능
  - Redis + 재고 점유 테이블 사용으로 장애 시 데이터 유실 방지
  - 다중 인스턴스 환경에서도 동시성 안전 확보
*** 

### [회복 탄력성을 위한 CircuitBreaker, Retry 도입](<https://github.com/ekdan38/HotDealService/wiki/%ED%9A%8C%EB%B3%B5-%ED%83%84%EB%A0%A5%EC%84%B1%EC%9D%84-%EC%9C%84%ED%95%9C-CuircuitBreaker,-Retry-%EB%8F%84%EC%9E%85>)
- **문제**
  -  FeignCleint를 사용 시, **하나의 서비스 장애가 다른 서비스로 전파될 위험** 존재
  -  장애 상황, 느린 응답으로 인해 **사용자 경험 저하** 발생
- **해결**
  - `Resilience4J`의 **CircuitBreaker & Retry** 도입
  - 테스트 코드로 CircuitBreaker & Retry작동 검증
- **성과**
  - 일시적 장애 발생 시에도 시스템 안정성 확보
  - MS 간 호출 실패 시 사용자 경험 저하 최소화

*** 
### [Kafka 기반 비동기 메시징 및 Outbox, Saga 패턴 적용](<https://github.com/ekdan38/HotDealService/wiki/Kafka-%EB%8F%84%EC%9E%85-(Outbox%ED%8C%A8%ED%84%B4,-Saga-%ED%8C%A8%ED%84%B4)>)
- **문제**
  - 기존 `Outbox + 이벤트 + FeignClient` 방식은 비동기 이벤트 발행 구조를 사용했지만, FeignClient 호출은 근본적으로 **동기 호출**
  - 결제 처리 후(결제 처리 -> 주문 상태 변경 -> 재고 처리) MS 간 비동기 통신에서 **강한 결합**과 동기 방식으로 인한 **성능 저하** 발생
  - MSA 환경에서의 **분산 트랜잭션 제어 문제** 존재 (예시 : 이전 작업에 대한 롤백 필요할때)
- **해결**
  - Kafka를 도입하여 결제 결과 이벤트를 **메시지 기반 비동기 통신** 전환
  - **Outbox 패턴**을 사용하여 비즈니스 트랜잭션 안에서 이벤트 저장 및 이벤트 발행 -> 트랜잭션 commit 후 Kafka 발행
    - `@TransactionalEventListener(afterCommit)`와 `@Async`가 선언된 EventListener를 통해 트랜잭션 커밋 이후 Kafka 또는 FeignClient를 통해 전송
  - `@RetryableTopic`을 사용한 메시지 **재처리 및 DLT처리**로 실패 메시지 처리
  - **Saga 패턴(코레오그래피)** 을 사용한 분산 트랜잭션 문제 해결
    - 각 MS는 로컬 트랜잭션 이후, 비지니스 로직 흐름에서 실패 시 이전 단계의 **보상 트랜잭션**을 실행하여 비지니스 로직의 일관성 유지 (예시 : 결제 실패로 인한 주문 상태 변경, 재고 점유 해제)
  -  **Kafka에 발행 실패한 메시지 처리**
    - Kafka에 발행은 성공했지만 Outbox 상태 변경 트랜잭션 오류 발생, Kafka자체의 장애로 발행 실패. 이 두가지 경우를 대비하여 스케줄러를 통해 재시도 처리 (`FAILED`, `PENDING`이지만 created_at이 오래된 Outbox 대상)
    - 재시도 횟수를 기록하여 초과된 경우 `ABORTED`로 변경하여 추후 수동으로 원인 분석후 처리 가능

-  **성과**
    - 서비스 간 안전한 비동기 이벤트 처리
    - 분산 트랜잭션 문제 해결

***

### [여러 인스턴스가 스케줄러를 중복 처리하는 문제 발생](<https://github.com/ekdan38/HotDealService/wiki/%EC%84%9C%EB%B9%84%EC%8A%A4%EC%9D%98-%EC%9D%B8%EC%8A%A4%ED%84%B4%EC%8A%A4%EC%97%90-%EB%94%B0%EB%A5%B8-%EC%8A%A4%EC%BC%80%EC%A5%B4%EB%9F%AC-%EC%A4%91%EB%B3%B5-%EC%8B%A4%ED%96%89>)
- **문제**
  - 서비스의 인스턴스가 N개라면 동일한 스케쥴러가 N개의 인스턴스에서 실행
  - 불필요한 작업 중복 및 처리되는 데이터 충돌 야기
- **해결**
  - `ShedLock`을 사용하여 **한개의 인스턴스만 스케쥴러를 실행**
- **성과**
  - 스케줄러 중복 실행 문제 해결


## 🚀 추후 개선사항 및 계획
HotDeal 프로젝트는 현재까지 구축된 MSA 기반의 안정적인 시스템을 바탕으로, 사용자 경험 향상 및 더욱 효율적인 운영을 위해 다음과 같은 개선사항을 계획하고 있습니다.

**1. 데이터베이스 최적화 및 확장 (읽기 전용 DB 분리)**

MySQL과 Redis를 활용하고 있지만, 읽기 작업이 많은 핫 딜 서비스의 특성을 고려하여 읽기 전용 DB(Read Replica)를 분리하여 데이터베이스 부하를 분산하고 성능을 더욱 향상시킬 계획입니다.

- **목표**
  - 읽기 트래픽 분산을 통한 메인 DB의 부하 감소
  - 전체적인 응답 시간 단축 및 처리량 증가

**2. 로깅 및 모니터링 강화**

서비스의 안정적인 운영을 위해 통합 로깅 및 모니터링 시스템을 구축할 예정입니다.

- **목표**
  - Prometheus/Grafana와 같은 도구를 활용한 중앙 집중식 로그 관리 및 시각화


