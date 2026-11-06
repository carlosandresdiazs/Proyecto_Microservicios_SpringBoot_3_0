# Build with repository root as context: docker build -f product-service/Dockerfile -t proyecto03/product-service:3.1 .
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -pl product-service -am clean package -DskipTests
RUN cp product-service/target/product-service-*.jar /tmp/app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /tmp/app.jar /app/app.jar
USER 10001:10001
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
