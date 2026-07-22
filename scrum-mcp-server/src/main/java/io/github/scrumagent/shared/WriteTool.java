package io.github.scrumagent.shared;

/**
 * Marker for an MCP tool presentation bean that writes / notifies. Write tools are registered
 * with the MCP server ONLY when {@code scrum.governance.read-only=false}. The same read-only
 * flag is also enforced inside the command handlers, so REST/CLI callers are gated identically.
 */
public interface WriteTool {
}
