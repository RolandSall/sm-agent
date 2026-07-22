package io.github.scrumagent.capacity;

/**
 * Seam for sourcing a team's sprint capacity.
 *
 * <p>No implementation ships in phase 1 — there is intentionally no bean of this type. The risk
 * handler injects it as optional and falls back to agent-supplied parameters when no bean is
 * present. Phase 2 adds a Graph-backed implementation and the fallback disappears.
 */
public interface CapacityGateway {

    TeamCapacity getTeamCapacity(String sprint);
}
