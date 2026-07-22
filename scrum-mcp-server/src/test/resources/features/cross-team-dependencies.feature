Feature: Cross-team dependency radar for one issue (raw ingredients)
  In order to reason myself about which other teams my work is waiting on
  As a delivery lead on team RIO
  I want EVERY Dependency issue on my issue's Epic returned raw — open and resolved, both directions —
  with the reporting team surfaced, so the agent applies the direction/open rule (no filter in code)

  # Cross-team dependencies are modelled as Dependency-type issues linked to an Epic (Epic Link),
  # NOT as Jira issue links. The tool no longer filters: it returns every Dependency on the Epic with
  # its raw status, blockingTeam (Depend On) and waitingTeam (Dependent/s), plus ourTeam. The agent
  # keeps the OPEN ones where waitingTeam == ourTeam and blockingTeam != ourTeam.

  Scenario: Every Dependency on the issue's Epic is returned raw — resolved and reverse-direction included
    Given issue "VAL-100" is on Epic "VAL-1"
    And the Epic "VAL-1" has these Dependency issues
      | key     | summary            | dependOn | dependents | status | deliverySprint |
      | DEP-501 | Waiting on Pricing | ROMA     | RIO        | Open   | RIO Sprint 5   |
      | DEP-502 | Already solved     | ROMA     | RIO        | Solved | RIO Sprint 5   |
      | DEP-503 | ROMA waits, not us | RIO      | ROMA       | Open   | RIO Sprint 5   |
    When I request the dependencies of "VAL-100"
    # The tool returns all three unfiltered; the agent keeps DEP-501 (open, RIO waiting, ROMA blocks).
    # DEP-502 (Solved) and DEP-503 (Depend On == RIO, reverse direction) would have been dropped before.
    Then the reporting team is "RIO"
    And the dependencies are
      | key     | status | blockingTeam | waitingTeam | epicKey | deliverySprint |
      | DEP-501 | Open   | ROMA         | RIO         | VAL-1   | RIO Sprint 5   |
      | DEP-502 | Solved | ROMA         | RIO         | VAL-1   | RIO Sprint 5   |
      | DEP-503 | Open   | RIO          | ROMA        | VAL-1   | RIO Sprint 5   |

  Scenario: An issue with no Epic has no cross-team Dependencies
    Given issue "VAL-900" has no Epic
    When I request the dependencies of "VAL-900"
    Then the reporting team is "RIO"
    And no cross-team dependency is reported
