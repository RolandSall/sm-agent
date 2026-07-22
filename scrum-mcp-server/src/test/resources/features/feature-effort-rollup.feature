Feature: Per-feature (Epic) effort roll-up
  In order to spot features where logged effort is drifting from the estimate
  As a delivery lead
  I want each Epic's child stories rolled up into effort ingredients

  Scenario: An Epic's children are summed into effort ingredients
    Given the following epics
      | key     | summary  | devJobPoints |
      | VAL-100 | Checkout | 40.0         |
    And epic "VAL-100" has the following children
      | key   | status      | points | estimate | logged |
      | VAL-1 | In Progress | 2.5    | 20h      | 20h    |
      | VAL-2 | In Progress | 2.0    | 16h      | 10h    |
    When I request the feature effort roll-up for epics "VAL-100"
    Then the feature roll-up for "VAL-100" is
      | storyCount | sumStoryPoints | sumEstimateHours | sumLoggedHours | devJobPoints |
      | 2          | 4.5            | 36.0             | 30.0           | 40.0         |

  Scenario: Logged hours use the aggregate field so sub-task time is included
    Given the following epics
      | key     | summary  | devJobPoints |
      | VAL-200 | Payments | 10.0         |
    And epic "VAL-200" has the following children
      | key   | status      | points | logged | aggregateLogged |
      | VAL-3 | In Progress | 3.0    | 1h     | 5h              |
    When I request the feature effort roll-up for epics "VAL-200"
    # Aggregate logged (5h, incl. sub-tasks) wins over the issue's own 1h.
    Then the feature roll-up for "VAL-200" is
      | sumLoggedHours |
      | 5.0            |

  Scenario: With no epics given, scope is derived from the active sprint
    Given the active sprint contains the following issues
      | key          | status      | epic    |
      | VAL-SPRINT-1 | In Progress | VAL-100 |
    And the following epics
      | key     | summary  | devJobPoints |
      | VAL-100 | Checkout | 40.0         |
    And epic "VAL-100" has the following children
      | key   | status      | points | estimate | logged |
      | VAL-1 | In Progress | 2.5    | 20h      | 20h    |
    When I request the feature effort roll-up with no epics
    Then the feature roll-up for "VAL-100" is
      | storyCount | sumStoryPoints |
      | 1          | 2.5            |
