# jdk 21
FROM openjdk:21-jdk

# 컨테이너 안에 디렉토리
WORKDIR /app

# jar 파일 컨테이너로 복사
COPY build/libs/hotdeal-0.0.1-SNAPSHOT.jar app.jar

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]

# 포트번호
EXPOSE 8080