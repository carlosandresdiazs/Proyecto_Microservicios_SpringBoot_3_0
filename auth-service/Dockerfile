# Build with repository root as context: docker build -f auth-service/Dockerfile -t proyecto03/auth-service:3.1 .
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -pl auth-service -am clean package -DskipTests
RUN cp auth-service/target/auth-service-*.jar /tmp/app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /tmp/app.jar /app/app.jar
USER 10001:10001
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
