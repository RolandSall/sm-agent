package io.github.scrumagent.worklog.internal;

import io.github.scrumagent.jira.JiraProjectConfig;
import io.github.scrumagent.jira.WorkTimeQueries;
import io.github.scrumagent.jira.JiraWorkTime;
import io.github.scrumagent.worklog.EstimateVarianceQuery;
import io.github.scrumagent.worklog.EstimateVarianceReport;
import io.github.scrumagent.shared.WorkingDays;
import io.github.scrumagent.worklog.EstimateVarianceReport.IssueVariance;
import io.github.springmediator.mediator.annotations.QueryHandler;
import io.github.springmediator.mediator.core.IQueryHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Reports logged time vs original estimate for every estimated active-sprint issue — a FETCH, not a
 * verdict. Reuses the native work-time rollup (logged + timetracking) and Jira's built-in status
 * category, so the numbers are instance-independent. Only issues that carry an original estimate (at
 * or above {@code minEstimateHours}) are considered — you cannot judge variance against no estimate.
 *
 * <p>Returns the raw ingredients ({@code usageRatio}, {@code statusCategory}, hours/days) for every
 * estimated issue and computes no OVERRUN / FINISHED_EARLY / NO_LOGGING flag — the caller applies its
 * team's line (see the estimate-variance skill). The only logic here is the fetch, the deterministic
 * usageRatio arithmetic, and the {@code minEstimateHours} fact-gate.
 */
@QueryHandler(EstimateVarianceQuery.class)
public class EstimateVarianceHandler implements IQueryHandler<EstimateVarianceQuery, EstimateVarianceReport> {

    private final WorkTimeQueries jira;
    private final JiraProjectConfig config;

    public EstimateVarianceHandler(WorkTimeQueries jira, JiraProjectConfig config) {
        this.jira = jira;
        this.config = config;
    }

    @Override
    public EstimateVarianceReport execute(EstimateVarianceQuery query) {
        int minEstimateHours = query.getMinEstimateHours() != null ? query.getMinEstimateHours() : 0;
        double whpd = config.workingHoursPerDay();

        List<IssueVariance> issues = new ArrayList<>();
        for (JiraWorkTime wt : jira.activeSprintWorkTime()) {
            double orig = WorklogMath.orZero(wt.originalEstimateHours());
            // Skip issues with no estimate (or below the floor) — variance is meaningless without one.
            if (orig <= 0 || orig < minEstimateHours) {
                continue;
            }
            double logged = WorklogMath.clampLogged(wt.key(), WorklogMath.orZero(wt.loggedHours()));
            double usageRatio = orig == 0 ? 0.0 : logged / orig;
            issues.add(new IssueVariance(wt.key(), wt.assignee(), logged, orig,
                    WorkingDays.fromHours(logged, whpd), WorkingDays.fromHours(orig, whpd),
                    usageRatio, wt.statusCategory()));
        }
        return new EstimateVarianceReport(issues);
    }
}
