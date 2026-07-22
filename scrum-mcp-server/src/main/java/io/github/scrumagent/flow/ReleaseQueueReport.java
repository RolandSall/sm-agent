package io.github.scrumagent.flow;

import java.util.List;
import java.util.Map;

/**
 * Curated release-queue result: how many issues are ready to test, bucketed by status.
 *
 * @param byStatus count per release status name
 * @param items the issues themselves
 */
public record ReleaseQueueReport(int count, Map<String, Integer> byStatus, List<ReleaseQueueItem> items) {

    public record ReleaseQueueItem(String key, String summary, String status, String assignee) {
    }
}
