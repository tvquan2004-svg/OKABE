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

# Render inject biến PORT, Spring Boot đọc từ application-prod.yml
EXPOSE 8080

# JVM flags tối ưu cho bộ nhớ giới hạn (Render Free: ~512MB)
# - Xmx320m: Heap tối đa (giảm để nhường chỗ cho Metaspace + native memory)
# - XX:MaxMetaspaceSize=128m: Giới hạn Metaspace, tránh OOM do class loading
# - Xss512k: Giảm kích thước stack mỗi thread (mặc định 1-2MB)
# - XX:+UseG1GC: GC hiệu quả, ưu tiên throughput
# - XX:InitiatingHeapOccupancyPercent=30: GC sớm, giữ heap luôn thấp
ENTRYPOINT ["java", "-Xmx320m", "-Xms320m", "-XX:MaxMetaspaceSize=128m", "-Xss512k", "-XX:+UseG1GC", "-XX:InitiatingHeapOccupancyPercent=30", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar", "--spring.profiles.active=prod"]
