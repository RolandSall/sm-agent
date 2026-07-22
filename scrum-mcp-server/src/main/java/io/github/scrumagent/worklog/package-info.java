/**
 * Logged-work capability: per-assignee logged hours vs original estimate vs remaining, from the
 * configured logged-hours custom field + Jira timetracking. Reads the jira gateway. Read-only.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"jira", "shared"})
package io.github.scrumagent.worklog;
