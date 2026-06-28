# syntax=docker/dockerfile:1

############################################################
# Étape 1 — Build du frontend React (Vite)
############################################################
FROM node:20-alpine AS frontend
WORKDIR /frontend
COPY banking-frontend/package*.json ./
RUN npm ci
COPY banking-frontend/ ./
RUN npm run build

############################################################
# Étape 2 — Build du jar Spring Boot
#   Le frontend compilé est injecté dans les ressources
#   statiques : Spring sert l'app et l'API sur le même port.
############################################################
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app
COPY banking-server/ ./
# On repart d'un dossier static propre, puis on y place le frontend.
RUN rm -rf src/main/resources/static && mkdir -p src/main/resources/static
COPY --from=frontend /frontend/dist/ ./src/main/resources/static/
# Les tests tournent dans la CI (étape suivante), pas dans l'image.
# Le cache BuildKit garde le dépôt Maven (.m2) entre deux builds.
RUN --mount=type=cache,target=/root/.m2 mvn -B clean package -DskipTests

############################################################
# Étape 3 — Image d'exécution légère (JRE seul)
############################################################
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && useradd -r -u 1001 spring
# Un seul .jar exécutable est produit (le .jar.original est ignoré).
COPY --from=backend /app/target/banking-server-*.jar app.jar
USER spring
EXPOSE 8080
# MaxRAMPercentage : la JVM s'adapte à la RAM allouée au conteneur.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
