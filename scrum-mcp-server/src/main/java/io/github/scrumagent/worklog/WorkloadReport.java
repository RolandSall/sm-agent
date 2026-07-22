package io.github.scrumagent.worklog;

import java.util.List;

/**
 * Curated logged-work rollup (hours). Per assignee and a team total; missing logged hours count
 * as zero. Each hours figure also carries a DERIVED working-days ingredient ({@code *Days}) computed
 * from the same hours via {@code WorkingDays.fromHours}; the hours are authoritative and untouched.
 *
 * <p>Because a missing worklog and a genuine 0h both sum to {@code logged == 0}, each assignee also
 * carries two exact COUNTS so the reader can tell them apart: {@code issueCount} (issues assigned)
 * and {@code worklogCount} (of those, how many carry any logged time {@code > 0}). {@code issueCount
 * > 0} with {@code worklogCount == 0} is the "assigned work but nothing logged" signal; the tool does
 * not classify it — see the logged-hours skill.
 */
public record WorkloadReport(List<AssigneeWorkload> perAssignee, WorkloadTotals totals) {

    public record AssigneeWorkload(String assignee, double logged, double originalEstimate, double remaining,
                                   double loggedDays, double originalEstimateDays, double remainingDays,
                                   int issueCount, int worklogCount) {
    }

    public record WorkloadTotals(double logged, double originalEstimate, double remaining,
                                 double loggedDays, double originalEstimateDays, double remainingDays) {
    }
}
