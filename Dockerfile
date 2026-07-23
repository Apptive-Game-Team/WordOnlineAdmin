# Stage 1: Build
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

# Copy gradle files first for dependency caching
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./

RUN ./gradlew dependencies --no-daemon

# Copy source and build
# lombok.config is required at compile time (copies @Qualifier to generated constructors)
COPY lombok.config ./
COPY src ./src
RUN ./gradlew clean build -x test --no-daemon

# Stage 2: Run
FROM eclipse-temurin:21-jre

WORKDIR /app

# Only the bootJar is produced because jar { enabled = false } in build.gradle
COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
