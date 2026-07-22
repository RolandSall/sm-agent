package io.github.scrumagent.worklog.internal;

import io.github.scrumagent.jira.JiraProjectConfig;
import io.github.scrumagent.jira.WorkTimeQueries;
import io.github.scrumagent.jira.JiraWorkTime;
import io.github.scrumagent.shared.IssueFilters;
import io.github.scrumagent.shared.WorkingDays;
import io.github.scrumagent.worklog.WorkloadQuery;
import io.github.scrumagent.worklog.WorkloadReport;
import io.github.scrumagent.worklog.WorkloadReport.AssigneeWorkload;
import io.github.scrumagent.worklog.WorkloadReport.WorkloadTotals;
import io.github.springmediator.mediator.annotations.QueryHandler;
import io.github.springmediator.mediator.core.IQueryHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Groups the active-sprint work-time rollup by assignee and totals it, all in code. */
@QueryHandler(WorkloadQuery.class)
public class WorkloadHandler implements IQueryHandler<WorkloadQuery, WorkloadReport> {

    private static final String UNASSIGNED = "Unassigned";

    private final WorkTimeQueries jira;
    private final JiraProjectConfig config;

    public WorkloadHandler(WorkTimeQueries jira, JiraProjectConfig config) {
        this.jira = jira;
        this.config = config;
    }

    @Override
    public WorkloadReport execute(WorkloadQuery query) {
        // [logged, original, remaining, issueCount, worklogCount] — the last two are exact counts.
        Map<String, double[]> byAssignee = new LinkedHashMap<>();
        double totalLogged = 0, totalOriginal = 0, totalRemaining = 0;

        for (JiraWorkTime wt : jira.activeSprintWorkTime()) {
            if (!IssueFilters.matches(query.getIssueType(), wt.type())
                    || !IssueFilters.matches(query.getAssignee(), wt.assignee())) {
                continue;
            }
            String assignee = wt.assignee() == null || wt.assignee().isBlank() ? UNASSIGNED : wt.assignee();
            double logged = WorklogMath.clampLogged(wt.key(), WorklogMath.orZero(wt.loggedHours()));
            double original = WorklogMath.orZero(wt.originalEstimateHours());
            double remaining = WorklogMath.orZero(wt.remainingEstimateHours());

            double[] acc = byAssignee.computeIfAbsent(assignee, k -> new double[5]);
            acc[0] += logged;
            acc[1] += original;
            acc[2] += remaining;
            acc[3] += 1;                       // one more issue assigned
            if (logged > 0) {
                acc[4] += 1;                   // one more issue that carries logged time
            }
            totalLogged += logged;
            totalOriginal += original;
            totalRemaining += remaining;
        }

        // Tier-2 override wins; a null/non-positive value falls back to the configured default.
        double whpd = query.getHoursPerDay() != null && query.getHoursPerDay() > 0
                ? query.getHoursPerDay() : config.workingHoursPerDay();
        List<AssigneeWorkload> perAssignee = new ArrayList<>();
        for (Map.Entry<String, double[]> e : byAssignee.entrySet()) {
            double[] a = e.getValue();
            perAssignee.add(new AssigneeWorkload(e.getKey(), a[0], a[1], a[2],
                    WorkingDays.fromHours(a[0], whpd),
                    WorkingDays.fromHours(a[1], whpd),
                    WorkingDays.fromHours(a[2], whpd),
                    (int) a[3], (int) a[4]));
        }
        WorkloadTotals totals = new WorkloadTotals(totalLogged, totalOriginal, totalRemaining,
                WorkingDays.fromHours(totalLogged, whpd),
                WorkingDays.fromHours(totalOriginal, whpd),
                WorkingDays.fromHours(totalRemaining, whpd));
        return new WorkloadReport(perAssignee, totals);
    }
}
