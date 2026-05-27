# ==========================================================================
# Multi-Stage Dockerfile for Premium AI Interview Prep System
# Stage 1: Build the Maven Project
# ==========================================================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Copy Maven descriptor and pre-fetch dependencies (speeds up rebuilds)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source directories and compile the executable fat JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==========================================================================
# Stage 2: Final Lightweight Runtime
# ==========================================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the fat JAR compiled in Stage 1
COPY --from=builder /build/target/prep-system-1.0-SNAPSHOT-jar-with-dependencies.jar interview-prep.jar

# Bind to standard web port
ENV PORT=8080
EXPOSE 8080

# Run the unified embedded server
CMD ["java", "-jar", "interview-prep.jar"]
