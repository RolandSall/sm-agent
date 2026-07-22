package io.github.scrumagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.modulith.Modulith;

/**
 * Scrum Master agent — MCP tool server (layer 3 of the architecture).
 *
 * <p>A Spring Modulith: each top-level package under this one is an enforced module.
 * Gateway modules ({@code jira}, {@code confluence}, {@code teams}) wrap one external
 * system behind a curated API; capability modules ({@code hygiene}, {@code metrics},
 * {@code capacity}) hold the deterministic computations ("metrics are computed in code,
 * never by the LLM"); presentation modules ({@code mcp}, {@code web}) are thin adapters
 * that dispatch the same mediator queries.
 *
 * <p>Phase 1: GitHub Copilot / Claude Code connect via MCP (streamable HTTP at /mcp) and
 * run the playbooks interactively. Phase 2: {@code @Scheduled} jobs + a ChatClient loop
 * are added as one more presentation adapter — same queries, same handlers.
 */
@Modulith
@SpringBootApplication
@ConfigurationPropertiesScan
public class ScrumAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScrumAgentApplication.class, args);
    }
}
