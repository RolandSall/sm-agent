Feature: Delivery-risk raw ingredients on the active sprint
  In order to reason about which stories are most likely to slip
  As a delivery lead on team RIO
  I want the sprint's stories, the cross-team Dependency issues on their Epics, and the team's capacity
  returned raw so the agent can join them by Epic and judge the risk itself (no verdict in code)

  # The tool does NOT filter or count. It returns every Dependency on the sprint's Epics; the agent
  # keeps the OPEN ones where RIO is the waiting side (Dependent/s == RIO, Depend On != RIO), joins
  # them to stories by epicKey, and reasons about the level.

  Scenario: The sprint's stories and the Dependencies on their Epics are returned raw, with capacity
    Given the active sprint has these stories
      | key     | summary       | type  | status      | epic  | points |
      | VAL-100 | Heavy blocked | Story | In Progress | VAL-1 | 8      |
    And these Dependency issues exist on those Epics
      | key     | summary            | dependOn | dependents | status | epic  |
      | DEP-501 | Waiting on Pricing | ROMA     | RIO        | Open   | VAL-1 |
    When I request the sprint risk with committed 40.0 and available 30.0 points
    Then the risk stories are
      | key     | epicKey | storyPoints | unestimated |
      | VAL-100 | VAL-1   | 8.0         | false       |
    And the cross-team dependencies are
      | key     | blockingTeam | waitingTeam | status | epicKey |
      | DEP-501 | ROMA         | RIO         | Open   | VAL-1   |
    And the remaining capacity is -10.0

  Scenario: Two stories under the same Epic are both returned; the Epic's Dependency appears once
    Given the active sprint has these stories
      | key     | summary | type  | status      | epic  | points |
      | VAL-100 | Story A | Story | In Progress | VAL-1 | 3      |
      | VAL-101 | Story B | Story | To Do       | VAL-1 | 2      |
    And these Dependency issues exist on those Epics
      | key     | summary      | dependOn | dependents | status | epic  |
      | DEP-501 | Waiting on X | ROMA     | RIO        | Open   | VAL-1 |
    When I request the sprint risk with committed 10.0 and available 30.0 points
    Then the risk stories are
      | key     | epicKey |
      | VAL-100 | VAL-1   |
      | VAL-101 | VAL-1   |
    And the cross-team dependencies are
      | key     | epicKey |
      | DEP-501 | VAL-1   |

  Scenario: All Dependencies on the Epics are returned raw — resolved and reverse-direction included
    Given the active sprint has these stories
      | key     | summary | type  | status      | epic  | points |
      | VAL-400 | Heavy   | Story | In Progress | VAL-4 | 8      |
    And these Dependency issues exist on those Epics
      | key   | summary         | dependOn | dependents | status | epic  |
      | DEP-A | We wait, open   | ROMA     | RIO        | Open   | VAL-4 |
      | DEP-B | We wait, solved | ROMA     | RIO        | Solved | VAL-4 |
      | DEP-C | We block Roma   | RIO      | ROMA       | Open   | VAL-4 |
    When I request the sprint risk with committed 10.0 and available 30.0 points
    # The tool returns all three; the agent filters to DEP-A (open, RIO waiting).
    Then the cross-team dependencies are
      | key   | blockingTeam | waitingTeam | status |
      | DEP-A | ROMA         | RIO         | Open   |
      | DEP-B | ROMA         | RIO         | Solved |
      | DEP-C | RIO          | ROMA        | Open   |

  Scenario: A story on an Epic with no Dependencies returns none, and positive capacity headroom
    Given the active sprint has these stories
      | key     | summary     | type  | status | epic  | points |
      | VAL-200 | Small clean | Story | To Do  | VAL-2 | 2      |
    And no Dependency issues exist on those Epics
    When I request the sprint risk with committed 10.0 and available 30.0 points
    Then the risk stories are
      | key     | epicKey | storyPoints |
      | VAL-200 | VAL-2   | 2.0         |
    And no cross-team dependencies are returned
    And the remaining capacity is 20.0
