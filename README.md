# HotDeal - 선착순 구매 이커머스
***
## 📌프로젝트 소개
<br>
HotDeal 프로젝트는 "핫 딜" 이벤트 기간동안 한정 된 수량의 상품을 "핫 딜 가격"으로 선착순 판매하는 온라인 플랫폼의 MSA 아키텍처 백엔드 API 서버 입니다.
사용자 인증, 위시리스트, 상품, 핫딜, 주문 기능을 제공합니다.


##### 프로젝트 진행 기간
2024.12.18 ~ 진행중

## 💻기술 스택
#### 프로그래밍 언어 및 프레임워크
- JAVA 21
- SpringBoot 3.4.0
#### 데이터베이스
- Redis
- Mysql
- H2
  - 테스트
#### 데이터 접근
- JPA
#### 보안
- SpringSecurity
  - 사용자 인증 및 권한 관리를 처리
#### API 게이트웨이 및 서비스 디스커버리
- Spring Cloud Gateway
  - 모든 요청을 중앙에서 라우팅하고 관리
- Spring Cloud Eureka
  - 서비스 디스커리를 톻애 마이크로서비스 간의 동적 상호작용 지원
#### Config 관리, 메시징 처리
- RabbitMQ
- Spring Cloud Config Server
- Spring Bus with AMQP
  - 외부 Config 관리 : 중앙 집중식 Config 관리, Config 번경 사항을 RabbitMQ를 통해 전파
#### SpringBoot Actuator
- 애플리케이션의 모니터링과 관리 엔드포인트를 제공
#### 신뢰성 및 회복력
- Resilience4j
  - 마이크로서비스 간 장애 복구 및 서비스 안정성을 위해 CircuitBreaker, Retry 를 사용
#### API 테스트
- Postman
- Junit5
#### Docker
아직 도커에 띄우지 않았습니다.(Todo)
#### 개발 도구
- Intellij IDEA
- Git & GitHub


## 🛠 ERD
업데이트 예정


## 🛠 아키텍처
![Image](https://github.com/user-attachments/assets/32748e58-7e23-4018-b415-27757036a5fb)

## 🎨주요 기능
### 사용자 인증
- 권한 부여
  - USER, ADMIN 권한 기반 API 접근 자원 제한
- 회원 가입
  - 회원 가입시 이메일 인증 코드 발송, 인증 코드 검사
- 로그인 및 로그아웃
  - JwtToken 기반 인증(Access, Refresh Token)
### 카테고리
- 카테고리 등록, 조회, 수정, 삭제
  - 카테고리 부모 자식 관계 설정 가능
### 상품 관리
- 상품 등록, 조회, 수정, 삭제
- 카테고리별 상품 분류
### 핫딜 관리
- 핫딜 이벤트 등록, 조회, 수정, 삭제
- 핫딜 상품, 할인률, 기간 설정
### 주문, 결제 관리
- 주문 생성, 조회, 취소, 반품
  - 주문 : 상품 재고 조회, 주문시에 상품에 재고에 관한 동시성 처리
  - 취소 : 배송 상태 아니고, 주문 후 하루 까지 가능
  - 반품 : 배송 완료 후 하루 까지 가능
    - 취소, 반품 완료시에 상품 재고에 관한 동시성 처리

## ❗ 트러블 슈팅
- 이메일 인증 코드 발송 비동기 처리
  - 이메일 인증 코드 발송 시에 응답 까지 약 10초 시간 소요 => 비동기 처리


- 마이크로서비스 간 FeignClient 호출 시 서비스 장애 상황 처리 위한 회복 탄력성 도입
  - Resilience4J CircuitBreaker, Retry 적용
    - Retry 과정에서 로그를 남기고 Fallback 메서드를 통해 서비스 장애시 안정적 처리(예외 응답 처리)


- 모놀로직 구조에서 MSA 구조로 전환시 인증 인가 처리 전략
  - 기존 모놀로직 구조에서는 SpringSecurity 로 인증, 권한이 필요한 엔드포인트 관리
  - MSA 구조로 전환 하면서 기존 인증 인가 방식 사용 불가능
  - ApiGateway 에서 Filter를 통해 Jwt Token 검증, User 조회, 각 서비스 라우팅시 권한 확인
    - @authenticationprincipal 사용 불가능, ApiGateway 에서 요청 헤더에 User 에대한 필요 정보 전달
  - <a href = "https://www.notion.so/MSA-16d87a78a3688021a66bedc854d1b358"> MSA 인증 인가 처리 </a>


## 🙉기술적 의사 결정
- 각 마이크로서비스 포트 번호 랜덤 포트 적용
  - 현재는 각각 1개의 마이크로서비스를 사용하지만 동일 서비스 확장시 포트 관리, 지정 어려움
  - Eureka 를 통한 인스턴스 관리, 로드밸런싱 적용


- MSA 환경에서 Config 변경 관리 문제 처리
  - MSA 구조에서 동일 서비스 확상시 Config 변경 관리 어려움 해결을 위해 Spring Cloud Coifng Server
    중심으로 Spring Cloud Bus와 RabbitMQ를 활용해 실시간 전파
  - <a href = "https://www.notion.so/Config-Server-SpringCloudBus-RabbitMQ-16d87a78a3688018bdf0d183fae13fa4"> MSA 환경 Config 관리 </a>


- 상품 조회, 재고 변경시 동시성 처리 전략 선정
  - 주문, 주문 취소, 환불시 상황에 따라 재고 조회, 재고 변경에 대한 동시성 제어 필요
  - 여러 동시성 제어 방법 중 Redisson 분산 락 적용
  - <a href = "https://www.notion.so/17687a78a36880d9b70cc7a5c0ca1b79">동시성 제어 전략 선정 </a>

## ❗ 에러 코드
업데이트 예정
## 📑API 문서
업데이트 예정
## 📁프로젝트 구조
업데이트 예정




