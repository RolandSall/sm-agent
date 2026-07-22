#!/usr/bin/env bash
#
# Smoke-test OUR capabilities against the running server (real Jira behind it).
# Start the server first:  ./setup.sh   OR   ./scripts/docker.sh run
# Uses the ./scrum wrapper, which reads the port from .scrum-env (or SCRUM_API_URL).
#
#   ./scripts/smoke.sh [SAMPLE-ISSUE-KEY]
#
set -uo pipefail
cd "$(dirname "$0")/.."
[ -f .env ] && { set -a; . ./.env; set +a; }
ISSUE="${1:-${JIRA_PROJECT_KEY:-PROJ}-1}"

run() { echo; echo "### $*"; "$@" || echo "(request failed — is the server up? ./scripts/docker.sh run)"; }

run ./scrum hygiene
run ./scrum wip --per-assignee 3 --team 15
run ./scrum worklog
run ./scrum velocity 6
run ./scrum release-queue "Ready for Release" "Ready to Test"
run ./scrum risk --committed 40 --available 35 --sp-high 8 --blockers-high 1
run ./scrum deps "$ISSUE" Blocks Dependency
run ./scrum issue "$ISSUE"

echo
echo "Eyeball the numbers against Jira. If story points / acceptance criteria / logged hours look wrong"
echo "or empty, your custom-field IDs are off — run ./scripts/check-jira.sh and fix application.yml."
