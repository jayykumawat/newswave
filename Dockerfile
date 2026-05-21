# Stage 1: Build compilation
FROM maven:3.8.6-eclipse-temurin-17 AS build
COPY . .
RUN mvn clean package -DskipTests -Dmaven.test.skip=true

# Stage 2: Runtime Environment
FROM eclipse-temurin:17-jre-alpine
COPY --from=build /target/news-app-0.0.1-SNAPSHOT.jar news-app.jar
EXPOSE 8080
# Environment variable initialization support
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java","-jar","news-app.jar"]