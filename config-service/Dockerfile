FROM gradle:8.11.1-jdk21 AS build

WORKDIR /home/gradle/src

COPY --chown=gradle:gradle . .

RUN gradle :config-service:bootJar -x test --no-daemon

FROM openjdk:21-jdk-slim

WORKDIR /app

COPY --from=build /home/gradle/src/config-service/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]