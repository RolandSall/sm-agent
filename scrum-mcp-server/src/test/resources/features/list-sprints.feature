Feature: List the board's sprints
  In order to target the right sprint given only the board id
  As a scrum agent
  I want to enumerate the board's sprints by id, full name and state

  Scenario: Listing sprints returns the board's active sprints with id, full name and state
    Given the board has active sprints "RIO Sprint 14" with id 201 and "PREP Sprint 3" with id 202
    When I list the board's sprints
    Then the sprints are
      | id  | name          | state  |
      | 201 | RIO Sprint 14 | active |
      | 202 | PREP Sprint 3 | active |

  Scenario: The state parameter selects the sprints in that state
    Given the board has active sprints "RIO Sprint 14" with id 201 and "PREP Sprint 3" with id 202
    And the board has closed sprints "RIO Sprint 13" with id 190
    When I list the board's sprints in state "closed"
    Then the sprints are
      | id  | name          | state  |
      | 190 | RIO Sprint 13 | closed |
