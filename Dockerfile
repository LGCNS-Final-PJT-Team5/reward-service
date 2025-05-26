# reward-service/Dockerfile
FROM gradle:8.5.0-jdk17 AS builder
WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . .
RUN gradle build -x test --no-daemon --stacktrace || (echo "Gradle build failed"; exit 1)

FROM openjdk:17-jdk-slim
VOLUME /tmp
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]