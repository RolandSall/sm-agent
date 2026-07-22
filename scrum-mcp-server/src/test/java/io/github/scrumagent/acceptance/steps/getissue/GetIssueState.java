package io.github.scrumagent.acceptance.steps.getissue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.spring.ScenarioScope;
import io.github.scrumagent.jira.JiraIssue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Scenario-scoped state and assertions for the get-issue feature. */
@Component
@ScenarioScope
public class GetIssueState {

    private final ObjectMapper objectMapper;
    private ResultActions result;

    @Autowired
    public GetIssueState(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void capture(ResultActions result) {
        this.result = result;
    }

    private JiraIssue issue() {
        try {
            String body = result.andReturn().getResponse().getContentAsString();
            return objectMapper.readValue(body, JiraIssue.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read issue", e);
        }
    }

    /**
     * Compares the curated issue's scalar fields against a single expected row, checking ONLY the
     * columns present: {@code key}, {@code summary}, {@code status}, {@code storyPoints}. The
     * list-shaped fields (labels, links) are asserted by their own membership steps.
     */
    public void assertIssueMatches(Map<String, String> row) {
        JiraIssue issue = issue();
        compareText(row, "key", issue.key());
        compareText(row, "summary", issue.summary());
        compareText(row, "status", issue.status());
        String points = row.get("storyPoints");
        if (points != null && !points.isBlank()) {
            assertThat(issue.storyPoints()).as("storyPoints").isEqualTo(Double.parseDouble(points.trim()));
        }
    }

    private static void compareText(Map<String, String> row, String column, String actual) {
        String expected = row.get(column);
        if (expected != null && !expected.isBlank()) {
            assertThat(actual).as(column).isEqualTo(expected.trim());
        }
    }

    public void assertLabel(String label) {
        assertThat(issue().labels()).contains(label);
    }

    public void assertLink(String type, String linkedKey) {
        assertThat(issue().links())
                .anyMatch(l -> l.type().equals(type) && l.key().equals(linkedKey));
    }
}
