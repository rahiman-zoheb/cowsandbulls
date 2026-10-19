# Build stage: compiles the jar inside the image, so a clean clone builds with
# `docker build backend` and no prior ./mvnw package (#24).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src

# Dependencies first, so editing source code doesn't re-download them.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q package -DskipTests

# Runtime stage: JRE only, maintained base image (#25), non-root user.
FROM eclipse-temurin:21-jre
RUN groupadd --system spring && useradd --system --gid spring --no-create-home spring
USER spring:spring
COPY --from=build /src/target/cowsandbulls-*.jar /app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
