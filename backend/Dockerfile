# Multi-stage Docker build for Spring Boot Backend using Eclipse Temurin Java 17
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create a non-privileged user for security
RUN groupadd -r coldchain && useradd -r -g coldchain coldchain

COPY --from=builder /app/target/cold-chain-platform-1.0.0.jar app.jar
RUN chown coldchain:coldchain app.jar

USER coldchain

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
