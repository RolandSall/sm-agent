package io.github.scrumagent.acceptance.steps.worklog;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.spring.ScenarioScope;
import io.github.scrumagent.worklog.WorkloadReport;
import io.github.scrumagent.worklog.WorkloadReport.AssigneeWorkload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Scenario-scoped state and assertions for the logged-hours feature. */
@Component
@ScenarioScope
public class WorklogState {

    private final ObjectMapper objectMapper;
    private ResultActions result;

    @Autowired
    public WorklogState(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void capture(ResultActions result) {
        this.result = result;
    }

    private WorkloadReport report() {
        try {
            String body = result.andReturn().getResponse().getContentAsString();
            return objectMapper.readValue(body, WorkloadReport.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read workload report", e);
        }
    }

    /**
     * Compares the report's per-assignee ingredients against an expected DataTable, checking ONLY the
     * columns present on each row (a missing/blank cell is not asserted). Recognised columns:
     * {@code logged}, {@code originalEstimate}, {@code remaining}, {@code loggedDays},
     * {@code originalEstimateDays}, {@code remainingDays} (hours/days), plus the exact counts
     * {@code issueCount} (issues assigned) and {@code worklogCount} (issues with logged time &gt; 0)
     * — all arithmetic, no verdicts.
     */
    public void assertWorkloadMatches(List<Map<String, String>> rows) {
        for (Map<String, String> row : rows) {
            String name = row.get("assignee");
            AssigneeWorkload w = byName(name);
            compare(row, "logged", name, w.logged());
            compare(row, "originalEstimate", name, w.originalEstimate());
            compare(row, "remaining", name, w.remaining());
            compare(row, "loggedDays", name, w.loggedDays());
            compare(row, "originalEstimateDays", name, w.originalEstimateDays());
            compare(row, "remainingDays", name, w.remainingDays());
            compare(row, "issueCount", name, w.issueCount());
            compare(row, "worklogCount", name, w.worklogCount());
        }
    }

    private static void compare(Map<String, String> row, String column, String name, double actual) {
        String expected = row.get(column);
        if (expected != null && !expected.isBlank()) {
            assertThat(actual)
                    .as("assignee %s column %s", name, column)
                    .isEqualTo(Double.parseDouble(expected.trim()));
        }
    }

    /**
     * Compares the report-level totals against a single expected row, checking ONLY the columns
     * present: {@code assigneeCount} (how many assignees survived the filter) and {@code logged}
     * (team logged-hours total).
     */
    public void assertWorkloadTotals(Map<String, String> row) {
        WorkloadReport report = report();
        String count = row.get("assigneeCount");
        if (count != null && !count.isBlank()) {
            assertThat(report.perAssignee()).as("assigneeCount").hasSize(Integer.parseInt(count.trim()));
        }
        String logged = row.get("logged");
        if (logged != null && !logged.isBlank()) {
            assertThat(report.totals().logged()).as("team logged total")
                    .isEqualTo(Double.parseDouble(logged.trim()));
        }
    }

    private AssigneeWorkload byName(String name) {
        return report().perAssignee().stream()
                .filter(a -> a.assignee().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No workload entry for " + name));
    }
}
