# Multi-stage Dockerfile for support-ticket-service
# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy parent and module POMs for efficient layer caching
COPY pom.xml .
COPY support-ticket-service/pom.xml support-ticket-service/
COPY technician-dispatch-service/pom.xml technician-dispatch-service/

# Download dependencies
RUN mvn dependency:go-offline -pl support-ticket-service -am || true

# Copy source code and build package
COPY support-ticket-service/src support-ticket-service/src
COPY technician-dispatch-service/src technician-dispatch-service/src
RUN mvn clean package -DskipTests -pl support-ticket-service -am

# Stage 2: Minimal runtime image
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app

COPY --from=builder /app/support-ticket-service/target/support-ticket-service-*.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --retries=5 --start-period=30s \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
