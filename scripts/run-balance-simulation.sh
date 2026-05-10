#!/bin/bash
set -euo pipefail

PAYLOAD_FILE="${1:-}"

if [ -z "$PAYLOAD_FILE" ]; then
  echo '{"error":"payload file argument is required"}'
  exit 1
fi

if [ ! -f "$PAYLOAD_FILE" ]; then
  echo '{"error":"payload file not found"}'
  exit 1
fi

SIMULATION_BATCH_ID="$(uuidgen | tr '[:upper:]' '[:lower:]')"

# This wrapper is the seam between admin-server and the disposable game-server
# simulation runtime. The Docker boot + API invocation will be filled by the
# companion game-server work; for now the wrapper returns a stable batch id
# contract so admin-server can persist proposal state and aggregate when rows exist.
cat <<EOF
{"simulationBatchId":"$SIMULATION_BATCH_ID","status":"PENDING_EXTERNAL_RUNTIME","payloadFile":"$PAYLOAD_FILE"}
EOF
