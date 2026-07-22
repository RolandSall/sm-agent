/**
 * Flow/queue-state capability: WIP limits (too many stories in progress at once) and the
 * release-queue count (how many issues are ready to test). Reads the jira gateway; in-progress
 * detection uses Jira's built-in {@code statusCategory}. Read-only.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"jira", "shared"})
package io.github.scrumagent.flow;
