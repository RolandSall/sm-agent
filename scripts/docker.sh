#!/usr/bin/env bash
#
# Image lifecycle helper — build once (where the network works), then move it to your corporate server.
#
#   ./scripts/docker.sh build                 # build jar on host + package into the image
#   ./scripts/docker.sh save                  # -> scrum-mcp-server.tar.gz  (copy to an offline server)
#   ./scripts/docker.sh load                  # load scrum-mcp-server.tar.gz on the target machine
#   ./scripts/docker.sh push registry.corp/scrum-mcp-server:1.0
#   ./scripts/docker.sh pull registry.corp/scrum-mcp-server:1.0
#   ./scripts/docker.sh run                   # run as an HTTP server (MCP /mcp + REST /api) on PORT (default 8097)
#   ./scripts/docker.sh stdio                 # run one stdio MCP session (what .mcp.json uses)
#   ./scripts/docker.sh install-skills [dir]  # copy the capability skills out of the image onto THIS
#                                             # machine (default ~/.copilot/skills) — needed when the
#                                             # image was shipped without the repo
#   ./scripts/docker.sh logs | stop
#
set -eo pipefail
SELF="$(cd "$(dirname "$0")" && pwd)/$(basename "$0")"
cd "$(dirname "$0")/.."
IMAGE="scrum-mcp-server:latest"
PORT="${PORT:-8097}"
cmd="${1:-help}"; shift || true

need_env() { [ -f .env ] || { echo "Create .env first (cp .env.example .env)"; exit 1; }; }

case "$cmd" in
  build)
    ./gradlew :scrum-mcp-server:bootJar
    docker build -t "$IMAGE" .
    docker images "$IMAGE" --format '{{.Repository}}:{{.Tag}}  {{.Size}}'
    ;;
  save)
    docker save "$IMAGE" | gzip > scrum-mcp-server.tar.gz
    echo "Wrote $(du -h scrum-mcp-server.tar.gz | cut -f1) -> scrum-mcp-server.tar.gz"
    echo "Copy it over:  scp scrum-mcp-server.tar.gz user@corp-server:/tmp/"
    ;;
  load)
    gunzip -c scrum-mcp-server.tar.gz | docker load
    ;;
  push)
    ref="${1:?usage: docker.sh push <registry.host/path:tag>}"
    docker tag "$IMAGE" "$ref"
    docker push "$ref"
    echo "Pushed $ref — pull it on the server with: ./scripts/docker.sh pull $ref"
    ;;
  pull)
    ref="${1:?usage: docker.sh pull <registry.host/path:tag>}"
    docker pull "$ref"
    docker tag "$ref" "$IMAGE"
    ;;
  run)
    need_env
    docker rm -f scrum >/dev/null 2>&1 || true
    docker run -d --name scrum --env-file .env -p "${PORT}:8097" "$IMAGE"
    echo "Running. MCP (HTTP): http://localhost:${PORT}/mcp   REST: http://localhost:${PORT}/api/hygiene"
    echo "Point ./scrum at it:  echo 'SCRUM_API_URL=http://localhost:${PORT}' > .scrum-env"
    ;;
  stdio)
    need_env
    docker run -i --rm --env-file .env "$IMAGE" \
      --spring.ai.mcp.server.stdio=true --spring.main.web-application-type=none --spring.main.banner-mode=off
    ;;
  install-skills)
    # Copy the capability skills OUT of the image and onto this machine's disk, where the harness
    # can discover them. Needed on any machine that got the image without cloning the repo — the
    # skills must sit beside the HARNESS (the user's laptop), not beside the server.
    #
    # Default target is the personal scope, which follows the user into any project they open.
    # Pass a path to install into one repo instead, e.g.:
    #   ./scripts/docker.sh install-skills ~/work/my-repo/.github/skills
    # Skills and the agent go to DIFFERENT directories — a harness scans each separately.
    base="${1:-$HOME/.copilot}"
    skills_dest="$base/skills"
    agents_dest="$base/agents"
    docker image inspect "$IMAGE" >/dev/null 2>&1 || {
      echo "Image $IMAGE not found — run './scripts/docker.sh load' (or 'pull') first." >&2; exit 1; }
    mkdir -p "$skills_dest" "$agents_dest"
    docker run --rm -v "$skills_dest:/out" --entrypoint sh "$IMAGE" -c 'cp -r /app/skills/. /out/'
    docker run --rm -v "$agents_dest:/out" --entrypoint sh "$IMAGE" -c 'cp -r /app/agents/. /out/'
    echo "Installed $(find "$skills_dest" -name SKILL.md | wc -l | tr -d ' ') skills -> $skills_dest"
    echo "Installed $(find "$agents_dest" -name '*.agent.md' | wc -l | tr -d ' ') agent   -> $agents_dest"
    echo
    echo "Restart your harness to pick them up."
    echo "  Copilot: pick 'Scrum Master' from the agents dropdown (or type /agents)."
    echo "  Skills load on their own — Copilot reads ~/.copilot/skills and <repo>/.github/skills;"
    echo "  Claude Code reads ~/.claude/skills and <repo>/.claude/skills."
    echo "Copilot Chat on github.com supports neither skills nor custom agents — that surface"
    echo "needs .github/copilot-instructions.md committed to the repo instead."
    ;;
  logs) docker logs -f scrum ;;
  stop) docker rm -f scrum >/dev/null 2>&1 && echo "stopped" || echo "not running" ;;
  *)
    grep '^#' "$SELF" | grep -v '^#!' | sed 's/^# \{0,1\}//'
    ;;
esac
