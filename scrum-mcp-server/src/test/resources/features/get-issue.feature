Feature: Fetch a single curated issue
  In order to inspect one ticket without a raw Jira payload
  As a delivery lead
  I want a curated view exposing just the fields the playbooks need

  Scenario: The curated issue exposes the fields the playbooks read
    Given issue "PROJ-42" exists with summary "Wire the gateway", status "In Progress", 5 story points, label "backend" and a Blocks link to "OTHER-9"
    When I request issue "PROJ-42"
    Then the issue is
      | key     | summary          | status      | storyPoints |
      | PROJ-42 | Wire the gateway | In Progress | 5.0         |
    And the issue has label "backend"
    And the issue has a "Blocks" link to "OTHER-9"
