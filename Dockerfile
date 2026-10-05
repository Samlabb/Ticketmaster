FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd ./

RUN find . -name pom.xml -exec true \; 2>/dev/null || true
COPY */pom.xml ./*/ 2>/dev/null || true

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests dependency:go-offline || true

COPY . .

ARG MODULE
ARG VERSION=1.0.0-SNAPSHOT
ARG JAR_NAME

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl "${MODULE}" -am -DskipTests package

RUN set -eux; \
    SRC="/workspace/${MODULE}/target/${JAR_NAME:-${MODULE}-${VERSION}.jar}"; \
    if [ ! -f "$SRC" ]; then \
        SRC="$(find "/workspace/${MODULE}/target" -maxdepth 1 -name '*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' ! -name '*.original' | head -n 1)"; \
    fi; \
    cp "$SRC" /workspace/app.jar

FROM eclipse-temurin:21-jre AS runtime

RUN groupadd --system --gid 1001 app \
 && useradd  --system --uid 1001 --gid app --home /app --shell /usr/sbin/nologin app

WORKDIR /app

COPY --from=build --chown=app:app /workspace/app.jar /app/app.jar

USER app

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]