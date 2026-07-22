/**
 * Gateway module for Jira Server/Data Center.
 *
 * <p>Exposed API: {@link io.github.scrumagent.jira.JiraGateway} plus curated DTOs and
 * mediator queries. Everything REST-specific (PAT auth, endpoints, custom-field IDs,
 * pagination) stays in {@code internal} — other modules never see a raw Jira payload.
 *
 * <p>Established integration patterns: Bearer PAT sessions,
 * {@code expand=renderedFields}, acceptance-criteria extraction from custom fields
 * (configurable, not hard-coded), curated small DTOs instead of raw payloads.
 */
package io.github.scrumagent.jira;
