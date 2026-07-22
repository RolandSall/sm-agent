Feature: Sprint velocity metrics
  In order to forecast how much the team can commit to
  As a delivery lead
  I want velocity averaged over completed points from recent closed sprints

  Scenario: Velocity averages completed points over closed sprints
    Given there are 2 closed sprints completing 8 and 12 points
    When I request the velocity report
    Then the velocity summary is
      | averageVelocity | sprintCount |
      | 10.0            | 2           |

  Scenario: The sprints override limits the average to the most recent sprints
    Given there are 3 closed sprints completing 6, 8 and 10 points
    When I request the velocity report for the last 2 sprints
    Then the velocity summary is
      | averageVelocity | sprintCount |
      | 9.0             | 2           |
