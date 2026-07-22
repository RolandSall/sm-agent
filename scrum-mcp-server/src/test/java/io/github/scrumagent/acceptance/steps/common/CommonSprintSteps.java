package io.github.scrumagent.acceptance.steps.common;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.github.scrumagent.acceptance.JiraJson;
import io.github.scrumagent.acceptance.JiraRows;
import io.github.scrumagent.acceptance.JiraStubs;

import java.util.List;
import java.util.Map;

/**
 * Shared {@code Given}s that stub the active sprint (and the cross-team Dependency issues on its
 * Epics) from inline DataTables, so the scenario data lives in the Gherkin. Each row becomes a
 * {@link JiraJson.Issue} via {@link JiraRows}; only the columns present are applied. Reused by the
 * active-sprint read features (worklog, estimate-variance, feature-effort, delivery-risk, the sprint
 * dependency radar).
 */
public class CommonSprintSteps {

    @Given("the active sprint contains the following issues")
    public void theActiveSprintContainsTheFollowingIssues(DataTable table) {
        stubActiveSprint(table);
    }

    @Given("the active sprint has these stories")
    public void theActiveSprintHasTheseStories(DataTable table) {
        stubActiveSprint(table);
    }

    /**
     * Stubs the {@code Dependency}-type issue search for the sprint's (or an issue's) Epics. Each row
     * is a Dependency issue: {@code key}, {@code summary}, {@code dependOn}, {@code dependents},
     * {@code status}, {@code epic} (Epic Link), and optional {@code deliverySprint}/{@code deliveryEnd}.
     */
    @Given("these Dependency issues exist on those Epics")
    public void theseDependencyIssuesExistOnThoseEpics(DataTable table) {
        List<Map<String, String>> rows = table.asMaps();
        JiraJson.Issue[] issues = rows.stream()
                .map(row -> JiraRows.toIssue(row).type("Dependency"))
                .toArray(JiraJson.Issue[]::new);
        JiraStubs.dependencySearch(JiraJson.searchResponse(issues));
    }

    @Given("no Dependency issues exist on those Epics")
    public void noDependencyIssuesExistOnThoseEpics() {
        JiraStubs.dependencySearch(JiraJson.searchResponse());
    }

    private static void stubActiveSprint(DataTable table) {
        List<Map<String, String>> rows = table.asMaps();
        JiraJson.Issue[] issues = rows.stream().map(JiraRows::toIssue).toArray(JiraJson.Issue[]::new);
        JiraStubs.activeSprintSearch(JiraJson.searchResponse(issues));
    }
}
