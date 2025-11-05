FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY target/*.jar app.jar
COPY config ./config
ENTRYPOINT ["java", "-jar", "app.jar"]