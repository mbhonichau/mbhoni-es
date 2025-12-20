# Koyeb Multi-Service Runner
FROM ubuntu:22.04

# Install Docker & Compose
RUN apt-get update && apt-get install -y \
    docker.io \
    docker-compose \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy your entire project
COPY . /app
WORKDIR /app

# Expose all ports
EXPOSE 8080 8081 8888 8761 3306

# Start all services with docker-compose
CMD ["docker-compose", "-f", "docker-compose-cloud.yml", "up", "-d"]
