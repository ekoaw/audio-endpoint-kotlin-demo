# Use Eclipse Temurin OpenJDK 21 runtime image
FROM eclipse-temurin:21-jdk-alpine

# Install FFmpeg on Alpine
RUN apk add --no-cache ffmpeg

# Set the working directory
WORKDIR /server

# Copy Gradle wrapper, build script
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle build.gradle
COPY settings.gradle settings.gradle

# Make Gradle wrapper executable
RUN chmod +x gradlew

# Download Gradle dependencies
RUN ./gradlew dependencies --no-daemon

# Copy the source code
COPY src src

# Build the application
RUN ./gradlew bootJar

# Move the built JAR to working directory
RUN mv build/libs/*.jar server.jar

# Expose port 8080
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "server.jar"]
