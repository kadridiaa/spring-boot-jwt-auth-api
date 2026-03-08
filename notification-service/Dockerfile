# Multi-stage build for optimized Docker image

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent pom
COPY ../pom.xml .
COPY pom.xml ./notification-service/

# Copy source code
COPY src ./notification-service/src

# Build the application
WORKDIR /app/notification-service
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy jar from build stage
COPY --from=build /app/notification-service/target/*.jar app.jar

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/api/notification/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
