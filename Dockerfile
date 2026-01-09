# Stage 1: Build the application
# Updated to use the Eclipse Temurin version of Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run the application
# Replaced the deprecated 'openjdk' image with 'eclipse-temurin'
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
# This copies the compiled .war file and renames it to app.war
COPY --from=build /app/target/*.war app.war

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.war"]