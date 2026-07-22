/**
 * Delivery-risk capability — a fetch, not a verdict. It returns the active sprint's stories, the
 * cross-team {@code Dependency} issues on their Epics, and the team's capacity headroom — all raw. It
 * does NOT filter, count, or classify: the agent joins stories to dependencies by Epic, keeps the open
 * ones where our team is the waiting side, and reasons about LOW/MEDIUM/HIGH (see the delivery-risk
 * skill). Capacity comes from the optional capacity gateway or, absent one, from agent-supplied
 * parameters. Read-only.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"jira", "capacity", "shared"})
package io.github.scrumagent.risk;
