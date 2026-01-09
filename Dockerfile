# Stage 1: Build the application
# Changed to Java 21 to match your project requirement
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run the application
# Changed to Java 21
FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
# This copies the compiled .war file and renames it to app.war
COPY --from=build /app/target/*.war app.war

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.war"]