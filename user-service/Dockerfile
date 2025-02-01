# 1단계: 빌드 단계
FROM gradle:8.11.1-jdk21 AS build
WORKDIR /home/gradle/project
# 소스 코드와 gradle 관련 파일을 복사 (캐시 활용을 위해 설정 파일 먼저 복사할 수도 있음)
COPY --chown=gradle:gradle . .
# 테스트를 제외하고 빌드 (테스트를 실행하고 싶다면 -x test 제거)
RUN gradle :user-service:clean :user-service:bootJar -x test --no-daemon

# 2단계: 실행 단계
FROM openjdk:21-jdk-slim
WORKDIR /app
# 빌드 단계에서 생성된 JAR 파일을 복사 (build/libs 폴더의 JAR 파일을 복사)
COPY --from=build /home/gradle/project/user-service/build/libs/*.jar app.jar
# 기본 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]