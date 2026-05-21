# Stage 1: Build the application using Maven
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run the application using OpenJDK
FROM openjdk:17-jdk-slim
COPY --from=build /target/news-app-0.0.1-SNAPSHOT.jar news-app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","news-app.jar"]