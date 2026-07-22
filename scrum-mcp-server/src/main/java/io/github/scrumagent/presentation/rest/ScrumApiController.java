package io.github.scrumagent.presentation.rest;

import io.github.scrumagent.dependencies.DependenciesOfQuery;
import io.github.scrumagent.dependencies.DependencyReport;
import io.github.scrumagent.dependencies.SprintDependenciesQuery;
import io.github.scrumagent.features.FeatureEffortQuery;
import io.github.scrumagent.features.FeatureEffortReport;
import io.github.scrumagent.flow.ReleaseQueueQuery;
import io.github.scrumagent.flow.ReleaseQueueReport;
import io.github.scrumagent.flow.SprintFlowQuery;
import io.github.scrumagent.flow.SprintFlowReport;
import io.github.scrumagent.flow.StoriesToTestQuery;
import io.github.scrumagent.flow.StoriesToTestReport;
import io.github.scrumagent.flow.WipReport;
import io.github.scrumagent.flow.WipStatusQuery;
import io.github.scrumagent.hygiene.BoardHygieneQuery;
import io.github.scrumagent.hygiene.BoardHygieneReport;
import io.github.scrumagent.jira.GetIssueQuery;
import io.github.scrumagent.jira.JiraIssue;
import io.github.scrumagent.metrics.ListSprintsQuery;
import io.github.scrumagent.metrics.MetricsPublicationPreview;
import io.github.scrumagent.metrics.PrepareMetricsPublicationQuery;
import io.github.scrumagent.metrics.ScopeChangeQuery;
import io.github.scrumagent.metrics.ScopeChangeReport;
import io.github.scrumagent.metrics.SprintListReport;
import io.github.scrumagent.metrics.VelocityQuery;
import io.github.scrumagent.metrics.VelocityReport;
import io.github.scrumagent.risk.RiskReport;
import io.github.scrumagent.risk.SprintRiskQuery;
import io.github.scrumagent.worklog.EstimateVarianceQuery;
import io.github.scrumagent.worklog.EstimateVarianceReport;
import io.github.scrumagent.worklog.WorkloadQuery;
import io.github.scrumagent.worklog.WorkloadReport;
import io.github.springmediator.mediator.bus.MediatorBus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The read HTTP API for every capability. This is the "Mode B for Java" surface: when a harness
 * cannot use MCP (org policy) but can run terminal commands, a skill tells it to
 * {@code curl http://<host>/api/...} against the already-running server — far cheaper than a
 * JVM-booting one-shot CLI. It is also the dashboard / phase-2-UI surface (Mode C).
 *
 * <p>Every endpoint dispatches the SAME mediator query as the matching MCP {@code @Tool} and CLI
 * subcommand — three thin adapters over one CQRS core. Read-only: the publish COMMIT (a write) is
 * intentionally not exposed here; it stays an MCP tool behind human approval.
 *
 * <p><b>When deployed</b> this must sit behind authentication (a Spring Security filter) — it is
 * unauthenticated by default and, in server mode, is served on the same port as {@code /mcp}.
 */
@RestController
@RequestMapping("/api")
class ScrumApiController {

    private final MediatorBus mediator;

    ScrumApiController(MediatorBus mediator) {
        this.mediator = mediator;
    }

    // --- jira ---

    @GetMapping("/issue/{key}")
    JiraIssue issue(@PathVariable String key) {
        return mediator.query(new GetIssueQuery(key));
    }

    // --- hygiene / flow ---

    @GetMapping("/hygiene")
    BoardHygieneReport hygiene(@RequestParam(required = false) String issueType,
                               @RequestParam(required = false) String assignee) {
        return mediator.query(new BoardHygieneQuery(issueType, assignee));
    }

    @GetMapping("/wip")
    WipReport wip(@RequestParam(required = false) Integer perAssignee,
                  @RequestParam(required = false) Integer team,
                  @RequestParam(required = false) String issueType,
                  @RequestParam(required = false) String assignee) {
        return mediator.query(new WipStatusQuery(perAssignee, team, issueType, assignee));
    }

    @GetMapping("/release-queue")
    ReleaseQueueReport releaseQueue(@RequestParam("status") List<String> statuses,
                                    @RequestParam(required = false) String issueType,
                                    @RequestParam(required = false) String assignee) {
        return mediator.query(new ReleaseQueueQuery(statuses, issueType, assignee));
    }

    @GetMapping("/sprint-flow")
    SprintFlowReport sprintFlow(@RequestParam(required = false) String issueType,
                                @RequestParam(required = false) String assignee) {
        return mediator.query(new SprintFlowQuery(issueType, assignee));
    }

    @GetMapping("/stories-to-test")
    StoriesToTestReport storiesToTest(
            @RequestParam(name = "status", required = false) List<String> statuses,
            @RequestParam(required = false) String issueType,
            @RequestParam(required = false) String assignee) {
        return mediator.query(new StoriesToTestQuery(statuses, issueType, assignee));
    }

    // --- features (per-Epic effort roll-up) ---

    @GetMapping("/feature-effort")
    FeatureEffortReport featureEffort(
            @RequestParam(name = "epic", required = false) List<String> epicKeys) {
        return mediator.query(new FeatureEffortQuery(epicKeys));
    }

    // --- worklog ---

    @GetMapping("/worklog")
    WorkloadReport worklog(@RequestParam(required = false) String issueType,
                           @RequestParam(required = false) String assignee,
                           @RequestParam(required = false) Double hoursPerDay) {
        return mediator.query(new WorkloadQuery(issueType, assignee, hoursPerDay));
    }

    @GetMapping("/estimate-variance")
    EstimateVarianceReport estimateVariance(@RequestParam(required = false) Integer minEstimateHours) {
        return mediator.query(new EstimateVarianceQuery(minEstimateHours));
    }

    // --- metrics ---

    @GetMapping("/velocity")
    VelocityReport velocity(@RequestParam(required = false) Integer sprints) {
        return mediator.query(new VelocityQuery(sprints));
    }

    @GetMapping("/sprints")
    SprintListReport sprints(@RequestParam(required = false) String state) {
        return mediator.query(new ListSprintsQuery(state));
    }

    @GetMapping("/scope-change")
    ScopeChangeReport scopeChange() {
        return mediator.query(new ScopeChangeQuery());
    }

    /**
     * Read half of the publish HITL — renders the summary + token, writes nothing. POST, not GET:
     * it registers a governance preview server-side (a state change), so it must not be a cacheable,
     * pre-fetchable GET even though it publishes nothing to Jira.
     */
    @PostMapping("/metrics-preview")
    MetricsPublicationPreview metricsPreview() {
        return mediator.query(new PrepareMetricsPublicationQuery());
    }

    // --- dependencies ---

    @GetMapping("/dependencies/{key}")
    DependencyReport dependenciesOf(@PathVariable String key) {
        return mediator.query(new DependenciesOfQuery(key));
    }

    @GetMapping("/dependencies")
    DependencyReport sprintDependencies() {
        return mediator.query(new SprintDependenciesQuery());
    }

    // --- risk (the E×D×C join) ---

    @GetMapping("/risk")
    RiskReport risk(@RequestParam(required = false) Double committed,
                    @RequestParam(required = false) Double available,
                    @RequestParam(required = false) String issueType,
                    @RequestParam(required = false) String assignee) {
        return mediator.query(new SprintRiskQuery(committed, available, issueType, assignee));
    }
}
