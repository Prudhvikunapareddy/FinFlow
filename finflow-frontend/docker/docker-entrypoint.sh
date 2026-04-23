#!/bin/sh
set -eu

API_BASE_URL="${FINFLOW_API_BASE_URL:-http://localhost:9083}"

cat > /usr/share/nginx/html/config.js <<EOF
window.__FINFLOW_CONFIG__ = {
  apiBaseUrl: "${API_BASE_URL}"
};
EOF
