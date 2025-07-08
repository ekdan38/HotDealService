FROM gradle:8.11.1-jdk21 AS build

WORKDIR /app

COPY --chown=gradle:gradle . .

RUN gradle :user-service:clean :user-service:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/user-service/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]



