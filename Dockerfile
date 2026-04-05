# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Create the final image
FROM eclipse-temurin:17-jre-focal

# Install gosu for secure user switching + curl for health checks
ENV GOSU_VERSION 1.17
RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends ca-certificates wget curl netcat-openbsd; \
    if ! command -v gpg; then \
    apt-get install -y --no-install-recommends gnupg dirmngr; \
    fi; \
    rm -rf /var/lib/apt/lists/*; \
    dpkgArch="$(dpkg --print-architecture | awk -F- '{ print $NF }')"; \
    wget -O /usr/local/bin/gosu "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$dpkgArch"; \
    wget -O /usr/local/bin/gosu.asc "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$dpkgArch.asc"; \
    export GNUPGHOME="$(mktemp -d)"; \
    gpg --batch --keyserver hkps://keys.openpgp.org --recv-keys B42F6819007F00F88E364FD4036A9C25BF357DD4; \
    gpg --batch --verify /usr/local/bin/gosu.asc /usr/local/bin/gosu; \
    gpgconf --kill all; \
    rm -rf "$GNUPGHOME" /usr/local/bin/gosu.asc; \
    chmod +x /usr/local/bin/gosu; \
    gosu --version; \
    gosu nobody true

# Arguments for user and group IDs to avoid permission issues
ARG UID=1000
ARG GID=1000

# Create a non-root user
RUN groupadd -g $GID -o appgroup && \
    useradd -m -u $UID -g $GID -o -s /bin/bash appuser

# Set working directory
WORKDIR /app

# Copy the JAR from the builder stage
COPY --from=builder /app/target/FinancasSpring-*.jar app.jar

# Copy the entrypoint script
COPY entrypoint.sh .
# Ensure the entrypoint script has Unix line endings and is executable
RUN sed -i 's/\r$//g' /app/entrypoint.sh && chmod +x /app/entrypoint.sh

EXPOSE 8080

# Set the entrypoint. It will run as root initially.
ENTRYPOINT ["/app/entrypoint.sh"]

# JVM flags otimizados para containers:
# -XX:+UseContainerSupport: respeita os limits de memória do Docker
# -XX:MaxRAMPercentage=75: usa até 75% da memória disponível
# -XX:InitialRAMPercentage=50: inicia com 50% da memória
# -Djava.security.egd: evita bloqueio em geração de random (startup mais rápido)
CMD ["java", \
     "-XX:+UseContainerSupport", \
     "-XX:MaxRAMPercentage=75.0", \
     "-XX:InitialRAMPercentage=50.0", \
     "-Djava.security.egd=file:/dev/./urandom", \
     "-jar", "app.jar"]