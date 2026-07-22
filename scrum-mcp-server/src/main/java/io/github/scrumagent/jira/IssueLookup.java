package io.github.scrumagent.jira;

/** Fetch a single issue by key. */
public interface IssueLookup {

    /** Fetch one issue with rendered description, acceptance criteria and links. */
    JiraIssue getIssue(String key);
}
