#!/usr/bin/env bash
#
# ============================================================================================
#  READ-ONLY Jira diagnostic.  It issues ONLY HTTP GET requests — it never creates, edits, or
#  deletes anything in Jira, and it never prints your token. Safe to run.
#
#  It verifies that the server's assumptions match YOUR Jira, and writes a full report to
#  scripts/jira-check-report.txt — hand THAT file back to Claude.
#
#  Usage:  ./scripts/check-jira.sh [SAMPLE-ISSUE-KEY]
#  Reads:  .env  (JIRA_BASE_URL, JIRA_TOKEN, JIRA_PROJECT_KEY, optional JIRA_BOARD_ID)
#  Needs:  curl  (jq or python3 optional — used only to pretty-print; raw JSON is saved regardless)
# ============================================================================================
set -uo pipefail    # NOT -e: keep going past failures so partial results are still captured
cd "$(dirname "$0")/.."
[ -f .env ] && { set -a; . ./.env 2>/dev/null; set +a; }

REPORT="scripts/jira-check-report.txt"
: > "$REPORT" 2>/dev/null || REPORT="/tmp/jira-check-report.txt"
# Mirror everything (stdout+stderr) to the report file as well as the console.
exec > >(tee "$REPORT") 2>&1

cat <<'BANNER'
=================== SCRUM-AGENT JIRA DIAGNOSTIC (READ-ONLY) ===================
FOR THE AGENT RUNNING THIS:
  * This script ONLY does HTTP GET. It makes NO changes to Jira. Do not run any other
    commands, and do not modify Jira, files, or config.
  * Run it to completion even if some sections fail — partial output is useful.
  * Reading the HTTP status on each section:
       2xx  -> OK.
       401/403 -> the PAT is wrong or lacks permission. Note it, continue.
       404  -> that endpoint isn't available on this Jira. Note it, continue (an ALTERNATIVE is printed).
       000 / curl error -> can't reach Jira (VPN/DNS/proxy). Stop and report this.
  * When done, return the file  scripts/jira-check-report.txt  to Claude. It contains no secrets.
==============================================================================
BANNER

# ---- required config (never prints the token) ----
missing=0
for v in JIRA_BASE_URL JIRA_TOKEN JIRA_PROJECT_KEY; do
  if [ -z "${!v:-}" ]; then echo "MISSING: $v (set it in .env)"; missing=1; fi
done
[ "$missing" = 1 ] && { echo "Fill .env and re-run."; exit 1; }

J="${JIRA_BASE_URL%/}"
KEY="$JIRA_PROJECT_KEY"
SAMPLE_ISSUE="${1:-}"
AUTH=(-H "Authorization: Bearer $JIRA_TOKEN" -H "Accept: application/json")
HAVE_JQ=0; command -v jq  >/dev/null 2>&1 && HAVE_JQ=1
HAVE_PY=0; command -v python3 >/dev/null 2>&1 && HAVE_PY=1
echo "Base: $J   Project: $KEY   jq:$( [ $HAVE_JQ = 1 ] && echo yes || echo no )   (token hidden)"

pretty() { if [ $HAVE_JQ = 1 ]; then jq .; elif [ $HAVE_PY = 1 ]; then python3 -m json.tool 2>/dev/null || cat; else cat; fi; }
show() {  # $1 = jq filter; falls back to full pretty JSON when jq is absent
  if [ $HAVE_JQ = 1 ]; then jq "$1" 2>/dev/null || { echo "(could not filter; raw:)"; pretty; }
  else echo "(jq not installed — raw JSON):"; pretty; fi
}
hint() { case "$1" in
    2*) echo "OK";;
    401|403) echo "(auth/permission — check the PAT)";;
    404) echo "(not available on this Jira — see ALTERNATIVE)";;
    000) echo "(NETWORK/VPN error reaching Jira)";;
    *) echo "";; esac; }

