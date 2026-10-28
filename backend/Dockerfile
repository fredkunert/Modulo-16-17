# syntax=docker/dockerfile:1.7

# ── Build ──────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /build

# Dependências primeiro: só são baixadas de novo quando o pom.xml muda.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src ./src
# Os testes rodam no CI; a imagem só empacota.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package \
 && cp target/*.jar app.jar \
 && java -Djarmode=layertools -jar app.jar extract --destination layers

# ── Runtime ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app

# Camadas da menos para a mais volátil, para aproveitar o cache do Docker.
COPY --from=build /build/layers/dependencies/ ./
COPY --from=build /build/layers/spring-boot-loader/ ./
COPY --from=build /build/layers/snapshot-dependencies/ ./
COPY --from=build /build/layers/application/ ./

USER app
EXPOSE 8083
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError" \
    TZ=America/Sao_Paulo

HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8083/api/produtos/health >/dev/null || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
