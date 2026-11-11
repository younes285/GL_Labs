# Sprint 1 - Backlog

Estimates were recorded before implementation. They are AI-proposed estimates
for ideal engineering time; the student should review them. Actuals measure this
assisted session, including test runs, and are not claimed as student work hours.

| ID | User story | Estimate (h) | Actual (h) | Status |
| --- | --- | ---: | ---: | --- |
| US-01 | As a librarian, I can record a borrowing so that the system tracks which member has which book. | 0.75 | Pending | TODO |
| US-02 | As a librarian, I can record a book return so that the book becomes available again. | 0.50 | Pending | TODO |
| US-03 | As a member, I can see my active borrowings so that I know which books I currently have. | 0.25 | Pending | TODO |
| US-04 | As a librarian, I can calculate the fine for an overdue book so that I can charge the right amount (EUR 0.50 per overdue day). | 0.25 | Pending | TODO |
| Total | | 1.75 | Pending | |

## Tasks

- US-01: write the borrowing test; observe failure; implement the transactional service and POST endpoint; verify green.
- US-02: write return tests; observe failure; implement return date, availability restoration and fine; verify green.
- US-03: write filtering tests; observe failure; implement active-loan query and GET endpoint; verify green.
- US-04: write fine boundary tests; observe failure; implement the pure calculator and reuse it for returns; verify green.
- Review the implementation, apply at least two improvements, rerun the suite, and write the report.

## Time log

Times are recorded in UTC using the session clock. Review/report overhead is
tracked separately rather than silently assigned to individual stories.

- US-01 started: 2026-11-11 13:57:27 UTC.
