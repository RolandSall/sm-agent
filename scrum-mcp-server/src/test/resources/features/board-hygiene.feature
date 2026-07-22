Feature: Board hygiene gaps on the active sprint
  In order to keep the active sprint ready for delivery
  As a delivery lead
  I want the per-field readiness signals for every issue so I can apply my team's definition-of-ready

  Scenario: Per-field signals are returned for every issue, clean ones included
    Given the active sprint has one clean issue plus issues missing estimate, acceptance criteria and assignee
    When I request the board hygiene report
    # The clean issue is returned too (not filtered out), with all three signals false.
    # Deterministic per-field ingredients — the caller applies its own definition-of-ready.
    Then the hygiene per issue is
      | key    | missingEstimate | missingAcceptanceCriteria | missingAssignee |
      | PROJ-1 | false           | false                     | false           |
      | PROJ-2 | true            | false                     | false           |
      | PROJ-3 | false           | true                      | false           |
      | PROJ-4 | false           | false                     | true            |
    # totalIssues = every issue examined (clean PROJ-1 included). The three counts are deterministic
    # per-field tallies — NO OR-combined "issues with gaps" number is reported, so the caller applies
    # its own definition-of-ready.
    And the hygiene totals are
      | totalIssues | missingEstimateCount | missingAcceptanceCriteriaCount | missingAssigneeCount |
      | 4           | 1                    | 1                              | 1                    |

  Scenario: An all-green sprint reports zero on every field
    Given the active sprint has only fully-populated issues
    When I request the board hygiene report
    Then the hygiene per issue is
      | key    | missingEstimate | missingAcceptanceCriteria | missingAssignee |
      | PROJ-1 | false           | false                     | false           |
    And the hygiene totals are
      | totalIssues | missingEstimateCount | missingAcceptanceCriteriaCount | missingAssigneeCount |
      | 2           | 0                    | 0                              | 0                    |
