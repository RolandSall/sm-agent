/**
 * Cross-team dependency-radar capability. On this instance a cross-team dependency is a
 * {@code Dependency}-type issue linked to an Epic (Epic Link), naming the team we wait on
 * ({@code Depend On}) and the team waiting ({@code Dependent/s}) — NOT a Jira issue link. The radar
 * takes an issue's (or the whole sprint's) Epics, fetches the Dependency issues on them, and keeps
 * the OPEN ones where our team is the waiting side. Returns ingredients only; the agent reasons about
 * severity. Read-only.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"jira", "shared"})
package io.github.scrumagent.dependencies;
