package io.github.scrumagent.acceptance.driver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Shared driver over the read HTTP API ({@code ScrumApiController} at {@code /api}). One method per
 * endpoint; each performs the request through MockMvc and returns the raw {@link ResultActions} for
 * a {@code State} to parse and assert against. Holds no assertion logic and no scenario state.
 */
@Component
public class ScrumApiDriver {

    private final MockMvc mockMvc;

    @Autowired
    public ScrumApiDriver(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    public ResultActions hygiene(String issueType, String assignee) throws Exception {
        var request = get("/api/hygiene");
        request = filters(request, issueType, assignee);
        return mockMvc.perform(request);
    }

    public ResultActions wip(Integer perAssignee, Integer team, String issueType, String assignee)
            throws Exception {
        var request = get("/api/wip");
        if (perAssignee != null) {
            request = request.param("perAssignee", String.valueOf(perAssignee));
        }
        if (team != null) {
            request = request.param("team", String.valueOf(team));
        }
        request = filters(request, issueType, assignee);
        return mockMvc.perform(request);
    }

    public ResultActions releaseQueue(List<String> statuses, String issueType, String assignee)
            throws Exception {
        var request = get("/api/release-queue");
        for (String status : statuses) {
            request = request.param("status", status);
        }
        request = filters(request, issueType, assignee);
        return mockMvc.perform(request);
    }

    public ResultActions storiesToTest(List<String> statuses, String issueType, String assignee)
            throws Exception {
        var request = get("/api/stories-to-test");
        if (statuses != null) {
            for (String status : statuses) {
                request = request.param("status", status);
            }
        }
        request = filters(request, issueType, assignee);
        return mockMvc.perform(request);
    }

    public ResultActions featureEffort(List<String> epicKeys) throws Exception {
        var request = get("/api/feature-effort");
        if (epicKeys != null) {
            for (String epic : epicKeys) {
                request = request.param("epic", epic);
            }
        }
        return mockMvc.perform(request);
    }

    public ResultActions worklog(String issueType, String assignee) throws Exception {
        return worklog(issueType, assignee, null);
    }

    public ResultActions worklog(String issueType, String assignee, Double hoursPerDay) throws Exception {
        var request = get("/api/worklog");
        request = filters(request, issueType, assignee);
        if (hoursPerDay != null) {
            request = request.param("hoursPerDay", String.valueOf(hoursPerDay));
        }
        return mockMvc.perform(request);
    }

    public ResultActions estimateVariance(Integer minEstimateHours) throws Exception {
        var request = get("/api/estimate-variance");
        if (minEstimateHours != null) {
            request = request.param("minEstimateHours", String.valueOf(minEstimateHours));
        }
        return mockMvc.perform(request);
    }

    public ResultActions sprintFlow(String issueType, String assignee) throws Exception {
        var request = get("/api/sprint-flow");
        request = filters(request, issueType, assignee);
        return mockMvc.perform(request);
    }

    public ResultActions velocity(Integer sprints) throws Exception {
        var request = get("/api/velocity");
        if (sprints != null) {
            request = request.param("sprints", String.valueOf(sprints));
        }
        return mockMvc.perform(request);
    }

    public ResultActions sprints(String state) throws Exception {
        var request = get("/api/sprints");
        if (state != null) {
            request = request.param("state", state);
        }
        return mockMvc.perform(request);
    }

    public ResultActions dependenciesOf(String key) throws Exception {
        return mockMvc.perform(get("/api/dependencies/{key}", key));
    }

    public ResultActions risk(Double committed, Double available, String issueType, String assignee)
            throws Exception {
        var request = get("/api/risk");
        if (committed != null) {
            request = request.param("committed", String.valueOf(committed));
        }
        if (available != null) {
            request = request.param("available", String.valueOf(available));
        }
        request = filters(request, issueType, assignee);
        return mockMvc.perform(request);
    }

    /** Appends the optional handler-side {@code issueType}/{@code assignee} filters when present. */
    private static MockHttpServletRequestBuilder filters(MockHttpServletRequestBuilder request,
                                                         String issueType, String assignee) {
        if (issueType != null) {
            request = request.param("issueType", issueType);
        }
        if (assignee != null) {
            request = request.param("assignee", assignee);
        }
        return request;
    }

    public ResultActions issue(String key) throws Exception {
        return mockMvc.perform(get("/api/issue/{key}", key));
    }

    public ResultActions scopeChange() throws Exception {
        return mockMvc.perform(get("/api/scope-change"));
    }

    public ResultActions sprintDependencies() throws Exception {
        return mockMvc.perform(get("/api/dependencies"));
    }
}
