# Estágio 1: Build com Gradle
FROM gradle:8.14-jdk17-alpine AS builder

WORKDIR /app
COPY gradle ./gradle
COPY gradlew .
COPY build.gradle settings.gradle ./
COPY src ./src

RUN chmod +x gradlew

# Build sem daemon (mais rápido no Docker)
RUN ./gradlew clean bootJar --no-daemon

# Estágio 2: Runtime leve (~180MB)
FROM eclipse-temurin:17-jre-alpine

# Criar usuário não-root
RUN addgroup -g 1001 -S app && \
        adduser -S -D -H -u 1001 -h /app -s /sbin/nologin -G app -g app app
WORKDIR /app
USER app

# Copiar apenas o JAR final
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
