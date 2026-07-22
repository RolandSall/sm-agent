package io.github.scrumagent.acceptance.steps.featureeffort;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.scrumagent.acceptance.JiraJson;
import io.github.scrumagent.acceptance.JiraRows;
import io.github.scrumagent.acceptance.JiraStubs;
import io.github.scrumagent.acceptance.driver.ScrumApiDriver;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Feature-effort steps: accumulate epics + their children from inline DataTables, stub the epic-fetch
 * and Epic-Link child search, drive {@code /api/feature-effort}, and delegate assertions to
 * {@link FeatureEffortState}.
 */
public class FeatureEffortSteps {

    private final ScrumApiDriver apiDriver;
    private final FeatureEffortState state;

    private final Map<String, JiraJson.Issue> epics = new LinkedHashMap<>();
    private final Map<String, List<JiraJson.Issue>> children = new LinkedHashMap<>();

    @Autowired
    public FeatureEffortSteps(ScrumApiDriver apiDriver, FeatureEffortState state) {
        this.apiDriver = apiDriver;
        this.state = state;
    }

    private List<JiraJson.Issue> childrenOf(String epicKey) {
        return children.computeIfAbsent(epicKey, k -> new ArrayList<>());
    }

    @Given("the following epics")
    public void theFollowingEpics(DataTable table) {
        for (Map<String, String> row : table.asMaps()) {
            String key = row.get("key");
            epics.put(key, JiraRows.toIssue(row));
            childrenOf(key);
        }
    }

    @Given("epic {string} has the following children")
    public void epicHasTheFollowingChildren(String epicKey, DataTable table) {
        for (Map<String, String> row : table.asMaps()) {
            childrenOf(epicKey).add(JiraRows.toIssue(row));
        }
    }

    private void stubEpics() {
        for (Map.Entry<String, JiraJson.Issue> entry : epics.entrySet()) {
            String epicKey = entry.getKey();
            JiraStubs.epicIssue(epicKey, JiraJson.issueResponse(entry.getValue()));
            JiraStubs.epicChildrenSearch(epicKey, JiraJson.searchResponse(
                    childrenOf(epicKey).toArray(new JiraJson.Issue[0])));
        }
    }

    @When("I request the feature effort roll-up for epics {string}")
    public void requestForEpics(String epicKey) throws Exception {
        stubEpics();
        state.capture(apiDriver.featureEffort(List.of(epicKey)));
    }

    @When("I request the feature effort roll-up with no epics")
    public void requestNoEpics() throws Exception {
        stubEpics();
        state.capture(apiDriver.featureEffort(null));
    }

    @Then("the feature roll-up for {string} is")
    public void theFeatureRollUpForIs(String epicKey, DataTable table) {
        state.assertFeatureRollup(epicKey, table.asMaps().get(0));
    }
}
