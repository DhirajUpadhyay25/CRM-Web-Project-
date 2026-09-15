# ===============================================
# Stage 1: Build the Spring Boot application
# ===============================================
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Copy project definition and dependency manifest
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Pre-fetch dependencies where possible to accelerate layer caching
RUN mvn dependency:go-offline -B || true

# Copy application source code and compile fat JAR
COPY src ./src
RUN mvn clean package -DskipTests

# ===============================================
# Stage 2: Production JRE Runtime
# ===============================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create an unprivileged user and persistent upload paths
RUN addgroup -S appgroup && adduser -S appuser -G appgroup && \
    mkdir -p /app/upload/courses /app/upload/instructors /app/upload/users && \
    chown -R appuser:appgroup /app

# Copy executable jar from builder stage
COPY --from=builder /build/target/EducationCrmProject-1.0.jar app.jar

USER appuser

# Platform defaults (overridden by Render at runtime)
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE ${PORT}

# Container-aware JVM memory settings and dynamic port binding
ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dserver.port=${PORT} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -jar app.jar"]
