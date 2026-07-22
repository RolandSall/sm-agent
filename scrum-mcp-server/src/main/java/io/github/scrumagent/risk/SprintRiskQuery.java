package io.github.scrumagent.risk;

import io.github.springmediator.mediator.core.IQuery;

/**
 * Mediator query: raw delivery-risk ingredients for the active sprint. Capacity numbers are
 * agent-supplied (Tier 2); a {@code null} capacity pair flags {@code capacityKnown=false} rather than
 * guessing. The handler returns the stories, the cross-team dependencies on their Epics, and capacity
 * — the agent reasons about the risk level from them (no thresholds, no verdict in code).
 */
public class SprintRiskQuery implements IQuery<RiskReport> {

    private final Double capacityCommittedPoints;
    private final Double capacityAvailablePoints;
    private final String issueType;
    private final String assignee;

    public SprintRiskQuery(Double capacityCommittedPoints, Double capacityAvailablePoints,
                           String issueType, String assignee) {
        this.capacityCommittedPoints = capacityCommittedPoints;
        this.capacityAvailablePoints = capacityAvailablePoints;
        this.issueType = issueType;
        this.assignee = assignee;
    }

    public Double getCapacityCommittedPoints() {
        return capacityCommittedPoints;
    }

    public Double getCapacityAvailablePoints() {
        return capacityAvailablePoints;
    }

    /** Optional issue-type filter ({@code null}/blank = all types). */
    public String getIssueType() {
        return issueType;
    }

    /** Optional assignee filter ({@code null}/blank = all assignees). */
    public String getAssignee() {
        return assignee;
    }
}
