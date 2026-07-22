package io.github.scrumagent.acceptance.steps.hygiene;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.spring.ScenarioScope;
import io.github.scrumagent.hygiene.BoardHygieneReport;
import io.github.scrumagent.hygiene.BoardHygieneReport.HygieneIssue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenario-scoped state for the board-hygiene feature: holds the captured API result and owns every
 * assertion. Parses the JSON body into the app's {@link BoardHygieneReport} record with the injected
 * Spring {@link ObjectMapper}.
 */
@Component
@ScenarioScope
public class HygieneState {

    private final ObjectMapper objectMapper;
    private ResultActions result;

    @Autowired
    public HygieneState(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void capture(ResultActions result) {
        this.result = result;
    }

    private BoardHygieneReport report() {
        try {
            String body = result.andReturn().getResponse().getContentAsString();
            return objectMapper.readValue(body, BoardHygieneReport.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read board hygiene report", e);
        }
    }

    private HygieneIssue issue(String key) {
        return report().issues().stream()
                .filter(i -> i.key().equals(key))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No hygiene entry for " + key));
    }

    /**
     * Compares the returned per-issue readiness signals against an expected DataTable, matched by
     * {@code key} and checking ONLY the columns present per row: {@code missingEstimate},
     * {@code missingAcceptanceCriteria}, {@code missingAssignee} — the deterministic per-field
     * ingredients the caller applies its own definition-of-ready to.
     */
    public void assertHygienePerIssue(List<Map<String, String>> rows) {
        for (Map<String, String> row : rows) {
            String key = row.get("key");
            HygieneIssue i = issue(key);
            compare(row, "missingEstimate", key, i.missingEstimate());
            compare(row, "missingAcceptanceCriteria", key, i.missingAcceptanceCriteria());
            compare(row, "missingAssignee", key, i.missingAssignee());
        }
    }

    /**
     * Compares the report totals against a single expected row, checking ONLY the columns present:
     * {@code totalIssues} (every issue examined, clean ones included) and the deterministic per-field
     * counts {@code missingEstimateCount}, {@code missingAcceptanceCriteriaCount},
     * {@code missingAssigneeCount}. There is deliberately no OR-combined gap count to assert.
     */
    public void assertHygieneTotals(Map<String, String> row) {
        BoardHygieneReport report = report();
        if (present(row, "totalIssues")) {
            int total = Integer.parseInt(row.get("totalIssues").trim());
            assertThat(report.totalIssues()).as("totalIssues").isEqualTo(total);
            // Every examined issue is returned (clean ones included) — this is the ingredient list.
            assertThat(report.issues()).as("returned issues").hasSize(total);
        }
        if (present(row, "missingEstimateCount")) {
            assertThat(report.missingEstimateCount()).as("missingEstimateCount")
                    .isEqualTo(Integer.parseInt(row.get("missingEstimateCount").trim()));
        }
        if (present(row, "missingAcceptanceCriteriaCount")) {
            assertThat(report.missingAcceptanceCriteriaCount()).as("missingAcceptanceCriteriaCount")
                    .isEqualTo(Integer.parseInt(row.get("missingAcceptanceCriteriaCount").trim()));
        }
        if (present(row, "missingAssigneeCount")) {
            assertThat(report.missingAssigneeCount()).as("missingAssigneeCount")
                    .isEqualTo(Integer.parseInt(row.get("missingAssigneeCount").trim()));
        }
    }

    private static void compare(Map<String, String> row, String column, String key, boolean actual) {
        if (present(row, column)) {
            assertThat(actual).as("%s %s", key, column)
                    .isEqualTo(Boolean.parseBoolean(row.get(column).trim()));
        }
    }

    private static boolean present(Map<String, String> row, String column) {
        String v = row.get(column);
        return v != null && !v.isBlank();
    }
}
