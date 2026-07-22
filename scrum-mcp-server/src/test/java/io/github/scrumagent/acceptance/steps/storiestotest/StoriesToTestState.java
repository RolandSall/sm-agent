package io.github.scrumagent.acceptance.steps.storiestotest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.spring.ScenarioScope;
import io.github.scrumagent.flow.StoriesToTestReport;
import io.github.scrumagent.flow.StoriesToTestReport.StoryToTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Scenario-scoped state and assertions for the stories-to-test feature. */
@Component
@ScenarioScope
public class StoriesToTestState {

    private final ObjectMapper objectMapper;
    private ResultActions result;

    @Autowired
    public StoriesToTestState(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void capture(ResultActions result) {
        this.result = result;
    }

    private StoriesToTestReport report() {
        try {
            String body = result.andReturn().getResponse().getContentAsString();
            return objectMapper.readValue(body, StoriesToTestReport.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read stories-to-test report", e);
        }
    }

    private StoryToTest story(String key) {
        return report().stories().stream()
                .filter(s -> s.key().equals(key))
                .findFirst()
                .orElse(null);
    }

    public void assertDoesNotContain(String key) {
        assertThat(story(key)).as("story %s absent", key).isNull();
    }

    /**
     * Compares the returned stories against an expected DataTable, matched by {@code key} (each row's
     * story must be present) and checking ONLY the columns present per row: {@code assignee},
     * {@code acceptanceCriteria}, {@code epicKey}, {@code status}, {@code summary}.
     */
    public void assertStoriesMatch(List<Map<String, String>> rows) {
        for (Map<String, String> row : rows) {
            String key = row.get("key");
            StoryToTest s = story(key);
            assertThat(s).as("story %s present", key).isNotNull();
            compare(row, "summary", key, s.summary());
            compare(row, "assignee", key, s.assignee());
            compare(row, "acceptanceCriteria", key, s.acceptanceCriteria());
            compare(row, "epicKey", key, s.epicKey());
            compare(row, "status", key, s.status());
        }
    }

    private static void compare(Map<String, String> row, String column, String key, String actual) {
        String expected = row.get(column);
        if (expected != null && !expected.isBlank()) {
            assertThat(actual).as("%s %s", key, column).isEqualTo(expected.trim());
        }
    }
}
