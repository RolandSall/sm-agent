package io.github.scrumagent.shared;

/**
 * Marker for an MCP tool presentation bean that only reads. Read tools are always registered,
 * regardless of {@code scrum.governance.read-only}. Every {@code *Tools} bean implements
 * {@link ReadTool} or {@link WriteTool} so the registration seam can gate writes structurally.
 */
public interface ReadTool {
}
