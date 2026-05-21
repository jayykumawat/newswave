# Stage 1: Build the application using Maven with official Eclipse Temurin JDK 17
FROM maven:3.8.6-eclipse-temurin-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run the application using official Eclipse Temurin JRE 17 (Lightweight)
FROM eclipse-temurin:17-jre-alpine
COPY --from=build /target/news-app-0.0.1-SNAPSHOT.jar news-app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","news-app.jar"]