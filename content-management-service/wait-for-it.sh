#!/bin/sh
set -e

HOST="$1"
PORT="$2"
TIMEOUT=120

echo "Waiting for $HOST:$PORT..."

for i in $(seq 1 $TIMEOUT); do
  if nc -z "$HOST" "$PORT" > /dev/null 2>&1; then
    echo "$HOST:$PORT is UP after $i seconds"
    shift 2
    exec "$@"
  fi
  sleep 1
done

echo "TIMEOUT: $HOST:$PORT not available"
exit 1