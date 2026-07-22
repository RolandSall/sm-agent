#!/usr/bin/env bash
#
# One-shot setup: build the image, wire up MCP (stdio) and the REST fallback (dynamic port).
# Run this after cloning:  ./setup.sh
#
set -eo pipefail
cd "$(dirname "$0")"
ROOT="$(pwd)"

command -v docker >/dev/null 2>&1 || { echo "Docker is required — install Docker Desktop first." >&2; exit 1; }
docker compose version >/dev/null 2>&1 || { echo "Docker Compose v2 is required (comes with Docker Desktop)." >&2; exit 1; }

# 1. Jira credentials
if [ ! -f .env ]; then
  cp .env.example .env
  echo "Created .env — fill in JIRA_BASE_URL, JIRA_TOKEN, JIRA_PROJECT_KEYS (comma-separated), then re-run ./setup.sh"
  exit 1
fi

# 2. Build the jar on the HOST, then package it. We build Java on the host (not in Docker) because
#    in-container Gradle/dependency downloads are slow or blocked on many corporate Docker networks.
if ! ./gradlew :scrum-mcp-server:bootJar --console=plain; then
  echo "Jar build failed — a JDK 21 must be installed and visible to Gradle (set JAVA_HOME, or see gradle.properties)." >&2
  exit 1
fi
echo "==> Packaging the jar into the slim image…"
docker compose build

# 3. MCP over stdio — the harness launches the image per session (no port). Reads .env for creds.
echo "==> Writing .mcp.json (MCP over stdio via docker run -i)…"
cat > .mcp.json <<JSON
{
  "mcpServers": {
    "scrum": {
      "command": "docker",
      "args": [
        "run", "-i", "--rm",
        "--env-file", "${ROOT}/.env",
        "scrum-mcp-server:latest",
        "--spring.ai.mcp.server.stdio=true",
        "--spring.main.web-application-type=none",
        "--spring.main.banner-mode=off"
      ]
    }
  }
}
JSON

# 4. REST fallback — start the container on a DYNAMIC host port and record it for ./scrum
echo "==> Starting the REST container on a free port…"
docker compose up -d
PORT="$(docker compose port scrum 8097 | sed 's/.*://')"
if [ -z "$PORT" ]; then echo "Could not determine the mapped port; is the container healthy? (docker compose logs)" >&2; exit 1; fi
echo "SCRUM_API_URL=http://localhost:${PORT}" > .scrum-env

cat <<EOF

Done.

  MCP (primary): restart your harness (Claude Code / Copilot) in this folder — it will launch the
                 image over stdio and expose the tools. Check with /mcp.

  REST (fallback, when MCP is disabled): the API is on http://localhost:${PORT}
                 Use the wrapper — it reads the port from .scrum-env, so no hardcoded ports:
                   ./scrum hygiene
                   ./scrum risk --committed 40 --available 35

  Stop the REST container:  docker compose down
EOF
