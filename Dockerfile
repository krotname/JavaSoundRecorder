FROM eclipse-temurin:21-jre-jammy as runtime

WORKDIR /app
COPY target/javasoundrecorder-*-all.jar /app/javasoundrecorder.jar

ENTRYPOINT ["java", "-jar", "/app/javasoundrecorder.jar"]
