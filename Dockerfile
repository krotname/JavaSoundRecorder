FROM eclipse-temurin:21-jre-jammy@sha256:3097cbbebb7d490494a98aed2301f284b38f79eba158eef098c6fc8c8af11c23 AS runtime

WORKDIR /app
COPY target/javasoundrecorder-*-all.jar /app/javasoundrecorder.jar

ENTRYPOINT ["java", "-jar", "/app/javasoundrecorder.jar"]
