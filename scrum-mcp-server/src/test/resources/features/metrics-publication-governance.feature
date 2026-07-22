Feature: Metrics publication governance (human-in-the-loop, read-only safe)
  In order to keep a human in the loop before anything is published
  As a delivery lead
  I want preparing a publication to write nothing and committing to be refused in read-only mode

  Scenario: Preparing a publication returns a token and writes nothing
    Given the sprint metrics have no closed or active sprints
    When I prepare a metrics publication
    Then a non-null publication token is returned
    And no POST or PUT request was made to Jira

  Scenario: Committing a publication is refused in read-only mode
    When I commit a metrics publication with token "any-token"
    Then the commit is refused with an IllegalStateException
