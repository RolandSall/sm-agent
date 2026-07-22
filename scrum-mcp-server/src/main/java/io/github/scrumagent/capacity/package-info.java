/**
 * Team-capacity capability: how many points the team can absorb in a sprint. Leaf in phase 1 —
 * it only defines the value type and the gateway seam. No implementation ships yet: the risk
 * module falls back to agent-supplied parameters until a Graph-backed gateway lands in phase 2.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"shared"})
package io.github.scrumagent.capacity;
