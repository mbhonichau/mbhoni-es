FROM eclipse-temurin:21-jdk
WORKDIR /app

# Copy JAR
COPY target/*.jar app.jar

# Copy config folder
COPY config ./config

# Copy SSL trust store
COPY src/main/resources/github-truststore.jks ./github-truststore.jks

# Run with trust store
ENTRYPOINT ["java", \
  "-Djavax.net.ssl.trustStore=./github-truststore.jks", \
  "-Djavax.net.ssl.trustStorePassword=changeit", \
  "-jar", "app.jar"]
