package io.github.scrumagent.shared;

import java.time.OffsetDateTime;

/**
 * One curated status transition from a Jira changelog. Produced by the jira gateway's changelog
 * parser, consumed by the dependency radar to answer "what happened to this dependency".
 *
 * @param from status transitioned from
 * @param to status transitioned to
 * @param when when the transition happened
 * @param author who made the transition
 */
public record StatusChange(String from, String to, OffsetDateTime when, String author) {
}
