Feature: Release queue on the active project
  In order to see what is ready to ship
  As a delivery lead
  I want issues in the release statuses counted and bucketed by status

  Scenario: Issues are counted and bucketed by status
    Given the project has 2 issues "Ready to Test" and 1 issue "In Review"
    When I request the release queue for statuses "Ready to Test" and "In Review"
    Then the release queue count is 3
    And the release queue by status is
      | status        | count |
      | Ready to Test | 2     |
      | In Review     | 1     |

  Scenario: The queue is empty when no issue is in those statuses
    Given the project has no issues in the release statuses
    When I request the release queue for statuses "Ready to Test" and "In Review"
    Then the release queue count is 0
