FROM gradle:8.11.1-jdk21 AS build

WORKDIR /app

COPY --chown=gradle:gradle . .

RUN gradle :apigateway-service:clean :apigateway-service:bootJar -x test --no-daemon

FROM openjdk:21-jdk-slim

WORKDIR /app

COPY --from=build /app/apigateway-service/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]