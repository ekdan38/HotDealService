# HotDeal - 선착순 구매 이커머스
***
## 📌프로젝트 소개
<br>
HotDeal 프로젝트는 "핫 딜" 이벤트 기간동안 한정 된 수량의 상품을 "핫 딜 가격"으로 선착순 판매하는 온라인 플랫폼의 MSA 아키텍처 백엔드 API 서버 입니다.
사용자 인증, 위시리스트 관리, 상품 관리, 핫딜, 주문, 결제 기능을 제공합니다.


##### 프로젝트 진행 기간
2024.12 ~ 2025.01

## 💻기술 스택

[//]: # (#### 프로그래밍 언어 및 프레임워크)
<div style="text-align: left;">
  <img src="https://img.shields.io/badge/java21-007396?style=for-the-badge&logo=OpenJDK&logoColor=white" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring Boot3.4.0-6DB33F?style=for-the-badge&logo=Spring Boot&logoColor=white" alt="Spring Boot 3.4.0">
  <img src="https://img.shields.io/badge/Spring Security-6DB33F?style=for-the-badge&logo=Spring Security&logoColor=white" alt="Spring Security">
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=MySQL&logoColor=white" alt="MySQL">
  <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=Redis&logoColor=white" alt="Redis">
  <img src="https://img.shields.io/badge/RabbitMQ-FF6600?style=for-the-badge&logo=RabbitMQ&logoColor=white" alt="RabbitMQ">
  <img src="https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white" alt="Docker">
  <img src="https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=Hibernate&logoColor=white" alt="Hibernate">
  <img src="https://img.shields.io/badge/PostMan-FF6C37?style=for-the-badge&logo=Postman&logoColor=white" alt="Postman">
  <img src="https://img.shields.io/badge/JUnit5-25A162?style=for-the-badge&logo=JUnit5&logoColor=white" alt="JUnit 5">
  <img src="https://img.shields.io/badge/Spring Cloud Gateway-25A162?style=for-the-badge&logo=Spring&logoColor=white" alt="Spring Cloud Gateway">
  <img src="https://img.shields.io/badge/Spring Cloud Eureka-25A162?style=for-the-badge&logo=Spring&logoColor=white" alt="Spring Cloud Eureka">
  <img src="https://img.shields.io/badge/Resilience4J-59666C?style=for-the-badge&&logoColor=white" alt="Resilience4J">
 <img src="https://img.shields.io/badge/Git-F05032?style=for-the-badge&logo=Git&logoColor=white" alt="Spring Cloud Eureka">
 <img src="https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=GitHub&logoColor=white" alt="Spring Cloud Eureka">
</div>


## 🛠 ERD
![Image](https://github.com/user-attachments/assets/54f9acd0-f17c-4cde-a2b3-1dc9371e72f5)

## 🛠 아키텍처
![Image](https://github.com/user-attachments/assets/eca2262b-aef1-4c26-82bd-7f0d52af181a)

## 🎨주요 기능
### 상품 관리
- 재고에 대한 동시성 처리로 안정적인 상품 재고 관리
- 카테고리별 상품 분류
- 위시리스트 기능
### 핫딜 관리
- 핫딜 상품, 할인률, 기간 설정
### 주문, 결제 관리
- 주문 : 동시성 제어로 안전한 수량 제한된 핫딜 상품 구매
- 취소 : 미배송 상태, 주문 후 하루 까지 가능
- 반품 : 배송 완료 후 하루 까지 가능


## 🚨 트러블 슈팅
- 예외 상황 응답 일관성 및 코드 중복을 줄이기 위해 에러코드 관리
  - 글 따로 빼면서, 왜 에러코드 썻는지랑 에러 코드 정리해서 올리자


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




