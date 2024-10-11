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
FROM ghcr.io/pagopa/docker-base-springboot-openjdk17:v2.2.0@sha256:b866656c31f2c6ebe6e78b9437ce930d6c94c0b4bfc8e9ecc1076a780b9dfb18

COPY --from=build /app/target/pagopa-gpd-upload*.jar /app/app.jar

RUN chown -R nobody:nobody /app

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
