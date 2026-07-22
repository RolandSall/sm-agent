Feature: Work-in-progress limits on the active sprint
  In order to spot overloaded team members early
  As a delivery lead
  I want in-progress work counted per assignee against a limit

  Scenario: In-progress work is counted per assignee with the supplied limit echoed, no verdict
    Given the active sprint has Bob with 3 in-progress issues and Alice with 1 in-progress issue
    When I request the WIP status with a per-assignee limit of 2
    # Bob's count (3) exceeds the supplied limit (2) but the tool returns the raw count and the
    # echoed limit only — no over flag. Judging over/under is the agent's job, not the handler's.
    Then the WIP per assignee is
      | assignee | count | limit |
      | Bob      | 3     | 2     |
      | Alice    | 1     | 2     |
    And the team WIP is
      | inProgress |
      | 4          |

  Scenario: Issues that are not in an In Progress category are excluded
    Given the active sprint has 1 in-progress issue plus a To Do and a Done issue
    When I request the WIP status with a per-assignee limit of 5
    Then the team WIP is
      | inProgress |
      | 1          |

  Scenario: Filtering WIP to issue type Story excludes an in-progress Bug
    Given the active sprint has 2 in-progress Stories and 1 in-progress Bug
    When I request the WIP status filtered to issue type "Story"
    # Arithmetic: 3 in-progress issues, but only the 2 Stories survive the type filter.
    Then the team WIP is
      | inProgress |
      | 2          |

  Scenario: The team in-progress count is summed with the supplied team limit echoed, no verdict
    Given the active sprint has Bob with 3 in-progress issues and Alice with 1 in-progress issue
    When I request the WIP status with a team limit of 3
    # Team sum (4) exceeds the supplied limit (3) but the tool returns the raw sum and the echoed
    # limit only — no over flag.
    Then the team WIP is
      | inProgress | limit |
      | 4          | 3     |

  Scenario: With no active sprint the WIP report is empty
    Given there is no active sprint
    When I request the WIP status with a per-assignee limit of 2
    Then the team WIP is
      | inProgress |
      | 0          |
    And no assignees are reported

  Scenario: With an active sprint but no issues the WIP report is zeroed
    Given the active sprint has no issues
    When I request the WIP status with a per-assignee limit of 2
    Then the team WIP is
      | inProgress |
      | 0          |
    And no assignees are reported
