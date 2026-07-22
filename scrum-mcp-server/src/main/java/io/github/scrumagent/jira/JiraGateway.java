package io.github.scrumagent.jira;

/**
 * The jira module's exposed API. Capability modules (hygiene, metrics, ...) depend on
 * this interface — or, preferably, on the NARROW role interface they actually use — never
 * on the REST client, so acceptance tests can stub Jira at the HTTP boundary (WireMock) or,
 * if ever needed, at this seam.
 *
 * <p>This is the full facade: the union of the eight role interfaces below. A single bean
 * ({@code JiraClient}) implements it, so it satisfies every role at once. New consumers should
 * inject the smallest role they need (interface segregation) rather than this fat aggregate.
 */
public interface JiraGateway extends IssueLookup, IssueSearch, SprintQueries, SprintFlowQueries,
        WorkTimeQueries, IssueHistoryQueries, FeatureEffortQueries, DependencyQueries, JiraProjectConfig {
}
