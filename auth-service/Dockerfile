# ============================================================
# Auth Service - Multi-Stage Docker Build
# Java 21 | Spring Boot | RSA JWT
# ============================================================

# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Install propertize-commons to local Maven repo
COPY propertize-commons/ /tmp/commons/
RUN mvn -f /tmp/commons/pom.xml install -DskipTests -q

COPY auth-service/pom.xml .
COPY auth-service/src ./src
RUN mvn package -DskipTests -q

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S propertize && adduser -S propertize -G propertize

COPY --from=build /app/target/auth-service-*.jar app.jar

# RSA keys for JWT signing/verification
COPY auth-service/keys/ ./keys/

RUN chown -R propertize:propertize /app
USER propertize

ENV SPRING_PROFILES_ACTIVE=docker
ENV RSA_PUBLIC_KEY_PATH=/app/keys/public_key.pem
ENV RSA_PRIVATE_KEY_PATH=/app/keys/private_key.pem

EXPOSE 8081

HEALTHCHECK --interval=20s --timeout=10s --start-period=60s --retries=5 \
    CMD wget -qO- http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
