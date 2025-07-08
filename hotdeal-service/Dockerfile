FROM gradle:8.11.1-jdk21 AS build

WORKDIR /app

COPY --chown=gradle:gradle . .

RUN gradle :hotdeal-service:clean :hotdeal-service:bootJar -x test --no-daemon


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/hotdeal-service/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]