FROM eclipse-temurin:21-jdk
WORKDIR /app

COPY wait-for-it.sh /app/wait-for-it.sh
RUN chmod +x /app/wait-for-it.sh

COPY target/*.jar app.jar

ENTRYPOINT ["/app/wait-for-it.sh", "admin-service:8081", "--timeout=180", "--", "java", "-jar", "app.jar"]