Feature: Estimate variance on the active sprint
  In order to judge where the effort spent no longer matches the estimate
  As a Scrum Master
  I want the raw logged-vs-estimate ingredients for every estimated issue, with no verdict applied

  Scenario: Every estimated issue is returned with its raw ingredients and no bucket applied
    Given the active sprint contains the following issues
      | key    | summary     | assignee | status                    | loggedField | estimate |
      | PROJ-1 | Runaway     | Alice    | In Development/In Progress | 20h         | 8h       |
      | PROJ-2 | Quick win   | Bob      | Closed/Done                | 1h          | 10h      |
      | PROJ-3 | Not started | Carol    | In Development/In Progress | 0h          | 8h       |
      | PROJ-4 | On track    | Dave     | In Development/In Progress | 8h          | 8h       |
    When I request the estimate-variance report
    # Raw deterministic ingredients only (usageRatio = logged/estimate + statusCategory + hours).
    # PROJ-4 is on-track (ratio 1.0) — the old code would have bucketed it "OK"; it is now RETURNED
    # unflagged, proving no verdict/filter ran. No advisory column exists anymore.
    Then the estimate variance per issue is
      | key    | usageRatio | statusCategory | loggedHours | originalEstimateHours |
      | PROJ-1 | 2.5        | In Progress    | 20.0        | 8.0                   |
      | PROJ-2 | 0.1        | Done           | 1.0         | 10.0                  |
      | PROJ-3 | 0.0        | In Progress    | 0.0         | 8.0                   |
      | PROJ-4 | 1.0        | In Progress    | 8.0         | 8.0                   |

  Scenario Outline: The minimum-estimate floor decides whether a small issue is considered
    Given the active sprint contains the following issues
      | key     | status                     | loggedField | estimate   |
      | PROJ-10 | In Development/In Progress  | 1h          | <estimate> |
    When I request the estimate-variance report with a minimum estimate of 2 hours
    Then issue "PROJ-10" returned status is "<returned>"

    Examples:
      | estimate | returned |
      | 1h       | false    |
      | 8h       | true     |
