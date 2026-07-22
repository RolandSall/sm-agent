Feature: Cross-team dependency radar across the active sprint (raw ingredients)
  In order to reason myself about every other team my whole sprint is waiting on
  As a delivery lead on team RIO
  I want EVERY Dependency issue on my sprint's Epics returned raw — open and resolved, both
  directions — with the reporting team surfaced, so the agent applies the rule (no filter in code)

  # The radar takes the Epics of the active-sprint stories and returns every Dependency-type issue
  # linked to those Epics, unfiltered, plus ourTeam. The agent keeps the blockers it faces
  # (Dependent/s == ourTeam, Depend On != ourTeam, open).

  Scenario: Every Dependency on the sprint's Epics is returned raw — resolved and reverse-direction included
    Given the active sprint has these stories
      | key     | summary     | type  | status      | epic  |
      | VAL-100 | Story on E1 | Story | In Progress | VAL-1 |
      | VAL-200 | Story on E2 | Story | To Do       | VAL-2 |
    And these Dependency issues exist on those Epics
      | key     | summary            | dependOn | dependents | status | epic  | deliverySprint |
      | DEP-501 | Waiting on Pricing | ROMA     | RIO        | Open   | VAL-1 | RIO Sprint 5   |
      | DEP-502 | Already solved     | ROMA     | RIO        | Solved | VAL-1 | RIO Sprint 5   |
      | DEP-503 | ROMA waits, not us | RIO      | ROMA       | Open   | VAL-2 | RIO Sprint 5   |
      | DEP-504 | Waiting on Data    | DATA     | RIO        | Open   | VAL-2 | RIO Sprint 6   |
    When I request the sprint dependency radar
    # All four returned unfiltered; the agent keeps DEP-501 and DEP-504 (open, RIO waiting). DEP-502
    # (Solved) and DEP-503 (Depend On == RIO, reverse direction) would have been dropped before.
    Then the reporting team is "RIO"
    And the dependencies are
      | key     | status | blockingTeam | waitingTeam | epicKey | deliverySprint |
      | DEP-501 | Open   | ROMA         | RIO         | VAL-1   | RIO Sprint 5   |
      | DEP-502 | Solved | ROMA         | RIO         | VAL-1   | RIO Sprint 5   |
      | DEP-503 | Open   | RIO          | ROMA        | VAL-2   | RIO Sprint 5   |
      | DEP-504 | Open   | DATA         | RIO         | VAL-2   | RIO Sprint 6   |
