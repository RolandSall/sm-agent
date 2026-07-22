package io.github.scrumagent.acceptance.steps.velocity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.spring.ScenarioScope;
import io.github.scrumagent.metrics.VelocityReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Scenario-scoped state and assertions for the sprint-velocity feature. */
@Component
@ScenarioScope
public class VelocityState {

    private final ObjectMapper objectMapper;
    private ResultActions result;

    @Autowired
    public VelocityState(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void capture(ResultActions result) {
        this.result = result;
    }

    private VelocityReport report() {
        try {
            String body = result.andReturn().getResponse().getContentAsString();
            return objectMapper.readValue(body, VelocityReport.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read velocity report", e);
        }
    }

    /**
     * Compares the velocity summary against a single expected row, checking ONLY the columns present:
     * {@code averageVelocity} (the code-computed average) and {@code sprintCount} (how many sprints
     * fed the average).
     */
    public void assertVelocitySummary(Map<String, String> row) {
        VelocityReport report = report();
        String avg = row.get("averageVelocity");
        if (avg != null && !avg.isBlank()) {
            assertThat(report.averageVelocity()).as("averageVelocity")
                    .isEqualTo(Double.parseDouble(avg.trim()));
        }
        String count = row.get("sprintCount");
        if (count != null && !count.isBlank()) {
            assertThat(report.sprints()).as("sprintCount").hasSize(Integer.parseInt(count.trim()));
        }
    }
}
