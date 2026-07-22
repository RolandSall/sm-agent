Feature: Sprint flow raw ingredients for the daily standup
  In order to run a focused daily scrum
  As a Scrum Master
  I want every active-sprint ticket returned with its raw changelog-derived flow ingredients
  so the agent applies the stuck / stalled / reopened lines itself (no bucketing in code)

  # The tool does NOT bucket or apply thresholds. It returns a single FLAT list of every
  # active-sprint issue with its raw ingredients — statusCategory (passed through, never re-derived),
  # ageInStatusDays, daysSinceUpdated, reopenCount. The agent applies the team's lines.

  Scenario: Every active-sprint issue is returned as one flat list with raw ingredients
    Given the active sprint has a long-idle in-progress issue, a stale issue, a reopened issue and a healthy to-do issue
    When I request the sprint-flow report
    Then the flow issues are
      | key   | statusCategory | daysSinceUpdated | reopenCount |
      | RIO-1 | In Progress    |                  | 0           |
      | RIO-2 | In Progress    |                  | 0           |
      | RIO-3 | In Progress    |                  | 1           |
      | RIO-4 | To Do          | 0                | 0           |
    # No bucketing ran: the healthy To-Do issue the old buckets would have excluded is RETURNED.
    And the flow list has 4 issues
    And issue "RIO-4" is in the flow list
    # An issue with no status transitions has an UNKNOWN age — null, not "stuck".
    And issue "RIO-2" has a null ageInStatusDays

  Scenario Outline: The issue-type filter narrows the flat list (blank = no filter)
    Given the active sprint has the following issues
      | key    | type  |
      | RIO-10 | Story |
      | RIO-11 | Bug   |
    When I request the sprint-flow report filtered to issue type "<type>"
    Then the flow list has <count> issues
    And issue "<present>" is in the flow list

    Examples:
      | type  | count | present |
      | Story | 1     | RIO-10  |
      | Bug   | 1     | RIO-11  |
      |       | 2     | RIO-10  |
