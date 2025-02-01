FROM gradle:8.11.1-jdk21 AS build

WORKDIR /app

COPY --chown=gradle:gradle . .

RUN gradle :order-service:clean :order-service:bootJar -x test --no-daemon

FROM openjdk:21-jdk-slim

WORKDIR /app

COPY --from=build /app/order-service/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]