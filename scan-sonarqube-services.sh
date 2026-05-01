#!/usr/bin/env bash
set -euo pipefail

SONAR_HOST_URL="${SONAR_HOST_URL:-http://sonarqube:9000}"
MAVEN_REPO="${MAVEN_REPO:-/workspace/.m2/repository}"
GENERATED_TOKEN_NAME=""

services=(
  "admin-service"
  "api-gateway"
  "application-service"
  "auth-service"
  "config-server"
  "document-service"
  "service-registry"
)

echo "Waiting for SonarQube at ${SONAR_HOST_URL}..."
for i in {1..60}; do
  if curl -fsS "${SONAR_HOST_URL}/api/system/status" >/tmp/sonar-status.json 2>/dev/null; then
    if grep -q '"status":"UP"' /tmp/sonar-status.json; then
      break
    fi
  fi
  if [ "$i" -eq 60 ]; then
    echo "SonarQube did not become ready in time."
    exit 1
  fi
  sleep 5
done

if [ -z "${SONAR_TOKEN:-}" ]; then
  SONAR_ADMIN_LOGIN="${SONAR_ADMIN_LOGIN:-admin}"
  SONAR_ADMIN_PASSWORD="${SONAR_ADMIN_PASSWORD:-admin}"
  GENERATED_TOKEN_NAME="finflow-docker-$(date +%s)"
  echo "SONAR_TOKEN is not set. Generating a temporary token with ${SONAR_ADMIN_LOGIN}..."
  token_response="$(curl -fsS -u "${SONAR_ADMIN_LOGIN}:${SONAR_ADMIN_PASSWORD}" -X POST "${SONAR_HOST_URL}/api/user_tokens/generate?name=${GENERATED_TOKEN_NAME}")" || {
    echo "Could not generate a Sonar token. Set SONAR_TOKEN, or set SONAR_ADMIN_LOGIN and SONAR_ADMIN_PASSWORD."
    exit 1
  }
  SONAR_TOKEN="$(printf '%s' "${token_response}" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')"
  if [ -z "${SONAR_TOKEN}" ]; then
    echo "Sonar token generation response did not include a token."
    exit 1
  fi
fi

cleanup() {
  if [ -n "${GENERATED_TOKEN_NAME}" ]; then
    curl -fsS -u "${SONAR_ADMIN_LOGIN}:${SONAR_ADMIN_PASSWORD}" -X POST "${SONAR_HOST_URL}/api/user_tokens/revoke?name=${GENERATED_TOKEN_NAME}" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

for service in "${services[@]}"; do
  echo "Building and scanning ${service}..."
  mvn \
    -f "/workspace/${service}/pom.xml" \
    "-Dmaven.repo.local=${MAVEN_REPO}" \
    clean verify sonar:sonar \
    "-Dsonar.host.url=${SONAR_HOST_URL}" \
    "-Dsonar.token=${SONAR_TOKEN}"
done

echo "Done. Open http://localhost:9000/projects to see each service."
