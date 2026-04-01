# ============================================================
# Employee Service (Employecraft) - Multi-Stage Docker Build
# Java 21 | Spring Boot | PostgreSQL
# ============================================================

# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Install propertize-commons to local Maven repo
COPY propertize-commons/ /tmp/commons/
RUN mvn -f /tmp/commons/pom.xml install -DskipTests -q

# Build employee-service
COPY employee-service/pom.xml .
COPY employee-service/src ./src
RUN mvn package -Dmaven.test.skip=true -q

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S propertize && adduser -S propertize -G propertize

COPY --from=build /app/target/employecraft-*.jar app.jar

# Public RSA key for JWT validation
COPY employee-service/config/ ./config/

RUN chown -R propertize:propertize /app
USER propertize

ENV SPRING_PROFILES_ACTIVE=docker

EXPOSE 8083

HEALTHCHECK --interval=20s --timeout=10s --start-period=60s --retries=5 \
    CMD wget -qO- http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
