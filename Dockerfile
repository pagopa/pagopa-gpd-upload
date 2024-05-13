#
# Build stage
#
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -Dmaven.test.skip=true

#
# Package stage
#
FROM openjdk:17-alpine
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.25.1/opentelemetry-javaagent.jar /opt/opentelemetry-javaagent.jar

COPY --from=build /app/target/pagopa-gpd-upload*.jar /app/app.jar

RUN chown -R nobody:nobody /app

EXPOSE 8080
ENTRYPOINT ["java", "-javaagent:/opt/opentelemetry-javaagent.jar", "-jar", "/app/app.jar"]