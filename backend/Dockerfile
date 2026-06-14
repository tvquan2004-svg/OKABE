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
# - Xmx180m: Heap tối đa (giảm mạnh để nhường chỗ cho Metaspace + native)
# - Xms64m: Heap khởi tạo thấp, không commit toàn bộ heap ngay
# - XX:MaxMetaspaceSize=110m: Giới hạn Metaspace
# - Xss512k: Stack mỗi thread (mặc định 1-2MB)
# - XX:+UseSerialGC: GC đơn luồng, footprint thấp hơn G1GC cho heap nhỏ
# - XX:+ExitOnOutOfMemoryError: Tự động thoát (container restart) khi OOM
ENTRYPOINT ["java", "-Xmx180m", "-Xms64m", "-XX:MaxMetaspaceSize=110m", "-Xss512k", "-XX:+UseSerialGC", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar", "--spring.profiles.active=prod"]
