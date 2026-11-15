FROM ghcr.io/graalvm/native-image-community:25 AS build

WORKDIR /app

COPY gradlew build.gradle.kts settings.gradle.kts /app/
COPY gradle /app/gradle
COPY src /app/src

RUN ./gradlew nativeCompile --no-daemon

FROM debian:13-slim

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/build/native/nativeCompile/auth /app/auth

ENV JAVA_OPTS="-Xmx128m -Xmn8m -Duser.timezone=Europe/Tallinn"

CMD ["sh", "-c", "exec /app/auth $JAVA_OPTS"]
