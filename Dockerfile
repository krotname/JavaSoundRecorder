FROM eclipse-temurin:25-jre-jammy@sha256:c3e62cd0cece58d8de8d760ab95a5014f3b5a6ea32178f54270edb5b4aab9d1f as runtime

WORKDIR /app
COPY target/javasoundrecorder-*-all.jar /app/javasoundrecorder.jar

ENTRYPOINT ["java", "-jar", "/app/javasoundrecorder.jar"]
