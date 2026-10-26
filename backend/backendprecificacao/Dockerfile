# ---- Backend 2 (precificação) ----
# Build:  docker build -t produtos-precificacao ./backend/backendprecificacao   (a partir da raiz do repositório)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser
EXPOSE 8084
# Variáveis esperadas: DB_URL, DB_USER, DB_PASSWORD, JWT_ISSUER, JWT_JWKS_URI, JWT_AUDIENCE
# (opcionais: PORT, JWT_PERMISSIONS_CLAIM, CORS_ALLOWED_ORIGINS)
# O schema é criado/atualizado pelo serviço de catálogo: suba o catálogo antes.
ENTRYPOINT ["java","-jar","app.jar"]
