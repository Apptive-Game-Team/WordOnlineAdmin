#!/bin/bash
set -euo pipefail

PAYLOAD_FILE="${1:-}"
SIMULATION_SERVER_URL="${SIMULATION_SERVER_URL:-http://localhost:8080}"
SIMULATION_HEALTH_PATH="${SIMULATION_HEALTH_PATH:-/healthcheck}"
SIMULATION_START_TIMEOUT_SECONDS="${SIMULATION_START_TIMEOUT_SECONDS:-60}"
CONTAINER_ID=""

if [ -z "$PAYLOAD_FILE" ]; then
  echo '{"error":"payload file argument is required"}' >&2
  exit 1
fi

if [ ! -f "$PAYLOAD_FILE" ]; then
  echo '{"error":"payload file not found"}' >&2
  exit 1
fi

if [ -z "${SIMULATION_LEFT_USER_ID:-}" ] || [ -z "${SIMULATION_RIGHT_USER_ID:-}" ]; then
  echo '{"error":"SIMULATION_LEFT_USER_ID and SIMULATION_RIGHT_USER_ID are required"}' >&2
  exit 1
fi

cleanup() {
  if [ -n "$CONTAINER_ID" ]; then
    docker rm -f "$CONTAINER_ID" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

read -r PARAMETER_PROFILE_ID RUNS_PER_MATCHUP < <(python3 - "$PAYLOAD_FILE" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as file:
    payload = json.load(file)

print(payload.get("parameterProfileId") or "", payload.get("runsPerMatchup") or 1)
PY
)

if [ -n "${SIMULATION_DOCKER_IMAGE:-}" ]; then
  CONTAINER_NAME="${SIMULATION_DOCKER_NAME:-word-online-simulation-$$}"
  # SIMULATION_DOCKER_ARGS is intentionally word-split so callers can pass
  # docker flags such as: -p 8080:8080 --env-file .env.simulation
  CONTAINER_ID="$(docker run -d --rm --name "$CONTAINER_NAME" ${SIMULATION_DOCKER_ARGS:-} "$SIMULATION_DOCKER_IMAGE")"
fi

deadline=$((SECONDS + SIMULATION_START_TIMEOUT_SECONDS))
until curl -fsS "${SIMULATION_SERVER_URL}${SIMULATION_HEALTH_PATH}" >/dev/null; do
  if [ "$SECONDS" -ge "$deadline" ]; then
    echo "{\"error\":\"simulation server did not become healthy\",\"url\":\"${SIMULATION_SERVER_URL}${SIMULATION_HEALTH_PATH}\"}" >&2
    exit 1
  fi
  sleep 1
done

REQUEST_FILE="$(mktemp)"
trap 'rm -f "$REQUEST_FILE"; cleanup' EXIT

python3 - "$REQUEST_FILE" "$SIMULATION_LEFT_USER_ID" "$SIMULATION_RIGHT_USER_ID" "$RUNS_PER_MATCHUP" "$PARAMETER_PROFILE_ID" <<'PY'
import json
import sys

request = {
    "leftUserId": int(sys.argv[2]),
    "rightUserId": int(sys.argv[3]),
    "matchCount": int(sys.argv[4]),
    "parameterProfileId": int(sys.argv[5]) if sys.argv[5] else None,
}

with open(sys.argv[1], "w", encoding="utf-8") as file:
    json.dump(request, file)
PY

AUTH_HEADER="${SIMULATION_AUTH_HEADER:-}"
if [ -z "$AUTH_HEADER" ] && [ -n "${SIMULATION_AUTHORIZATION:-}" ]; then
  AUTH_HEADER="Authorization: ${SIMULATION_AUTHORIZATION}"
fi
if [ -z "$AUTH_HEADER" ] && [ -n "${SIMULATION_JWT_FILE:-${JWT_FILE_PATH:-}}" ]; then
  JWT_FILE="${SIMULATION_JWT_FILE:-${JWT_FILE_PATH:-}}"
  if [ -f "$JWT_FILE" ]; then
    AUTH_HEADER="Authorization: $(tr -d '\n' < "$JWT_FILE")"
  fi
fi

curl_args=(
  -fsS
  -X POST
  "${SIMULATION_SERVER_URL}/api/admin/simulations/matchup"
  -H "Content-Type: application/json"
  --data "@${REQUEST_FILE}"
)

if [ -n "$AUTH_HEADER" ]; then
  curl_args+=(-H "$AUTH_HEADER")
fi

curl "${curl_args[@]}"
