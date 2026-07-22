package io.github.scrumagent.shared;

/**
 * Jira's three built-in status <em>category</em> names (not workflow status names). Plain constants
 * so the same literals are not scattered as magic strings across the flow / worklog / jira code.
 */
public final class StatusCategory {

    public static final String TO_DO = "To Do";
    public static final String IN_PROGRESS = "In Progress";
    public static final String DONE = "Done";

    private StatusCategory() {
    }
}
