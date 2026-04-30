# Build stage
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
# Cache dependencies
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S okabe && adduser -S okabe -G okabe

# Copy the JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

RUN chown okabe:okabe app.jar
USER okabe

# Render sử dụng biến môi trường PORT, chúng ta cấu hình để Spring Boot nhận port này
ENV SERVER_PORT=8080
EXPOSE 8080

# Cấu hình RAM (Xmx384m) để không bị crash trên gói Free của Render
ENTRYPOINT ["java", "-Xmx384m", "-Xms384m", "-jar", "app.jar", "--spring.profiles.active=prod"]
