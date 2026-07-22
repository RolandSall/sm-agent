package io.github.scrumagent.acceptance.steps.featureeffort;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.spring.ScenarioScope;
import io.github.scrumagent.features.FeatureEffortReport;
import io.github.scrumagent.jira.FeatureEffort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Scenario-scoped state and assertions for the feature-effort-rollup feature. */
@Component
@ScenarioScope
public class FeatureEffortState {

    private final ObjectMapper objectMapper;
    private ResultActions result;

    @Autowired
    public FeatureEffortState(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void capture(ResultActions result) {
        this.result = result;
    }

    private FeatureEffortReport report() {
        try {
            String body = result.andReturn().getResponse().getContentAsString();
            return objectMapper.readValue(body, FeatureEffortReport.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read feature-effort report", e);
        }
    }

    private FeatureEffort feature(String epicKey) {
        return report().features().stream()
                .filter(f -> f.epicKey().equals(epicKey))
                .findFirst()
                .orElseThrow(() -> new AssertionError("feature " + epicKey + " not in report"));
    }

    /**
     * Compares one feature's rolled-up ingredients against an expected single-row DataTable, checking
     * ONLY the columns present. Recognised columns: {@code storyCount}, {@code sumStoryPoints},
     * {@code sumEstimateHours}, {@code sumLoggedHours}, {@code sumEstimateDays}, {@code sumLoggedDays},
     * {@code devJobPoints} — all arithmetic/ingredients, no verdicts.
     */
    public void assertFeatureRollup(String epicKey, Map<String, String> row) {
        FeatureEffort f = feature(epicKey);
        if (present(row, "storyCount")) {
            assertThat(f.storyCount()).as("%s storyCount", epicKey)
                    .isEqualTo(Integer.parseInt(row.get("storyCount").trim()));
        }
        compare(row, "sumStoryPoints", epicKey, f.sumStoryPoints());
        compare(row, "sumEstimateHours", epicKey, f.sumEstimateHours());
        compare(row, "sumLoggedHours", epicKey, f.sumLoggedHours());
        compare(row, "sumEstimateDays", epicKey, f.sumEstimateDays());
        compare(row, "sumLoggedDays", epicKey, f.sumLoggedDays());
        if (present(row, "devJobPoints")) {
            assertThat(f.devJobPoints()).as("%s devJobPoints", epicKey)
                    .isEqualTo(Double.parseDouble(row.get("devJobPoints").trim()));
        }
    }

    private static boolean present(Map<String, String> row, String column) {
        String v = row.get(column);
        return v != null && !v.isBlank();
    }

    private static void compare(Map<String, String> row, String column, String epicKey, double actual) {
        if (present(row, column)) {
            assertThat(actual).as("%s column %s", epicKey, column)
                    .isEqualTo(Double.parseDouble(row.get(column).trim()));
        }
    }
}
