FROM eclipse-temurin:21-jre-jammy@sha256:199aebeb3adcde4910695cdebfe782ada38dadb6cc8013159b58d3724451befd as runtime

WORKDIR /app
COPY target/javasoundrecorder-*-all.jar /app/javasoundrecorder.jar

ENTRYPOINT ["java", "-jar", "/app/javasoundrecorder.jar"]
