#
# Build stage
#
FROM maven:3.8.2-openjdk-17-slim AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package -Dmaven.test.skip=true

#
# Package stage
#
FROM openjdk:17-alpine
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.25.1/opentelemetry-javaagent.jar .
COPY --from=build /home/app/target/pagopa-gpd-upload*.jar /usr/local/lib/app.jar
RUN true
EXPOSE 8080
ENTRYPOINT ["java","-jar","/usr/local/lib/app.jar"]
