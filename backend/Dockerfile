# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S okabe && adduser -S okabe -G okabe

# Copy the pre-built JAR from the target directory
# Note: Ensure 'mvn package' is run before building this Docker image
COPY target/*.jar app.jar

RUN chown okabe:okabe app.jar
USER okabe

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
