FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Install propertize-commons to local Maven repo
COPY propertize-commons/ /tmp/commons/
RUN mvn -f /tmp/commons/pom.xml install -DskipTests -q

# Build payment-service
COPY payment-service/pom.xml .
COPY payment-service/src ./src
RUN mvn package -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S propertize && adduser -S propertize -G propertize
COPY --from=build /app/target/*.jar app.jar
RUN chown -R propertize:propertize /app
USER propertize
ENV SPRING_PROFILES_ACTIVE=docker
EXPOSE 8084
HEALTHCHECK --interval=20s --timeout=10s --start-period=90s --retries=5 \
    CMD wget -qO- http://localhost:8084/actuator/health || exit 1
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
