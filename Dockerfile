# One image for the whole game: the backend serves the built frontend from the
# same origin, so the browser needs no CORS and no proxy (#46).

# 1. Build the frontend.
FROM node:24-alpine AS frontend
WORKDIR /src/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# 2. Build the backend jar with the frontend inside it, as static resources.
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /src/backend
# Dependencies first, so editing source code doesn't re-download them.
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY backend/src src
COPY --from=frontend /src/frontend/build src/main/resources/static
RUN ./mvnw -B -q package -DskipTests

# 3. Runtime: JRE only, non-root user, games persisted under /data (#27).
FROM eclipse-temurin:21-jre
RUN groupadd --system spring && useradd --system --gid spring --no-create-home spring \
    && mkdir /data && chown spring:spring /data
VOLUME /data
ENV SPRING_PROFILES_ACTIVE=prod
USER spring:spring
COPY --from=backend /src/backend/target/cowsandbulls-*.jar /app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
