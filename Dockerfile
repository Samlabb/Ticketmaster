FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY . .

ARG MODULE
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl "${MODULE}" -am -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app
ARG MODULE
COPY --from=build "/workspace/${MODULE}/target/${MODULE}-1.0.0-SNAPSHOT.jar" app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]