package io.github.scrumagent.acceptance.steps.estimatevariance;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.spring.ScenarioScope;
import io.github.scrumagent.worklog.EstimateVarianceReport;
import io.github.scrumagent.worklog.EstimateVarianceReport.IssueVariance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Scenario-scoped state and assertions for the estimate-variance feature. */
@Component
@ScenarioScope
public class EstimateVarianceState {

    private final ObjectMapper objectMapper;
    private ResultActions result;

    @Autowired
    public EstimateVarianceState(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void capture(ResultActions result) {
        this.result = result;
    }

    private EstimateVarianceReport report() {
        try {
            String body = result.andReturn().getResponse().getContentAsString();
            return objectMapper.readValue(body, EstimateVarianceReport.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read estimate-variance report", e);
        }
    }

    private IssueVariance entry(String key) {
        return report().issues().stream()
                .filter(i -> i.key().equals(key))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No variance entry for " + key));
    }

    /**
     * Compares the report entries against an expected DataTable keyed by {@code key}, checking ONLY the
     * raw ingredient columns present per row — {@code usageRatio}, {@code statusCategory},
     * {@code loggedHours}, {@code originalEstimateHours}. There is no verdict/advisory to assert; the
     * caller (the skill/LLM) owns the judgement, so the table proves the structured data only.
     */
    public void assertVarianceMatches(List<Map<String, String>> rows) {
        for (Map<String, String> row : rows) {
            String key = row.get("key");
            IssueVariance v = entry(key);
            assertDouble(key, "usageRatio", row.get("usageRatio"), v.usageRatio());
            assertDouble(key, "loggedHours", row.get("loggedHours"), v.loggedHours());
            assertDouble(key, "originalEstimateHours", row.get("originalEstimateHours"), v.originalEstimateHours());
            String statusCategory = row.get("statusCategory");
            if (statusCategory != null && !statusCategory.isBlank()) {
                assertThat(v.statusCategory()).as("%s statusCategory", key).isEqualTo(statusCategory.trim());
            }
        }
    }

    private void assertDouble(String key, String field, String expected, double actual) {
        if (expected != null && !expected.isBlank()) {
            assertThat(actual).as("%s %s", key, field).isEqualTo(Double.parseDouble(expected.trim()));
        }
    }

    public void assertReturned(String key, boolean expected) {
        boolean actual = report().issues().stream().anyMatch(i -> i.key().equals(key));
        assertThat(actual).as("issue %s returned in the report", key).isEqualTo(expected);
    }
}
