# Stage 1: Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy maven wrapper and pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Ensure wrapper script has execute permissions and pre-fetch dependencies
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy source code and build package
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root system group and user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the built jar from the builder stage with proper ownership to optimize image layer caching
COPY --from=builder --chown=appuser:appgroup /app/target/*.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
