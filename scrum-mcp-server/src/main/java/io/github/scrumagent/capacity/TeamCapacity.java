package io.github.scrumagent.capacity;

/**
 * Curated capacity snapshot for one sprint.
 *
 * <p>In phase 1 these numbers arrive as tool parameters — the agent tells us what the team
 * committed and what it can actually do. In phase 2 a Graph-backed {@link CapacityGateway}
 * implementation will source them from the org's capacity/leave data and replace the parameters.
 *
 * @param committedPoints points already committed for the sprint
 * @param availablePoints points the team can realistically deliver
 */
public record TeamCapacity(String sprint, double committedPoints, double availablePoints) {

    /** Headroom left after the commitment; negative means over-committed. */
    public double remainingPoints() {
        return availablePoints - committedPoints;
    }
}
