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
![Image](https://github.com/user-attachments/assets/41e21d7e-87ea-41a0-9705-a652134e8b37)

## 🔍️ Flow Diagram
![Image](https://github.com/user-attachments/assets/1f66ccff-0f0e-45e7-9266-dd538193f5c0)


##  🎨 주요 구현 내용

### MSA 적용 배경 및 목표

HotDeal 프로젝트는 "핫 딜"이라는 이벤트에서 **높은 트래픽과 순간적인 대량의 요청**을 안정적으로 처리할 수 있는 시스템 구축을 목표로 잡았습니다. 초기 모놀리식 구조는 이러한 **대규모 동시성 처리 및 확장성**에 한계가 명확했습니다. 따라서 다음과 같은 목표를 가지고 MSA를 적용했습니다.

- **독립적인 서비스**
  - 각 비지니스 도메인(유저, 상품, 주문, 결제)을 독립적인 서비스로 분리하여, 특정 서비스에 부하가 집중되더라도 전체 시스템에 영향을 미치지 않고 해당 서비스만 유연하게 스케일아웃 가능
- **회복 탄력성 강화**
  - 서비스 간의 의존성을 낮추고, CircuitBreaker, Retry 를 적용하여 하나의 서비스 장애가 전체 시스템으로의 전파 방지

이를 통해 HotDeal 프로젝트는 **초당 수백건의 주문 요청**을 처리하며, 상황에 따라 **스케일 아웃으로 유연한 대처**가 가능하도록 시스템을 구축하고자 했습니다.

### 주요 구현 내용 및 패턴
- **모놀리식 구조를 MSA로 리팩토링**
  - 비지니스 도메인별 서비스 분리
- **Eureka, API Gateway 적용**
  - 서비스를 관리하고, 클라이언트 요청을 서비스로 라우팅하며, 전역적인 인증/인가 처리
- **동시성 처리를 통한 상품 재고 관리**
  - Redis 기반 분산락을 활용하여 핫 딜 상품의 재고 관리
- **"Outbox + FeignClient + 이벤트"를 통한 서비스 간 비동기 통신**
  - 결합도를 낮추고, 데이터 일관성을 유지하며 서비스 간 통신 병목 해소
- **ErrordDecoder로 서비스 통신 간 발생하는 예외 처리**
  - 서비스 간 호출 시 발생하는 예외를 일관되게 처리하여 시스템의 안정성 향상
- **Resilience4J의 CircuitBreaker, Retry를 통한 회복 탄력성**
  - 서비스 간 호출 시 특정 서비스의 장애가 다른 서비스로 전파되는 문제 예방

##  ❗ 성능 개선 사례
>[성능 개선 사례 바로가기](<https://github.com/ekdan38/HotDealService/wiki/%EC%84%B1%EB%8A%A5-%EA%B0%9C%EC%84%A0-%EC%82%AC%EB%A1%80-%EB%AA%A9%EB%A1%9D>)

### 테스트 환경
- **CPU** : Intel i5-8250U 1.6GHz
- **RAM** : 8GB
- **OS** : Window10
- **Databse** : MySQL 8.0
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
> 기존 Feign 동기 요청에서 Outbox + 이벤트 기반 비동기 처리.  
> -> 통신 병목 제거로 Latency, TPS 개선 효과를 얻음.

- **개선 전** : Feign 동기 요청
- **개선 후** : **Outbox 패턴 + 이벤트 기반 비동기 처리** 적용. 주문 생성 트랜잭션 내부에서 Outbox를 생성하고, 트랜잭션 Commit 후 스케줄러에 의해 이벤트 기반으로 처리

**주문 API 성능 테스트 결과**

더미 데이터 : 1만개의 핫딜, 10만개의 상품<br>
시나리오 : 50개의 상품중 1 ~ 3개 랜덤 주문

<img src="https://github.com/user-attachments/assets/d7adef4e-0a85-4737-9ad8-0243526b6e8d" alt="image" width="700" />

<br>

**3. 최종 성능 목표 및 결과**

**개선 전**
| 시나리오 (3분) | 총 처리량 | 평균 지연 시간 (Latency mean) | 95% 지연 시간 (Latency P95) | TPS |
|----------------|------------|-------------------------------|------------------------------|-----|
| VU 100         | 6,028      | 3.01s                         | 3.74s                        | 32  |
| VU 200         | 5,587      | 6.56s                         | 7.86s                        | 29  |
| VU 300         | 5,657      | 9.83s                         | 18.12s                       | 29  |

**개선 후**
| 시나리오 (3분) | 총 처리량 | 평균 지연 시간 (Latency mean) | 95% 지연 시간 (Latency P95) | TPS |
|----------------|------------|-------------------------------|------------------------------|-----|
| VU 100         | 7,507      | 2.41s                         | 3.14s                        | 41  |
| VU 200         | 7,678      | 4.75s                         | 5.58s                        | 41  |
| VU 300         | 7,925      | 6.94s                         | 11.67s                       | 42  |

HotDeal 프로젝트는 높은 트래픽과 순간적인 대량의 요청에도 동시성을 제어하며 안정적인 시스템을 구축하는것이 목표입니다. 주문API 성능 개선을 통해, **이전 대비 유의미한 TPS 향상과 Latency 감소**를 보였습니다. 특히, 결제 생성을 비동기 처리함으로써 주문 과정에서의 병목을 크게 줄였습니다.

##  🧑‍💻 트러블 슈팅 및 의사결정
### [모놀로직 구조에서 MSA 구조로 전환시 인증/인가 처리](<https://github.com/ekdan38/HotDealService/wiki/MSA-%EC%97%90%EC%84%9C%EC%9D%98-%EC%9D%B8%EC%A6%9D-%EC%9D%B8%EA%B0%80-%EC%B2%98%EB%A6%AC>)
- **문제**
  - 모놀리식에서는 Filter, SecurityConfig로 시스템의 전체적인 인증/인가 관리
  - MSA에서 각 서비스가 독립적으로 인증/인가를 처리하면 코드 중복 및 유지보수 부담 증가
- **해결**
  - **API Gateway에서 인증/인가를 처리**하고, **검증된 사용자 정보를 헤더에 담아 각 서비스로 전달**하여 중복 구현 방지
  -   API Gateway의 Filter에서 JWT Token의 유효성 검증
  - 서비스별 엔드포인트가 요구하는 `requiredRole`을 비교하여 접근 제어
  - `X-User-Id` : userId
  - `X-User-Role` : userRole
***

### [재고 관리 방식 및 동시성 제어](<https://github.com/ekdan38/HotDealService/wiki/%EC%9E%AC%EA%B3%A0-%EC%B2%98%EB%A6%AC-%EB%B0%A9%EB%B2%95(%EB%B0%A9%EC%8B%9D-%EB%B0%8F-%EB%8F%99%EC%8B%9C%EC%84%B1-%EC%A0%9C%EC%96%B4)>)
- **재고 관리 구조**
  -  상품의 원본 재고 수량은 RDB 테이블에 저장
  - **실시간 재고 변동**에 대응하기 위해 **Redis를 사용한 재고 캐싱**
  - 빠른 재고 조회 및 재고 감소 처리
  - **RDB 병목 완화**
  - 스케쥴러를 통해 **기간이 종료된 핫딜**의 Redis 재고 RDB에 **동기화 처리**

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
    - 다중 인스턴스에서도 락 공유 가능
*** 

### [회복 탄력성을 위한 CircuitBreaker, Retry 도입](<https://github.com/ekdan38/HotDealService/wiki/%ED%9A%8C%EB%B3%B5-%ED%83%84%EB%A0%A5%EC%84%B1%EC%9D%84-%EC%9C%84%ED%95%9C-CuircuitBreaker,-Retry-%EB%8F%84%EC%9E%85>)
- **문제**
  - MSA 환경에서 서비스 간 통신 시, **하나의 서비스 장애가 다른 서비스로 전파될 위험** 존재
  -  다른 서비스의 일시적인 장애나 느린 응답으로 인해 호출 실패가 발생하면, 사용자 경험에 큰 영향을 줄 수 있음
- **해결**
  - `Resilience4J`의 **CircuitBreaker, Retry** 도입으로 일시적인 장애나 느린 응답에도 시스템이 안정적으로 동작하도록 회복 탄력성 적용
  - 테스트 코드로 CircuitBreaker & Retry작동 검증

*** 
### [서비스 간 통신 방법 고민](<https://github.com/ekdan38/HotDealService/wiki/%EC%84%9C%EB%B9%84%EC%8A%A4-%EA%B0%84-%ED%86%B5%EC%8B%A0-%EB%B0%A9%EC%8B%9D-%EA%B3%A0%EB%AF%BC>)
- RestTemplate vs FeignClient 중 **인터페이스 기반인 FeignClient 선택**
- **비동기 처리**
  - **"Outbox + FeignClient + 이벤트"** 방식 사용
  -  CircuitBreaker + Retry 로 회복 탄력성이 존재하지만, 비동기 처리에 대한 실패건 **재시도 환경**을 구성하여 안정성 강화

***

### [여러 인스턴스가 스케쥴러를 중복 처리하는 문제 발생](<https://github.com/ekdan38/HotDealService/wiki/%EC%84%9C%EB%B9%84%EC%8A%A4%EC%9D%98-%EC%9D%B8%EC%8A%A4%ED%84%B4%EC%8A%A4%EC%97%90-%EB%94%B0%EB%A5%B8-%EC%8A%A4%EC%BC%80%EC%A5%B4%EB%9F%AC-%EC%A4%91%EB%B3%B5-%EC%8B%A4%ED%96%89>)
- **문제**
  - 서비스의 인스턴스가 N개라면 동일한 스케쥴러가 N개의 인스턴스에서 실행
- **해결**
  - `ShedLock`을 사용하여 **한개의 인스턴스만 스케쥴러를 실행**하도록하여 중복 실행 문제 해결

## 🚀 추후 개선사항 및 계획
HotDeal 프로젝트는 현재까지 구축된 MSA 기반의 안정적인 시스템을 바탕으로, 사용자 경험 향상 및 더욱 효율적인 운영을 위해 다음과 같은 개선사항을 계획하고 있습니다.

**1. 이벤트 기반 아키텍처 확장 (Kafka 도입)**

"Outbox + FeignClient + 이벤트" 방식을 통해 서비스 간 비동기 통신을 구현했지만, 더 높은 확장성과 메시지 처리의 안정성을 위해 **Kafka 도입**

- **목표**
  - 대규모 이벤트 발생 시 메시지 유실 없는 안정적인 처리 보장
  - 더 복잡한 비동기 로직 및 이벤트 기반 데이터 동기화 구현.

**2. 데이터베이스 최적화 및 확장 (읽기 전용 DB 분리)**

MySQL과 Redis를 활용하고 있지만, 읽기 작업이 많은 핫 딜 서비스의 특성을 고려하여 읽기 전용 DB(Read Replica)를 분리하여 데이터베이스 부하를 분산하고 성능을 더욱 향상시킬 계획입니다.

- **목표**
  - 읽기 트래픽 분산을 통한 메인 DB의 부하 감소
  - 전체적인 응답 시간 단축 및 처리량 증대.

**3. 로깅 및 모니터링 강화**

서비스의 안정적인 운영을 위해 통합 로깅 및 모니터링 시스템을 구축할 예정입니다.

- **목표**
  - Prometheus/Grafana와 같은 도구를 활용한 중앙 집중식 로그 관리 및 시각화
