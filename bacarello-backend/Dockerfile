# ---- Stage 1: build del fat jar con il wrapper Gradle ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Cache delle dipendenze: copia prima i file di build
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Sorgenti + build
COPY src ./src
RUN ./gradlew --no-daemon buildFatJar

# ---- Stage 2: runtime leggero su JRE ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY --from=build /app/build/libs/*-all.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
