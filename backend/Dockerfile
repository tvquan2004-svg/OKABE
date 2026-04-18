# Build stage
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S okabe && adduser -S okabe -G okabe
COPY --from=builder /app/target/*.jar app.jar
RUN chown okabe:okabe app.jar
USER okabe
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
