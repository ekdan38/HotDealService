FROM gradle:8.11.1-jdk21 AS build

#WORKDIR /home/gradle/src
WORKDIR /app


COPY --chown=gradle:gradle . .

RUN gradle :config-service:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/config-service/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]