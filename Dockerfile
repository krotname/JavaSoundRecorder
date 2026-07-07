FROM eclipse-temurin:21-jre-jammy@sha256:d63bd8d9b171999cbed8576f2c76e874dd4856791a358536e5c4d407e77edc13 AS runtime

WORKDIR /app
COPY target/javasoundrecorder-*-all.jar /app/javasoundrecorder.jar

ENTRYPOINT ["java", "-jar", "/app/javasoundrecorder.jar"]
