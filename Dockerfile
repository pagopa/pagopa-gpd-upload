#
# Build stage
#
FROM maven:3.8.4-openjdk-17-slim@sha256:150deb7b386bad685dcf0c781b9b9023a25896087b637c069a50c8019cab86f8 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -Dmaven.test.skip=true

#
# Package stage
#
FROM openjdk:17-alpine@sha256:4b6abae565492dbe9e7a894137c966a7485154238902f2f25e9dbd9784383d81

# https://github.com/microsoft/ApplicationInsights-Java/releases
ADD --chown=spring:spring https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.6.0/applicationinsights-agent-3.6.0.jar /applicationinsights-agent.jar
COPY --chown=spring:spring docker/applicationinsights.json ./applicationinsights.json

COPY --from=build /app/target/pagopa-gpd-upload*.jar /app/app.jar

RUN chown -R nobody:nobody /app

EXPOSE 8080
ENTRYPOINT ["java", "-javaagent:/applicationinsights-agent.jar", "-jar", "/app/app.jar"]
