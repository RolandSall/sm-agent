Feature: Logged hours vs estimate on the active sprint
  In order to understand where effort is going this sprint
  As a delivery lead
  I want logged work rolled up against estimates per assignee, with missing logs counted as zero

  Scenario: Logged hours and estimates are rolled up per assignee with per-assignee counts
    Given the active sprint contains the following issues
      | key    | summary     | assignee | loggedField | estimate | remaining |
      | PROJ-1 | Alice one   | Alice    | 4h          | 8h       | 4h        |
      | PROJ-2 | Alice two   | Alice    | 2h          | 4h       | 2h        |
      | PROJ-3 | Bob work    | Bob      | 2h          | 4h       | 2h        |
    When I request the workload report
    # Derived days at the default 8h/day: 6h -> 0.8d, 12h -> 1.5d, 2h -> 0.3d, 4h -> 0.5d.
    # issueCount = issues assigned; worklogCount = issues with logged > 0 (both are exact counts).
    Then the workload per assignee is
      | assignee | logged | originalEstimate | remaining | loggedDays | originalEstimateDays | issueCount | worklogCount |
      | Alice    | 6.0    | 12.0             | 6.0       | 0.8        | 1.5                  | 2          | 2            |
      | Bob      | 2.0    | 4.0              | 2.0       | 0.3        | 0.5                  | 1          | 1            |

  Scenario: An assignee with issues but no logged time is still returned (never-logged, not 0h worked)
    Given the active sprint contains the following issues
      | key    | summary     | assignee | loggedField | estimate | remaining |
      | PROJ-1 | Logged work | Alice    | 4h          | 8h       | 4h        |
      | PROJ-2 | No worklog  | Mallory  |             | 8h       | 8h        |
    When I request the workload report
    # Mallory has an assigned, estimated issue but no worklog. The tool does NOT bucket or drop her:
    # she is returned with logged 0 yet issueCount 1 and worklogCount 0 — the distinguishing signal.
    Then the workload per assignee is
      | assignee | logged | originalEstimate | issueCount | worklogCount |
      | Alice    | 4.0    | 8.0              | 1          | 1            |
      | Mallory  | 0.0    | 8.0              | 1          | 0            |
    And the workload totals are
      | assigneeCount | logged |
      | 2             | 4.0    |

  Scenario Outline: Logged hours convert to working-days at the given hours-per-day
    Given the active sprint contains the following issues
      | key    | assignee | loggedField |
      | PROJ-1 | Alice    | <logged>    |
    When I request the workload report at <hoursPerDay> hours per day
    Then the workload per assignee is
      | assignee | logged  | loggedDays |
      | Alice    | <hours> | <days>     |

    Examples:
      | logged | hoursPerDay | hours | days |
      | 4h     | 8           | 4.0   | 0.5  |
      | 8h     | 8           | 8.0   | 1.0  |
      | 6h     | 6           | 6.0   | 1.0  |
      | 4h     | 10          | 4.0   | 0.4  |

  Scenario Outline: The assignee filter keeps only the matching rows (blank = no filter)
    Given the active sprint contains the following issues
      | key    | assignee | loggedField |
      | PROJ-1 | Alice    | 4h          |
      | PROJ-2 | Bob      | 2h          |
    When I request the workload report filtered to assignee "<assignee>"
    Then the workload totals are
      | assigneeCount | logged  |
      | <assignees>   | <total> |

    Examples:
      | assignee | assignees | total |
      | Alice    | 1         | 4.0   |
      | Bob      | 1         | 2.0   |
      |          | 2         | 6.0   |

  Scenario Outline: A missing or negative logged-hours value contributes zero and no worklog count
    Given the active sprint contains the following issues
      | key    | assignee | loggedField | estimate |
      | PROJ-9 | <who>    | <logged>    | 8h       |
    When I request the workload report
    # The issue is still assigned (issueCount 1) but carries no positive logged time (worklogCount 0).
    Then the workload per assignee is
      | assignee | logged     | loggedDays | issueCount | worklogCount |
      | <who>    | <expected> | 0.0        | 1          | 0            |

    Examples:
      | who   | logged | expected |
      | Dave  | -5h    | 0.0      |
      | Carol |        | 0.0      |
