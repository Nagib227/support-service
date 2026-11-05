FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /src
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle gradle
COPY src src
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl \
    && addgroup -S app \
    && adduser -S app -G app
WORKDIR /app
COPY --from=build /src/build/libs/support-service.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