LAST_CODE=""; LAST_BODY=""
call() {  # $1 = path
  local resp
  if ! resp=$(curl -sS -w $'\n%{http_code}' "${AUTH[@]}" "$J$1" 2>&1); then
    LAST_CODE=000; LAST_BODY=""; echo "HTTP 000  GET $1  $(hint 000): $resp"; return; fi
  LAST_CODE=${resp##*$'\n'}; LAST_BODY=${resp%$'\n'*}
  echo "HTTP $LAST_CODE  GET $1  $(hint "$LAST_CODE")"
}
callg() {  # $1 = path, rest = curl -G args (e.g. --data-urlencode "jql=...")
  local path="$1"; shift; local resp
  if ! resp=$(curl -sS -G -w $'\n%{http_code}' "${AUTH[@]}" "$@" "$J$path" 2>&1); then
    LAST_CODE=000; LAST_BODY=""; echo "HTTP 000  GET $path  $(hint 000): $resp"; return; fi
  LAST_CODE=${resp##*$'\n'}; LAST_BODY=${resp%$'\n'*}
  echo "HTTP $LAST_CODE  GET $path  $(hint "$LAST_CODE")"
}

echo; echo "### 0. Auth / connectivity"
call "/rest/api/2/myself"; echo "$LAST_BODY" | show '{name, displayName, active}'

echo; echo "### 1. Custom fields (story points / acceptance criteria / logged hours)"
call "/rest/api/2/field"
echo "$LAST_BODY" | show '[.[] | select(.name|test("story point|acceptance|logged|worklog|remaining|sprint";"i")) | {id, name}]'
if [ $HAVE_JQ = 0 ]; then echo ">> jq not installed: the FULL field list is above as raw JSON — Claude will read the ids from it."; fi

echo; echo "### 2. Agile board(s) for $KEY"
call "/rest/agile/1.0/board?projectKeyOrId=$KEY"; echo "$LAST_BODY" | show '.values // .'
BOARD_ID="${JIRA_BOARD_ID:-}"
[ -z "$BOARD_ID" ] && [ $HAVE_JQ = 1 ] && BOARD_ID=$(echo "$LAST_BODY" | jq -r '.values[0].id // empty' 2>/dev/null)
echo "Board id in use: ${BOARD_ID:-<unknown — pass JIRA_BOARD_ID or read section 2 above>}"
if [ "$LAST_CODE" = 404 ]; then echo ">> ALTERNATIVE: /rest/agile/1.0 is disabled — this may not be Jira Software with boards. Velocity/sprint tools won't work; tell Claude."; fi

echo; echo "### 3. Statuses + categories (WIP uses category; release_queue needs exact names)"
call "/rest/api/2/project/$KEY/statuses"
echo "$LAST_BODY" | show '[.[].statuses[] | {name, category: .statusCategory.name}] | unique_by(.name)'

echo; echo "### 4. Issue link types (dependency_radar needs these names)"
call "/rest/api/2/issueLinkType"; echo "$LAST_BODY" | show '[.issueLinkTypes[].name]'

echo; echo "### 5. Active sprint issues (hygiene / wip / worklog / risk need an open sprint)"
callg "/rest/api/2/search" --data-urlencode "jql=project=$KEY AND sprint in openSprints()" --data-urlencode "maxResults=5" --data-urlencode "fields=summary,status,assignee"
echo "$LAST_BODY" | show '{total, sample: [.issues[]? | {key, status: .fields.status.name, assignee: .fields.assignee.displayName}]}'
[ -z "$SAMPLE_ISSUE" ] && [ $HAVE_JQ = 1 ] && SAMPLE_ISSUE=$(echo "$LAST_BODY" | jq -r '.issues[0].key // empty' 2>/dev/null)
if [ "$LAST_CODE" = 400 ]; then echo ">> ALTERNATIVE: 'sprint in openSprints()' rejected — your Jira may lack Software/JQL sprints; tell Claude and we'll switch the query."; fi

echo; echo "### 6. VELOCITY — the unofficial greenhopper endpoint sprint_velocity relies on"
if [ -n "$BOARD_ID" ]; then
  call "/rest/greenhopper/1.0/rapid/charts/velocity?rapidViewId=$BOARD_ID"
  if [ "$LAST_CODE" = 200 ]; then
    echo "OK — greenhopper velocity works."; echo "$LAST_BODY" | show '{sprints: [.sprints[]? .name], entries: (.velocityStatEntries|keys)}'
  else
    echo ">> greenhopper velocity NOT available (HTTP $LAST_CODE)."
    echo ">> ALTERNATIVE (supported Agile API) — closed sprints we could compute velocity from:"
    call "/rest/agile/1.0/board/$BOARD_ID/sprint?state=closed"; echo "$LAST_BODY" | show '[.values[]? | {id, name}]'
    echo ">> Tell Claude 'greenhopper velocity failed' and it will switch sprint_velocity to the Agile API."
  fi
else echo "skipped — no board id"; fi

echo; echo "### 7. LOGGED WORK — custom field vs Jira native worklog (worklog_summary reads a custom field today)"
if [ -n "$SAMPLE_ISSUE" ]; then
  call "/rest/api/2/issue/$SAMPLE_ISSUE?fields=worklog,timetracking"
  echo "$LAST_BODY" | show '{issue: "'"$SAMPLE_ISSUE"'", nativeWorklogEntries: (.fields.worklog.total // 0), nativeTimeSpentSeconds: .fields.timetracking.timeSpentSeconds, originalEstimateSeconds: .fields.timetracking.originalEstimateSeconds, remainingEstimateSeconds: .fields.timetracking.remainingEstimateSeconds}'
  echo ">> nativeWorklogEntries > 0  => you use Jira's NATIVE worklog (worklog_summary needs a code change to sum it)."
  echo ">> otherwise your logged hours are a custom field => use its id (section 1) as scrum.jira.fields.logged-hours."
else echo "skipped — no sample issue. Re-run as: ./scripts/check-jira.sh <PROJ-123>"; fi

echo; echo "### 8. Raw issue shape the metric/dependency code parses (fields + links + changelog)"
if [ -n "$SAMPLE_ISSUE" ]; then
  call "/rest/api/2/issue/$SAMPLE_ISSUE?expand=renderedFields,changelog"
  echo "$LAST_BODY" | show '{key, status: .fields.status.name, category: .fields.status.statusCategory.name, links: [.fields.issuelinks[]? | {type: .type.name, out: .outwardIssue.key, in: .inwardIssue.key}], statusChanges: [.changelog.histories[]? as $h | $h.items[]? | select(.field=="status") | {when: $h.created, from: .fromString, to: .toString}]}'
else echo "skipped — no sample issue"; fi

echo; echo "=============================================================================="
echo "DONE (read-only). Full report saved to: $REPORT"
echo "Return that file to Claude — it will give you the exact application.yml field ids,"
echo "confirm or replace the velocity path, and say whether logged-work needs a code change."
echo "=============================================================================="
