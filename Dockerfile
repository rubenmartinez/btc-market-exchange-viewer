FROM openjdk:8-jre-alpine

EXPOSE 8080

WORKDIR "/app"

ARG VERSION
ADD stpc-app-${VERSION}.jar /app/stpc.jar

# TODO: Dual layer approach for docker optimization (not so important for the exercise"
ENTRYPOINT ["/usr/bin/java", "-jar", "/app/stpc.jar"]

