FROM eclipse-temurin:25-jdk AS builder

WORKDIR /build

COPY gradle/ gradle/
COPY gradlew settings.gradle build.gradle ./
RUN chmod +x gradlew

# Прогрев кеша
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon help >/dev/null

COPY src/ src/

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootJar


FROM eclipse-temurin:25-jre

WORKDIR /app


RUN useradd -r -u 10001 appuser


COPY --from=builder /build/build/libs/*.jar /app/app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]