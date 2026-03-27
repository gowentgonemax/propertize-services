# ============================================================
# Payroll Service - Multi-Stage Docker Build
# Java 21 | Spring Boot | PostgreSQL
# ============================================================

# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S propertize && adduser -S propertize -G propertize

COPY --from=build /app/target/payroll-service-*.jar app.jar

RUN chown -R propertize:propertize /app
USER propertize

ENV SPRING_PROFILES_ACTIVE=docker

EXPOSE 8085

HEALTHCHECK --interval=20s --timeout=10s --start-period=90s --retries=5 \
    CMD wget -qO- http://localhost:8085/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
