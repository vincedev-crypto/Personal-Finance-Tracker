# Stage 1: Build the application
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM openjdk:17-jdk-slim
WORKDIR /app
# This copies the compiled .war file and renames it to app.war
# We use *.war so it works even if your version number changes
COPY --from=build /app/target/*.war app.war

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.war"]