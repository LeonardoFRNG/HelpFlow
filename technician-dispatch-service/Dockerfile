# Multi-stage Dockerfile for technician-dispatch-service
# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy POM files for layer caching
COPY pom.xml .
COPY technician-dispatch-service/pom.xml technician-dispatch-service/
COPY support-ticket-service/pom.xml support-ticket-service/

RUN mvn dependency:go-offline -pl technician-dispatch-service -am || true

# Copy source code and build
COPY technician-dispatch-service/src technician-dispatch-service/src
RUN mvn clean package -DskipTests -pl technician-dispatch-service -am

# Stage 2: Minimal runtime image
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app

COPY --from=builder /app/technician-dispatch-service/target/technician-dispatch-service-*.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8081

HEALTHCHECK --interval=10s --timeout=5s --retries=5 --start-period=20s \
  CMD wget -qO- http://localhost:8081/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
