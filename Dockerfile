# Use Eclipse Temurin OpenJDK 21 runtime as build image
FROM eclipse-temurin:21-jdk-alpine AS build

# Set the working directory
WORKDIR /source

# Copy Gradle wrapper
COPY gradlew gradlew
COPY gradle gradle

# Make Gradle wrapper executable
RUN chmod +x gradlew

# Copy build script
COPY build.gradle build.gradle
COPY settings.gradle settings.gradle

# Download Gradle dependencies
RUN ./gradlew dependencies --no-daemon
# RUN --mount=type=cache,target=/root/.gradle ./gradlew dependencies --no-daemon

# Copy the source code
COPY src src

# Build the application
RUN ./gradlew bootJar

# Use Eclipse Temurin OpenJDK 21 runtime as server image
FROM eclipse-temurin:21-jdk-alpine

# Install FFmpeg on Alpine
RUN apk add --no-cache ffmpeg

# Set the working directory
WORKDIR /app

# Copy the jar from build image
COPY --from=build /source/build/libs/*.jar app.jar

# Expose port 8080
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
