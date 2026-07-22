package io.github.scrumagent.acceptance.steps.listsprints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.spring.ScenarioScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenario-scoped state and assertions for the list-sprints feature. Parses the response as a raw
 * JSON tree and asserts facts (count, id, full name, state) rather than a verdict — so the steps
 * compile independently of the report type and the assertions read straight off the wire.
 */
@Component
@ScenarioScope
public class ListSprintsState {

    private final ObjectMapper objectMapper;
    private ResultActions result;

    @Autowired
    public ListSprintsState(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void capture(ResultActions result) {
        this.result = result;
    }

    private JsonNode sprints() {
        try {
            String body = result.andReturn().getResponse().getContentAsString();
            if (body == null || body.isBlank()) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(body).path("sprints");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read sprint list report", e);
        }
    }

    /**
     * Compares the listed sprints against an expected DataTable. The listing must hold exactly as many
     * sprints as rows given; each row is matched by {@code name} and ONLY the columns present are
     * asserted: {@code id} and {@code state}.
     */
    public void assertSprintsMatch(List<Map<String, String>> rows) {
        JsonNode sprints = sprints();
        assertThat(sprints).as("listed sprint count").hasSize(rows.size());
        for (Map<String, String> row : rows) {
            String name = row.get("name");
            JsonNode match = null;
            for (JsonNode sprint : sprints) {
                if (name.equals(sprint.path("name").asText())) {
                    match = sprint;
                    break;
                }
            }
            assertThat(match).as("a sprint named '%s' is listed", name).isNotNull();
            String id = row.get("id");
            if (id != null && !id.isBlank()) {
                assertThat(match.path("id").asLong()).as("%s id", name).isEqualTo(Long.parseLong(id.trim()));
            }
            String state = row.get("state");
            if (state != null && !state.isBlank()) {
                assertThat(match.path("state").asText()).as("%s state", name).isEqualTo(state.trim());
            }
        }
    }
}
