#!/bin/sh
set -e

# Create logs directory if it doesn't exist
mkdir -p /app/logs

# Change the owner of the /app directory to the appuser
# This ensures the application has write permissions for logs
# and ownership of the files it manages.
chown -R appuser:appgroup /app/logs /app/app.jar

# ── Wait for PostgreSQL ──────────────────────────────────────────────────────
if [ -n "$DB_HOST" ]; then
    echo "[Entrypoint] Aguardando PostgreSQL em $DB_HOST:${DB_PORT:-5432}..."
    MAX_RETRIES=30
    RETRY=0
    while ! nc -z "$DB_HOST" "${DB_PORT:-5432}" 2>/dev/null; do
        RETRY=$((RETRY + 1))
        if [ $RETRY -ge $MAX_RETRIES ]; then
            echo "[Entrypoint] ERRO: PostgreSQL não respondeu após $MAX_RETRIES tentativas."
            exit 1
        fi
        echo "[Entrypoint] PostgreSQL não está pronto. Tentativa $RETRY/$MAX_RETRIES..."
        sleep 2
    done
    echo "[Entrypoint] PostgreSQL está pronto!"
fi

# ── Wait for RabbitMQ ─────────────────────────────────────────────────────────
RABBIT_HOST="${SPRING_RABBITMQ_HOST:-${RABBITMQ_HOST:-rabbitmq}}"
RABBIT_PORT="${SPRING_RABBITMQ_PORT:-${RABBITMQ_PORT:-5672}}"
if [ "$RABBIT_HOST" != "localhost" ] || [ -n "$RABBITMQ_HOST" ]; then
    echo "[Entrypoint] Aguardando RabbitMQ em $RABBIT_HOST:$RABBIT_PORT..."
    MAX_RETRIES=30
    RETRY=0
    while ! nc -z "$RABBIT_HOST" "$RABBIT_PORT" 2>/dev/null; do
        RETRY=$((RETRY + 1))
        if [ $RETRY -ge $MAX_RETRIES ]; then
            echo "[Entrypoint] ERRO: RabbitMQ não respondeu após $MAX_RETRIES tentativas."
            exit 1
        fi
        echo "[Entrypoint] RabbitMQ não está pronto. Tentativa $RETRY/$MAX_RETRIES..."
        sleep 2
    done
    echo "[Entrypoint] RabbitMQ está pronto!"
fi

# Execute the main command (CMD from Dockerfile) as the 'appuser'
# 'exec' replaces the shell with the Java process, and '"$@"' passes along any arguments.
exec gosu appuser:appgroup "$@"