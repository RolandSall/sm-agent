package io.github.scrumagent.flow;

import java.util.List;

/**
 * Curated PO "ready to test" result: the stories currently sitting in the team's acceptance-test
 * statuses, each with the ingredients a PO needs to start testing.
 *
 * @param stories the stories waiting for acceptance
 */
public record StoriesToTestReport(List<StoryToTest> stories) {

    /**
     * @param key the story key
     * @param summary the story summary
     * @param assignee display name of who did the work, or {@code null}
     * @param acceptanceCriteria the AC to test against, or {@code null} if none captured
     * @param epicKey the parent Epic ("feature") key, or {@code null}
     * @param status the current status
     */
    public record StoryToTest(String key, String summary, String assignee, String acceptanceCriteria,
                              String epicKey, String status) {
    }
}
