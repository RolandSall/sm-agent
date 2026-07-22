Feature: Stories ready for the PO to test
  In order to know what dev-complete work is waiting for acceptance
  As a product owner
  I want stories in the "ready to test" statuses listed with their acceptance criteria and epic

  Scenario: A story in the default "Ready for Acceptance" status is listed with its AC and epic
    Given a story "VAL-1" summary "Login page" assignee "Alice" with AC "User can log in" under epic "VAL-100" in status "Ready for Acceptance"
    When I request the stories to test with the default statuses
    Then the stories to test are
      | key   | assignee | acceptanceCriteria | epicKey | status               |
      | VAL-1 | Alice    | User can log in    | VAL-100 | Ready for Acceptance |

  Scenario: Only the stories the status filter returns are listed
    Given a story "VAL-1" summary "Login page" assignee "Alice" with AC "User can log in" under epic "VAL-100" in status "Ready for Acceptance"
    When I request the stories to test with the default statuses
    Then the stories to test are
      | key   |
      | VAL-1 |
    And the stories to test list does not contain "VAL-9"

  Scenario: A custom status set drives which stories are returned
    Given a story "VAL-2" summary "Logout" assignee "Bob" with AC "User can log out" under epic "VAL-100" in status "Under Validation"
    When I request the stories to test with statuses "Under Validation" and "Under Testing"
    Then the stories to test are
      | key   | status           |
      | VAL-2 | Under Validation |
