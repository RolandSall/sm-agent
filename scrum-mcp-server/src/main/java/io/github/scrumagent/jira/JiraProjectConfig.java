package io.github.scrumagent.jira;

import java.util.List;

/** The configured team project identity. */
public interface JiraProjectConfig {

    /** The configured team project key(s) — used to tell "our" issues from other teams' issues. */
    List<String> projectKeys();

    /**
     * Hours in one working day, for re-expressing effort hours as derived working-days. Team policy
     * from config; a sensible default (8.0) applies when unset, so callers never see a non-positive.
     */
    double workingHoursPerDay();
}
