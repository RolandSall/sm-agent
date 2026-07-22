package io.github.scrumagent.acceptance.steps.releasequeue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.spring.ScenarioScope;
import io.github.scrumagent.flow.ReleaseQueueReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Scenario-scoped state and assertions for the release-queue feature. */
@Component
@ScenarioScope
public class ReleaseQueueState {

    private final ObjectMapper objectMapper;
    private ResultActions result;

    @Autowired
    public ReleaseQueueState(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void capture(ResultActions result) {
        this.result = result;
    }

    private ReleaseQueueReport report() {
        try {
            String body = result.andReturn().getResponse().getContentAsString();
            return objectMapper.readValue(body, ReleaseQueueReport.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read release queue report", e);
        }
    }

    public void assertCount(int expected) {
        assertThat(report().count()).isEqualTo(expected);
    }

    /**
     * Compares the per-status buckets against an expected DataTable of {@code | status | count |}
     * rows — the arithmetic count of issues sitting in each release status.
     */
    public void assertByStatus(List<Map<String, String>> rows) {
        for (Map<String, String> row : rows) {
            String status = row.get("status");
            int expected = Integer.parseInt(row.get("count").trim());
            assertThat(report().byStatus().getOrDefault(status, 0))
                    .as("release status %s count", status).isEqualTo(expected);
        }
    }
}
