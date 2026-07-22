package localdev;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * Rich, prod-like Jira fixtures for the local dev harness ({@link LocalDevRunner}).
 *
 * <p>One coherent active sprint ("RIO PI-3 Sprint 4", its window straddling today) whose issues
 * deliberately exercise EVERY scrum skill and its edge cases in a single dataset: hygiene gaps,
 * WIP over-limit, release-queue buckets, worklog overrun / no-timetracking, velocity history,
 * scope add-and-remove-after-start, cross-team blockers (open vs resolved), delivery-risk
 * LOW/MEDIUM/HIGH boundaries, and sprint-flow stuck/stalled/reopened.
 *
 * <p>Each story is modelled ONCE ({@link Story}) and rendered into the different Jira wire shapes
 * the {@code JiraClient} requests (full {@code /search}, the two {@code /sprint/{id}/issue}
 * changelog projections, and single-issue GETs) so a story that is unestimated for hygiene is also
 * unestimated for risk, etc. Dates are computed relative to {@code now} at startup because
 * {@code activeSprint()} selects by "now is inside the window" and flow/scope compare against now.
 *
 * <p>Field ids mirror {@code application-local.yml} / prod: story points {@code customfield_10043},
 * acceptance criteria {@code customfield_11977} then {@code customfield_34061}, and logged time via
 * native {@code timetracking.timeSpentSeconds} (no logged-hours custom field).
 */
final class LocalJiraFixtures {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter JIRA_OFFSET =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").withZone(ZoneOffset.UTC);

    private static final int BOARD_ID = 62;
    private static final long SPRINT_ID = 5001;

    private static final String SP = "customfield_10043";
    private static final String AC1 = "customfield_11977";
    private static final String AC2 = "customfield_34061";
    private static final String EPIC = "customfield_12471";   // epic-link
    private static final String DEVJOB = "customfield_24665";  // dev-job effort estimate on the epic

    private LocalJiraFixtures() {
    }

    // ------------------------------------------------------------------------------------------
    // Registration
    // ------------------------------------------------------------------------------------------

    /** Register every stub on a live in-JVM MockServer (used by {@link LocalDevRunner}). */
    static void register(ClientAndServer mock) {
        for (Stub s : stubs(Instant.now())) {
            var req = request().withMethod("GET").withPath(s.path());
            if (s.param() != null) {
                req = req.withQueryStringParameter(s.param(), s.regex());
            }
            mock.when(req).respond(response().withStatusCode(200).withBody(s.body(), MediaType.APPLICATION_JSON));
        }
    }

    /**
     * Emit the SAME stubs as a MockServer initialization JSON array, for a standalone (Docker)
     * MockServer container loaded via {@code MOCKSERVER_INITIALIZATION_JSON_PATH}. Dates are baked
     * at generation time (regenerate if the sprint window drifts out of "today").
     */
    static String initializerJson(Instant now) {
        List<Object> entries = new ArrayList<>();
        for (Stub s : stubs(now)) {
            Map<String, Object> httpRequest = obj("method", "GET", "path", s.path());
            if (s.param() != null) {
                httpRequest.put("queryStringParameters", obj(s.param(), List.of(s.regex())));
            }
            Map<String, Object> httpResponse = obj("statusCode", 200);
            httpResponse.put("headers", obj("content-type", List.of("application/json")));
            httpResponse.put("body", parse(s.body())); // embed as a JSON object so MockServer serves it verbatim
            entries.add(obj("httpRequest", httpRequest, "httpResponse", httpResponse));
        }
        return writePretty(entries);
    }

