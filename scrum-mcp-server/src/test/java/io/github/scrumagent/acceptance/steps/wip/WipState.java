package io.github.scrumagent.acceptance.steps.wip;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.spring.ScenarioScope;
import io.github.scrumagent.flow.WipReport;
import io.github.scrumagent.flow.WipReport.AssigneeWip;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Scenario-scoped state and assertions for the WIP-limit feature. */
@Component
@ScenarioScope
public class WipState {

    private final ObjectMapper objectMapper;
    private ResultActions result;

    @Autowired
    public WipState(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void capture(ResultActions result) {
        this.result = result;
    }

    private WipReport report() {
        try {
            String body = result.andReturn().getResponse().getContentAsString();
            return objectMapper.readValue(body, WipReport.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read WIP report", e);
        }
    }

    public void assertNoAssignees() {
        assertThat(report().perAssignee()).isEmpty();
    }

    /**
     * Compares the per-assignee WIP ingredients against an expected DataTable, matched by
     * {@code assignee} and checking ONLY the columns present per row: {@code count} (exact
     * in-progress count) and {@code limit} (echoed per-assignee limit). There is no {@code over}
     * verdict — the handler computes none; a reader judges counts against limits themselves.
     */
    public void assertWipPerAssignee(List<Map<String, String>> rows) {
        for (Map<String, String> row : rows) {
            String name = row.get("assignee");
            AssigneeWip w = byName(name);
            if (present(row, "count")) {
                assertThat(w.inProgress()).as("%s count", name)
                        .isEqualTo(Integer.parseInt(row.get("count").trim()));
            }
            if (present(row, "limit")) {
                assertThat(w.limit()).as("%s limit", name)
                        .isEqualTo(Integer.valueOf(row.get("limit").trim()));
            }
        }
    }

    /**
     * Compares the team-level WIP ingredients against a single expected row, checking ONLY the
     * columns present: {@code inProgress} (exact team sum) and {@code limit} (echoed team limit).
     * There is no {@code over} verdict — the handler computes none.
     */
    public void assertTeamWip(Map<String, String> row) {
        WipReport report = report();
        if (present(row, "inProgress")) {
            assertThat(report.teamInProgress()).as("team inProgress")
                    .isEqualTo(Integer.parseInt(row.get("inProgress").trim()));
        }
        if (present(row, "limit")) {
            assertThat(report.teamLimit()).as("team limit")
                    .isEqualTo(Integer.valueOf(row.get("limit").trim()));
        }
    }

    private static boolean present(Map<String, String> row, String column) {
        String v = row.get(column);
        return v != null && !v.isBlank();
    }

    private AssigneeWip byName(String name) {
        return report().perAssignee().stream()
                .filter(a -> a.assignee().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No WIP entry for " + name));
    }
}
