package io.github.scrumagent.shared;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mode-neutral safety gate. When {@code readOnly} is true (the default), write/commit/notify
 * tools are not registered with the MCP server AND command handlers refuse to act — so the
 * guard holds across MCP, CLI, and REST alike.
 *
 * @param readOnly whether the agent may only read and propose (never mutate external systems)
 */
@ConfigurationProperties(prefix = "scrum.governance")
public record GovernanceProperties(boolean readOnly) {

    public GovernanceProperties {
        // default-on: an absent/blank config is treated as read-only.
    }

    /** @return true when writes are permitted (read-only explicitly disabled). */
    public boolean writesAllowed() {
        return !readOnly;
    }
}
