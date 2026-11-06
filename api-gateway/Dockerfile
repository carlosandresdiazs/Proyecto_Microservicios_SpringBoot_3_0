# Build with repository root as context: docker build -f api-gateway/Dockerfile -t proyecto03/api-gateway:3.1 .
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -pl api-gateway -am clean package -DskipTests
RUN cp api-gateway/target/api-gateway-*.jar /tmp/app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /tmp/app.jar /app/app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
