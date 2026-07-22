/**
 * Board-hygiene capability: flags active-sprint issues missing an estimate, acceptance criteria,
 * or an assignee. Reads the jira gateway; computes flags in code. Read-only.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"jira", "shared"})
package io.github.scrumagent.hygiene;