    /** Pretty-print variant used only for the committed initializer file, so its diff stays reviewable. */
    private static String writePretty(Object value) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize fixture JSON", e);
        }
    }

    /**
     * The single source of truth for the mock: one list of (path, optional query matcher, body).
     * Query values are regexes (full-match), matched the same way whether registered live or loaded
     * from the initializer JSON.
     */
    private static List<Stub> stubs(Instant now) {
        Instant start = now.minus(Duration.ofDays(5));   // 2-week sprint, we're on day 5
        Instant end = now.plus(Duration.ofDays(9));
        List<Story> sprint = sprintStories(now, start);
        List<Story> queue = releaseQueueStories();

        List<Stub> out = new ArrayList<>();

        // 1) Active-sprint resolution: the real "RIO" sprint (window contains today) + two decoys —
        //    one "RIO" future sprint (fails the date window) and one non-RIO sprint whose window
        //    DOES contain today (fails the name filter). Exercises both narrowing branches.
        out.add(new Stub("/rest/agile/1.0/board/" + BOARD_ID + "/sprint", "state", "active", sprintList(
                sprintNode(SPRINT_ID, "RIO PI-3 Sprint 4", "active", start, end),
                sprintNode(5002, "RIO PI-3 Sprint 5", "active", end, end.plus(Duration.ofDays(14))),
                sprintNode(5099, "PLTF Hardening", "active", now.minus(Duration.ofDays(3)), now.plus(Duration.ofDays(11))))));
        out.add(new Stub("/rest/agile/1.0/board/" + BOARD_ID + "/sprint", "state", "closed", sprintList(
                sprintNode(4900, "RIO PI-3 Sprint 3", "closed", now.minus(Duration.ofDays(19)), now.minus(Duration.ofDays(6))),
                sprintNode(4800, "RIO PI-3 Sprint 2", "closed", now.minus(Duration.ofDays(33)), now.minus(Duration.ofDays(20))))));

        // 2) Active-sprint issue search (hygiene, wip, worklog, estimate-variance, risk, sprint-deps).
        out.add(new Stub("/rest/api/2/search", "jql", ".*sprint = .*", searchResponse(sprint)));
        // 3) Release queue (project+status search, NOT the active sprint).
        out.add(new Stub("/rest/api/2/search", "jql", ".*status in.*", searchResponse(queue)));
        // 4) Velocity (greenhopper chart): >6 sprints so the "keep last N" trim fires; one sprint
        //    present in the list but missing from the stats map (defaults to 0).
        out.add(new Stub("/rest/greenhopper/1.0/rapid/charts/velocity", "rapidViewId", String.valueOf(BOARD_ID), velocity()));
        // 5) Single sprint (read startDate for scope-change's "after start" cutoff).
        out.add(new Stub("/rest/agile/1.0/sprint/" + SPRINT_ID, null, null, sprintOnly(SPRINT_ID, "RIO PI-3 Sprint 4", "active", start, end)));
        // 6) Two projections of the sprint's issues, discriminated by the `fields` query param.
        out.add(new Stub("/rest/agile/1.0/sprint/" + SPRINT_ID + "/issue", "fields", ".*customfield_10043.*", agileScope(sprint)));
        out.add(new Stub("/rest/agile/1.0/sprint/" + SPRINT_ID + "/issue", "fields", ".*updated.*", agileFlow(sprint)));
        // 7) Single-issue GETs. expand=renderedFields (root fetch / get_issue) vs
        //    expand=renderedFields,changelog (cross-team link targets for the dependency radar).
        out.add(new Stub("/rest/api/2/issue/VAL-101", "expand", "renderedFields", singleIssue(byKey(sprint, "VAL-101"), false)));
        out.add(new Stub("/rest/api/2/issue/VAL-104", "expand", "renderedFields", singleIssue(byKey(sprint, "VAL-104"), false)));
        out.add(new Stub("/rest/api/2/issue/PLTF-88", "expand", "renderedFields,changelog", singleIssue(pltf88(now), true)));
        out.add(new Stub("/rest/api/2/issue/DATA-42", "expand", "renderedFields,changelog", singleIssue(data42(now), true)));
        out.add(new Stub("/rest/api/2/issue/PLTF-91", "expand", "renderedFields,changelog", singleIssue(pltf91(now), true)));
        out.add(new Stub("/rest/api/2/issue/PLTF-90", "expand", "renderedFields,changelog", singleIssue(pltf90(now), true)));

        // 8) feature_effort_rollup: the epics linked from the sprint (via epic-link customfield_12471 on
        //    the stories below), each fetched as an issue (summary + dev-job) plus its children via the
        //    "Epic Link" = <key> JQL. Matched on fields=...customfield_24665 (epic GET) and the epic key
        //    inside the jql (children search) so they don't collide with get_issue / sprint / release stubs.
        out.add(new Stub("/rest/api/2/issue/VAL-26930", "fields", ".*customfield_24665.*", epicIssue("VAL-26930", "Payments resilience", 21.0)));
        out.add(new Stub("/rest/api/2/issue/VAL-26931", "fields", ".*customfield_24665.*", epicIssue("VAL-26931", "KYC & tax uplift", 13.0)));
        out.add(new Stub("/rest/api/2/search", "jql", ".*Epic Link.*VAL-26930.*", searchResponse(byKeys(sprint, "VAL-101", "VAL-110", "VAL-143"))));
        out.add(new Stub("/rest/api/2/search", "jql", ".*Epic Link.*VAL-26931.*", searchResponse(byKeys(sprint, "VAL-161", "VAL-162"))));
        return out;
    }

    /** An Epic issue node as fetched by feature_effort ({@code fields=summary,customfield_24665}). */
    private static String epicIssue(String key, String summary, double devJobPoints) {
        return write(obj("key", key, "fields", obj("summary", summary, DEVJOB, devJobPoints)));
    }

    /** One stub: a GET path, an optional single query-param regex matcher, and the JSON body to return. */
    private record Stub(String path, String param, String regex, String body) {
    }

    // ------------------------------------------------------------------------------------------
    // The dataset
    // ------------------------------------------------------------------------------------------

    private static List<Story> sprintStories(Instant now, Instant start) {
        List<Story> s = new ArrayList<>();

        // --- Hygiene edge cases -------------------------------------------------------------
        // Clean + the cross-team dependency/risk root (open blocker -> HIGH risk).
        s.add(story("VAL-101", "Payment retry orchestration").status("In Progress", "In Progress")
                .assignee("Alice Ng").labels("payments", "resilience").sp(5).epic("VAL-26930").ac1("Given a failed payment, when retried up to 3x, then it settles or dead-letters.")
                .fix("VAL 24.3").desc("<p>Orchestrate idempotent payment retries with backoff.</p>")
                .link("Blocks", "outward", "PLTF-88", "In Progress")
                .link("Dependency", "outward", "DATA-42", "Resolved")
                .link("Relates", "outward", "PLTF-90", "Open")
                .link("Blocks", "outward", "VAL-777", "To Do"));
        s.add(story("VAL-102", "Refund ledger reconciliation").status("To Do", "To Do")
                .assignee("Bob Kaur").ac1("Ledger balances net to zero after refunds.")); // missing estimate -> also risk MEDIUM (unestimated)
        s.add(story("VAL-103", "Currency rounding edge cases").status("To Do", "To Do")
                .assignee("Alice Ng").sp(3)); // missing AC
        s.add(story("VAL-104", "Statement PDF layout").status("To Do", "To Do")
                .assignee("Cara Diaz").sp(2).ac2("AC lives only in the fallback field.")); // AC via fallback field
        s.add(story("VAL-105", "Webhook signature verify").status("To Do", "To Do")
                .sp(3).ac1("HMAC signature is validated before processing.")); // missing assignee
        s.add(story("VAL-106", "Audit log export").status("To Do", "To Do")
                .ac1("Exports are tamper-evident.")); // missing estimate + assignee
        s.add(story("MXEV-201", "Event schema migration").status("To Do", "To Do")
                .assignee("Dan Poe").sp(5).ac1("   ")); // whitespace-only AC -> blank

        // --- WIP: Alice has 4 In Progress (VAL-101,110,111,112) -> over per-assignee limit 3 -----
        s.add(story("VAL-110", "Fraud scoring v2").status("In Progress", "In Progress")
                .assignee("Alice Ng").sp(8).epic("VAL-26930").time(20.0, 8.0, 4.0) // logged 20h vs 8h est -> OVERRUN; also STUCK + high-SP risk
                .statusTx(at(now, 6), "Alice Ng", "To Do", "In Progress").updated(at(now, 1)));
        s.add(story("VAL-111", "Dispute intake API").status("In Progress", "In Progress")
                .assignee("Alice Ng").sp(3).updated(at(now, 1)).statusTx(at(now, 1), "Alice Ng", "To Do", "In Progress"));
        s.add(story("VAL-112", "Ledger snapshotting").status("In Review", "In Progress") // free-text status, In Progress category
                .assignee("Alice Ng").sp(5).updated(at(now, 1)).statusTx(at(now, 1), "Alice Ng", "In Progress", "In Review"));
        s.add(story("VAL-114", "Settlement batch").status("In Progress", "In Progress")
                .assignee("Bob Kaur").sp(3).updated(at(now, 1)).statusTx(at(now, 2), "Bob Kaur", "To Do", "In Progress")); // Bob under limit
        s.add(story("VAL-115", "Chargeback sync").status("In Progress", "In Progress")
                .sp(2).time(4.0, 5.0, 1.0).updated(at(now, 1))); // unassigned + logged -> Unassigned buckets

        // --- Sprint-flow edge cases ---------------------------------------------------------
        s.add(story("VAL-116", "Reporting API pagination").status("In Progress", "In Progress")
                .assignee("Bob Kaur").sp(3).updated(at(now, 4)) // updated 4d ago -> STALLED (not stuck: recent transition)
                .statusTx(at(now, 1), "Bob Kaur", "To Do", "In Progress"));
        s.add(story("VAL-117", "Onboarding wizard step 3").status("In Progress", "In Progress")
                .assignee("Cara Diaz").sp(3).updated(at(now, 2)) // REOPENED once
                .statusTx(at(now, 8), "Cara Diaz", "To Do", "In Progress")
                .statusTxJira(at(now, 5), "Cara Diaz", "In Progress", "Done")
                .statusTx(at(now, 2), "Cara Diaz", "Done", "In Progress"));
        s.add(story("VAL-118", "Rate limiter tuning").status("Done", "Done")
                .assignee("Dan Poe").sp(2).updated(at(now, 2)) // REOPENED twice
                .statusTx(at(now, 10), "Dan Poe", "To Do", "In Progress")
                .statusTx(at(now, 7), "Dan Poe", "In Progress", "Done")
                .statusTx(at(now, 4), "Dan Poe", "Done", "In Progress")
                .statusTx(at(now, 2), "Dan Poe", "In Progress", "Done"));
        s.add(story("VAL-119", "Cache invalidation bug").status("In Progress", "In Progress")
                .assignee("Alice Ng").sp(5).updated(at(now, 6)) // STUCK + REOPENED overlap
                .statusTx(at(now, 12), "Alice Ng", "To Do", "In Progress")
                .statusTx(at(now, 9), "Alice Ng", "In Progress", "Done")
                .statusTx(at(now, 6), "Alice Ng", "Done", "In Progress"));
        s.add(story("VAL-120", "Search relevance tweak").status("In Progress", "In Progress")
                .assignee("Bob Kaur").sp(3).updated(at(now, 2)) // "Shipped" not a done-keyword -> reopenCount 0
                .statusTx(at(now, 8), "Bob Kaur", "To Do", "In Progress")
                .statusTx(at(now, 5), "Bob Kaur", "In Progress", "Shipped")
                .statusTx(at(now, 2), "Bob Kaur", "Shipped", "In Progress"));
        s.add(story("VAL-121", "Feature flag cleanup").status("To Do", "To Do")
                .assignee("Bob Kaur").sp(2).ac1("Dead flags removed.").updated(at(now, 1))); // no transitions -> age null
        s.add(story("VAL-130", "Metrics dashboard").status("Done", "Done")
                .assignee("Cara Diaz").sp(3).updated(at(now, 10)) // Done + stale -> NOT stalled, NOT stuck
                .statusTx(at(now, 6), "Cara Diaz", "In Progress", "Done"));

        // --- Worklog / estimate-variance (native timetracking) ------------------------------
        s.add(story("VAL-141", "Email template refresh").status("Done", "Done")
                .assignee("Bob Kaur").sp(2).time(3.0, 10.0, 0.0)); // FINISHED_EARLY
        s.add(story("VAL-142", "SSO token refresh").status("In Progress", "In Progress")
                .assignee("Cara Diaz").sp(3).time(0.0, 6.0, 6.0)); // NO_LOGGING
        s.add(story("VAL-143", "Locale negotiation").status("Done", "Done")
                .assignee("Alice Ng").sp(3).epic("VAL-26930").time(8.0, 8.0, 0.0)); // OK (ratio 1.0)
        s.add(story("VAL-144", "Spike: GraphQL eval").status("To Do", "To Do")
                .assignee("Dan Poe")); // no estimate + no timetracking -> variance SKIP, worklog zero
        s.add(story("VAL-145", "Copy tweak").status("In Progress", "In Progress")
                .assignee("Bob Kaur").sp(1).time(1.0, 2.0, 1.0)); // 2h estimate -> skipped when minEstimate>2
        s.add(story("VAL-146", "Config validation").status("Done", "Done")
                .assignee("Alice Ng").sp(3).time(10.0, 8.0, 0.0)); // logged == 8*1.25 exactly -> NOT overrun

        // --- Scope change (Sprint-field changelog) ------------------------------------------
        s.add(story("VAL-150", "Late scope: partner API").status("To Do", "To Do")
                .assignee("Dan Poe").sp(5) // ADDED after start (created after start; multi-value CSV)
                .sprintTx(at(now, 2), "Dan Poe", "", "5001, 5002"));
        s.add(story("VAL-151", "Descoped: legacy import").status("To Do", "To Do")
                .assignee("Dan Poe").sp(3) // REMOVED after start
                .sprintTx(at(now, 1), "Dan Poe", "5001", ""));
        s.add(story("VAL-152", "Committed at planning").status("In Progress", "In Progress")
                .assignee("Alice Ng").sp(8) // Sprint change AT/BEFORE start -> original commitment, NOT added
                .sprintTx(at(now, 6), "Alice Ng", "", "5001"));

        // --- Delivery-risk boundaries -------------------------------------------------------
        s.add(story("VAL-161", "KYC provider swap").status("In Progress", "In Progress")
                .assignee("Bob Kaur").sp(8).epic("VAL-26931")); // storyPoints == storyPointsHigh(8) -> MEDIUM (inclusive)
        s.add(story("VAL-162", "Tax engine upgrade").status("To Do", "To Do")
                .assignee("Cara Diaz").sp(3).epic("VAL-26931").link("Blocks", "outward", "DATA-42", "Resolved")); // blocker resolved -> not counted
        s.add(story("VAL-163", "Notification retries").status("To Do", "To Do")
                .assignee("Cara Diaz").sp(3).link("Relates", "outward", "PLTF-90", "Open")); // non-blocker type -> not counted
        s.add(story("VAL-164", "Balance cache warmup").status("To Do", "To Do")
                .assignee("Bob Kaur").sp(3).link("Blocks", "outward", "VAL-778", "To Do")); // same project -> not cross-team
        s.add(story("VAL-165", "Ledger export v2").status("To Do", "To Do")
                .assignee("Bob Kaur").sp(3).linkNoStatus("Blocks", "outward", "PLTF-91")); // blocker status null -> not counted
        s.add(story("VAL-160", "Low-risk chore").status("To Do", "To Do")
                .assignee("Alice Ng").sp(2).ac1("Trivial.")); // LOW

        return s;
    }

    /** Release-queue members (project+status search, not the active sprint). */
    private static List<Story> releaseQueueStories() {
        return List.of(
                story("VAL-301", "Payout export").status("Ready for Release", "In Progress").assignee("Alice Ng").sp(3),
                story("VAL-302", "Reconcile report").status("Ready to Test", "In Progress").assignee("Bob Kaur").sp(5).epic("VAL-26931").ac1("Reconciliation totals match ledger."),
                story("MXEV-303", "Schema publish").status("In UAT", "In Progress").assignee("Dan Poe").sp(3).epic("VAL-26930"),
                story("VAL-304", "Fee schedule update").status("Ready for Release", "In Progress").assignee("Cara Diaz").sp(2));
    }

    /** Cross-team blocker, still open, moved recently (movedSinceLastCheck=true), has a target release. */
    private static Story pltf88(Instant now) {
        return story("PLTF-88", "Platform: connection pool exhaustion").status("In Progress", "In Progress")
                .assignee("Priya Rao").fix("Platform 24.3")
                .statusTx(at(now, 9), "Priya Rao", "To Do", "In Progress")
                .statusTx(at(now, 2), "Priya Rao", "In Progress", "Blocked"); // last transition 2d ago (< 7d lookback)
    }

    /** Cross-team issue linked only via "Relates" from VAL-101/VAL-163 (fetched if linkType=Relates). */
    private static Story pltf90(Instant now) {
        return story("PLTF-90", "Platform: metrics exporter").status("Open", "To Do")
                .assignee("Priya Rao")
                .statusTx(at(now, 3), "Priya Rao", "Backlog", "Open");
    }

    /** Cross-team blocker referenced by VAL-165 (open, so the sprint radar has a history to return). */
    private static Story pltf91(Instant now) {
        return story("PLTF-91", "Platform: TLS cert rotation").status("To Do", "To Do")
                .assignee("Priya Rao")
                .statusTx(at(now, 4), "Priya Rao", "Backlog", "To Do");
    }

    /** Cross-team dependency, resolved, last moved long ago (movedSinceLastCheck=false), no fixVersion. */
    private static Story data42(Instant now) {
        return story("DATA-42", "Data: nightly ETL backfill").status("Resolved", "Done")
                .assignee("Sam Lee")
                .statusTx(at(now, 20), "Sam Lee", "To Do", "In Progress")
                .statusTx(at(now, 14), "Sam Lee", "In Progress", "Resolved"); // 14d ago (> 7d lookback)
    }

    // ------------------------------------------------------------------------------------------
    // Projections -> Jira wire JSON
    // ------------------------------------------------------------------------------------------

    /** {@code /rest/api/2/search} body — full fields the client requests for active-sprint tools. */
    private static String searchResponse(List<Story> stories) {
        List<Object> nodes = new ArrayList<>();
        for (Story st : stories) {
            Map<String, Object> fields = obj();
            fields.put("summary", st.summary);
            fields.put("issuetype", obj("name", st.type));
            fields.put("status", statusMap(st.statusName, st.statusCategory));
            fields.put("assignee", st.assignee == null ? null : obj("displayName", st.assignee));
            fields.put("labels", st.labels);
            fields.put("issuelinks", searchLinks(st));
            fields.put("fixVersions", nameList(st.fixVersions));
            putTimetracking(fields, st);
            if (st.storyPoints != null) {
                fields.put(SP, st.storyPoints);
            }
            if (st.devJobPoints != null) {
                fields.put(DEVJOB, st.devJobPoints);
            }
            if (st.ac1 != null) {
                fields.put(AC1, st.ac1);
            }
            if (st.ac2 != null) {
                fields.put(AC2, st.ac2);
            }
            if (st.epicKey != null) {
                fields.put(EPIC, st.epicKey);
            }
            nodes.add(obj("key", st.key, "fields", fields));
        }
        return write(obj("issues", nodes));
    }

    /** {@code /sprint/{id}/issue?fields=customfield_10043,status} — story points + Sprint changelog. */
    private static String agileScope(List<Story> stories) {
        List<Object> nodes = new ArrayList<>();
        for (Story st : stories) {
            Map<String, Object> fields = obj();
            if (st.storyPoints != null) {
                fields.put(SP, st.storyPoints);
            }
            fields.put("status", statusMap(st.statusName, st.statusCategory));
            Map<String, Object> node = obj("key", st.key, "fields", fields);
            if (!st.sprintTx.isEmpty()) {
                node.put("changelog", obj("histories", sprintHistories(st)));
            }
            nodes.add(node);
        }
        return write(obj("issues", nodes));
    }

    /** {@code /sprint/{id}/issue?fields=summary,assignee,status,updated} — status changelog + updated. */
    private static String agileFlow(List<Story> stories) {
        List<Object> nodes = new ArrayList<>();
        for (Story st : stories) {
            Map<String, Object> fields = obj();
            fields.put("summary", st.summary);
            fields.put("assignee", st.assignee == null ? null : obj("displayName", st.assignee));
            fields.put("status", statusMap(st.statusName, st.statusCategory));
            if (st.updated != null) {
                fields.put("updated", JIRA_OFFSET.format(st.updated));
            }
            Map<String, Object> node = obj("key", st.key, "fields", fields);
            if (!st.statusTx.isEmpty()) {
                node.put("changelog", obj("histories", statusHistories(st)));
            }
            nodes.add(node);
        }
        return write(obj("issues", nodes));
    }

    /** {@code /rest/api/2/issue/{key}} — full single-issue shape, optionally with status changelog. */
    private static String singleIssue(Story st, boolean withChangelog) {
        Map<String, Object> fields = obj();
        fields.put("summary", st.summary);
        fields.put("issuetype", obj("name", st.type));
        fields.put("status", statusMap(st.statusName, st.statusCategory));
        fields.put("assignee", st.assignee == null ? null : obj("displayName", st.assignee));
        fields.put("labels", st.labels);
        fields.put("issuelinks", searchLinks(st));
        fields.put("fixVersions", nameList(st.fixVersions));
        putTimetracking(fields, st);
        if (st.storyPoints != null) {
            fields.put(SP, st.storyPoints);
        }
        if (st.devJobPoints != null) {
            fields.put(DEVJOB, st.devJobPoints);
        }
        if (st.ac1 != null) {
            fields.put(AC1, st.ac1);
        }
        if (st.ac2 != null) {
            fields.put(AC2, st.ac2);
        }
        if (st.epicKey != null) {
            fields.put(EPIC, st.epicKey);
        }
        if (st.descriptionHtml != null) {
            fields.put("description", st.descriptionHtml);
        }
        Map<String, Object> rendered = obj();
        if (st.descriptionHtml != null) {
            rendered.put("description", st.descriptionHtml);
        }
        if (st.ac1 != null) {
            rendered.put(AC1, st.ac1);
        }
        if (st.ac2 != null) {
            rendered.put(AC2, st.ac2);
        }
        Map<String, Object> node = obj("key", st.key, "fields", fields);
        node.put("renderedFields", rendered);
        if (withChangelog) {
            node.put("changelog", obj("histories", statusHistories(st)));
        }
        return write(node);
    }

    // ------------------------------------------------------------------------------------------
    // Small builders
    // ------------------------------------------------------------------------------------------

    private static Map<String, Object> statusMap(String name, String category) {
        return obj("name", name, "statusCategory", obj("name", category));
    }

    /**
     * Emit the full native {@code timetracking} block AND the top-level {@code aggregatetimespent} /
     * {@code aggregatetimeoriginalestimate} mirrors (prod parity: real Jira returns both, and
     * feature_effort_rollup prefers the aggregates). With no sub-tasks the aggregate equals the
     * issue's own value, so the derived numbers are identical whichever the client reads.
     */
    private static void putTimetracking(Map<String, Object> fields, Story st) {
        if (st.timeSpentSec == null && st.origEstSec == null && st.remEstSec == null) {
            return;
        }
        Map<String, Object> tt = obj();
        if (st.timeSpentSec != null) {
            tt.put("timeSpentSeconds", st.timeSpentSec);
            fields.put("aggregatetimespent", st.timeSpentSec);
        }
        if (st.origEstSec != null) {
            tt.put("originalEstimateSeconds", st.origEstSec);
            fields.put("aggregatetimeoriginalestimate", st.origEstSec);
        }
        if (st.remEstSec != null) {
            tt.put("remainingEstimateSeconds", st.remEstSec);
        }
        fields.put("timetracking", tt);
    }

    private static List<Object> searchLinks(Story st) {
        List<Object> out = new ArrayList<>();
        for (Link l : st.links) {
            Map<String, Object> linked = obj("key", l.key);
            if (l.status != null) {
                linked.put("fields", obj("status", obj("name", l.status)));
            }
            out.add(obj("type", obj("name", l.type), l.direction + "Issue", linked));
        }
        return out;
    }

    private static List<Object> statusHistories(Story st) {
        List<Object> out = new ArrayList<>();
        for (StatusTx t : st.statusTx) {
            Map<String, Object> item = obj("field", "status", "fromString", t.from, "toString", t.to);
            out.add(obj("created", t.jiraFormat ? JIRA_OFFSET.format(t.when) : t.when.toString(),
                    "author", obj("displayName", t.author), "items", List.of(item)));
        }
        return out;
    }

    private static List<Object> sprintHistories(Story st) {
        List<Object> out = new ArrayList<>();
        for (SprintTx t : st.sprintTx) {
            Map<String, Object> item = obj("field", "Sprint", "from", t.fromIds, "to", t.toIds);
            out.add(obj("created", t.when.toString(), "author", obj("displayName", t.author), "items", List.of(item)));
        }
        return out;
    }

    private static String sprintList(Map<String, Object>... sprints) {
        return write(obj("values", List.of(sprints)));
    }

    private static Map<String, Object> sprintNode(long id, String name, String state, Instant startDate, Instant endDate) {
        Map<String, Object> n = obj("id", id, "name", name, "state", state);
        n.put("startDate", startDate.toString());
        n.put("endDate", endDate.toString());
        n.put("goal", "Ship the " + name + " commitments.");
        return n;
    }

    private static String sprintOnly(long id, String name, String state, Instant startDate, Instant endDate) {
        return write(sprintNode(id, name, state, startDate, endDate));
    }

    private static String velocity() {
        long[] ids = {4700, 4800, 4900, 4950, 4980, 5000, 5001};
        String[] names = {"Sprint 1", "Sprint 2", "Sprint 3", "Sprint 3b", "Sprint 3c", "Sprint 3d", "RIO PI-3 Sprint 4"};
        double[] committed = {34, 40, 38, 42, 30, 45, 44};
        double[] completed = {30, 38, 41, 36, 30, 39, 0};

        List<Object> sprints = new ArrayList<>();
        Map<String, Object> stats = obj();
        for (int i = 0; i < ids.length; i++) {
            sprints.add(obj("id", ids[i], "name", names[i]));
            // Leave the last (current) sprint OUT of the stats map -> estimated/completed default to 0.
            if (i < ids.length - 1) {
                stats.put(String.valueOf(ids[i]),
                        obj("estimated", obj("value", committed[i]), "completed", obj("value", completed[i])));
            }
        }
        return write(obj("sprints", sprints, "velocityStatEntries", stats));
    }

    private static List<Object> nameList(List<String> names) {
        List<Object> out = new ArrayList<>();
        for (String n : names) {
            out.add(obj("name", n));
        }
        return out;
    }

    private static Story byKey(List<Story> stories, String key) {
        return stories.stream().filter(s -> s.key.equals(key)).findFirst().orElseThrow();
    }

    private static List<Story> byKeys(List<Story> stories, String... keys) {
        List<Story> out = new ArrayList<>();
        for (String k : keys) {
            out.add(byKey(stories, k));
        }
        return out;
    }

    private static Instant at(Instant now, int daysAgo) {
        return now.minus(Duration.ofDays(daysAgo));
    }

    // ------------------------------------------------------------------------------------------
    // MockServer helpers
    // ------------------------------------------------------------------------------------------

    /** Parse a JSON body string into a tree so it can be embedded as an object in the initializer JSON. */
    private static Object parse(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse fixture JSON", e);
        }
    }

    // ------------------------------------------------------------------------------------------
    // JSON helpers
    // ------------------------------------------------------------------------------------------

    private static Map<String, Object> obj() {
        return new LinkedHashMap<>();
    }

    private static Map<String, Object> obj(String k, Object v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(k, v);
        return m;
    }

    private static Map<String, Object> obj(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> m = obj(k1, v1);
        m.put(k2, v2);
        return m;
    }

    private static Map<String, Object> obj(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
        Map<String, Object> m = obj(k1, v1, k2, v2);
        m.put(k3, v3);
        return m;
    }

    private static Map<String, Object> obj(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) {
        Map<String, Object> m = obj(k1, v1, k2, v2, k3, v3);
        m.put(k4, v4);
        return m;
    }

    private static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize fixture JSON", e);
        }
    }

    private static Story story(String key, String summary) {
        return new Story(key, summary);
    }

    // ------------------------------------------------------------------------------------------
    // Model
    // ------------------------------------------------------------------------------------------

    private static final class Story {
        private final String key;
        private final String summary;
        private String type = "Story";
        private String statusName = "To Do";
        private String statusCategory = "To Do";
        private String assignee;
        private Double storyPoints;
        private Double devJobPoints;
        private String ac1;
        private String ac2;
        private String epicKey;
        private String descriptionHtml;
        private Integer timeSpentSec;
        private Integer origEstSec;
        private Integer remEstSec;
        private Instant updated;
        private final List<String> labels = new ArrayList<>();
        private final List<String> fixVersions = new ArrayList<>();
        private final List<Link> links = new ArrayList<>();
        private final List<StatusTx> statusTx = new ArrayList<>();
        private final List<SprintTx> sprintTx = new ArrayList<>();

        private Story(String key, String summary) {
            this.key = key;
            this.summary = summary;
        }

        private Story status(String name, String category) {
            this.statusName = name;
            this.statusCategory = category;
            return this;
        }

        private Story assignee(String name) {
            this.assignee = name;
            return this;
        }

        private Story sp(double points) {
            this.storyPoints = points;
            return this;
        }

        /** The Epic's own "Planned Dev Job" snapshot ({@code customfield_24665}) — reference only. */
        private Story devJob(double points) {
            this.devJobPoints = points;
            return this;
        }

        private Story ac1(String text) {
            this.ac1 = text;
            return this;
        }

        private Story ac2(String text) {
            this.ac2 = text;
            return this;
        }

        private Story epic(String epicKey) {
            this.epicKey = epicKey;
            return this;
        }

        private Story desc(String html) {
            this.descriptionHtml = html;
            return this;
        }

        private Story labels(String... l) {
            this.labels.addAll(List.of(l));
            return this;
        }

        private Story fix(String name) {
            this.fixVersions.add(name);
            return this;
        }

        private Story time(double spentHours, double originalHours, double remainingHours) {
            this.timeSpentSec = (int) (spentHours * 3600);
            this.origEstSec = (int) (originalHours * 3600);
            this.remEstSec = (int) (remainingHours * 3600);
            return this;
        }

        private Story updated(Instant when) {
            this.updated = when;
            return this;
        }

        private Story link(String type, String direction, String key, String status) {
            this.links.add(new Link(type, direction, key, status));
            return this;
        }

        private Story linkNoStatus(String type, String direction, String key) {
            this.links.add(new Link(type, direction, key, null));
            return this;
        }

        private Story statusTx(Instant when, String author, String from, String to) {
            this.statusTx.add(new StatusTx(when, author, from, to, false));
            return this;
        }

        /** Same as {@link #statusTx} but emits Jira's colon-less offset timestamp (exercises the lenient parser). */
        private Story statusTxJira(Instant when, String author, String from, String to) {
            this.statusTx.add(new StatusTx(when, author, from, to, true));
            return this;
        }

        private Story sprintTx(Instant when, String author, String fromIds, String toIds) {
            this.sprintTx.add(new SprintTx(when, author, fromIds, toIds));
            return this;
        }
    }

    private record Link(String type, String direction, String key, String status) {
    }

    private record StatusTx(Instant when, String author, String from, String to, boolean jiraFormat) {
    }

    private record SprintTx(Instant when, String author, String fromIds, String toIds) {
    }
}
