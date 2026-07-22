/**
 * Presentation layer: the two thin transport adapters — the MCP {@code @Tool} surface
 * ({@code presentation.mcp}) and the read REST API ({@code presentation.rest}). Both hold no logic;
 * each method builds a mediator query/command and dispatches it. Depends on every capability module
 * whose queries it exposes plus {@code shared} for the read/write tool markers and governance types.
 * It does NOT depend on {@code capacity}: capacity is a seam behind the {@code risk} module, never
 * addressed from a transport.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {
        "jira", "hygiene", "flow", "worklog", "metrics", "dependencies", "risk", "features", "shared"})
package io.github.scrumagent.presentation;
