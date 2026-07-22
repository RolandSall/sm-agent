Feature: Scope change on the active sprint
  In order to see how much the plan moved after the sprint started
  As a Scrum Master
  I want points added and removed since the sprint start totalled from the changelog

  Scenario: Points added and removed since sprint start are totalled from the changelog
    Given the active sprint started with a 5-point committed story, a 3-point story added and a 2-point story removed after start
    When I request the scope-change report
    # Deterministic arithmetic: committed-at-start includes the removed story's original commitment.
    # Added and removed stay SEPARATE ingredients — no signed net collapses them.
    Then the scope change is
      | sprint          | committedPoints | addedPoints | removedPoints |
      | RIO Scope Sprint | 7.0            | 3.0         | 2.0           |
    # The removed story is RETURNED raw with its points (a net-% would have hidden it under the added).
    And the scope-change items are
      | key    | bucket  | storyPoints |
      | PROJ-2 | added   | 3.0         |
      | PROJ-3 | removed | 2.0         |
