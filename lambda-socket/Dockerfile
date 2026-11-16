# Esta imagen solo sirve para compilar y comprobar el JAR. Lo que se despliega en Lambda
# es target/function.jar con Runtime java21 y este Handler:
#   com.kafka.socket.WebSocketConnectHandler::handleRequest
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/function.jar /app/function.jar
CMD ["sh", "-c", "ls -la /app/function.jar && java -version"]
