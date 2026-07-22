package io.github.scrumagent.hygiene.internal;

import io.github.scrumagent.hygiene.BoardHygieneQuery;
import io.github.scrumagent.hygiene.BoardHygieneReport;
import io.github.scrumagent.hygiene.BoardHygieneReport.HygieneIssue;
import io.github.scrumagent.jira.IssueSearch;
import io.github.scrumagent.jira.JiraIssue;
import io.github.scrumagent.shared.IssueFilters;
import io.github.springmediator.mediator.annotations.QueryHandler;
import io.github.springmediator.mediator.core.IQueryHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes the per-field hygiene signals in code for every active-sprint issue. Returns the raw
 * per-field booleans for ALL issues (clean ones included) so the caller can apply its own
 * definition-of-ready. The only aggregates are DETERMINISTIC per-field COUNTS — one count per field,
 * never an OR-combined "issues with gaps" number, because combining fields is exactly the
 * definition-of-ready judgement that belongs to the team, not this tool.
 */
@QueryHandler(BoardHygieneQuery.class)
public class BoardHygieneHandler implements IQueryHandler<BoardHygieneQuery, BoardHygieneReport> {

    private final IssueSearch jira;

    public BoardHygieneHandler(IssueSearch jira) {
        this.jira = jira;
    }

    @Override
    public BoardHygieneReport execute(BoardHygieneQuery query) {
        List<JiraIssue> sprintIssues = jira.activeSprintIssues();
        List<HygieneIssue> issues = new ArrayList<>();
        int missingEstimateCount = 0;
        int missingAcceptanceCriteriaCount = 0;
        int missingAssigneeCount = 0;

        for (JiraIssue issue : sprintIssues) {
            if (!IssueFilters.matches(query.getIssueType(), issue.type())
                    || !IssueFilters.matches(query.getAssignee(), issue.assignee())) {
                continue;
            }
            boolean missingEstimate = issue.storyPoints() == null;
            boolean missingAc = issue.acceptanceCriteria() == null || issue.acceptanceCriteria().isBlank();
            boolean missingAssignee = issue.assignee() == null || issue.assignee().isBlank();
            if (missingEstimate) {
                missingEstimateCount++;
            }
            if (missingAc) {
                missingAcceptanceCriteriaCount++;
            }
            if (missingAssignee) {
                missingAssigneeCount++;
            }
            issues.add(new HygieneIssue(issue.key(), issue.summary(), issue.assignee(),
                    missingEstimate, missingAc, missingAssignee));
        }

        // totalIssues counts the EXAMINED set (after the optional type/assignee filter), so the
        // report's arithmetic stays self-consistent: issues.size() == totalIssues in every case.
        // The three counts are each a plain per-field tally, NOT a definition-of-ready verdict.
        return new BoardHygieneReport(issues.size(), missingEstimateCount,
                missingAcceptanceCriteriaCount, missingAssigneeCount, issues);
    }
}
