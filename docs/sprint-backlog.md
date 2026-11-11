# Sprint 1 - Backlog

Estimates were recorded before implementation. They are AI-proposed estimates
for ideal engineering time; the student should review them. Actuals measure this
assisted technical session, including test runs, and are not claimed as student
work hours. Report writing, committing, and publishing are outside this timing.

| ID | User story | Estimate (h) | Actual (h) | Status |
| --- | --- | ---: | ---: | --- |
| US-01 | As a librarian, I can record a borrowing so that the system tracks which member has which book. | 0.75 | 0.0572 | DONE |
| US-02 | As a librarian, I can record a book return so that the book becomes available again. | 0.50 | 0.0183 | DONE |
| US-03 | As a member, I can see my active borrowings so that I know which books I currently have. | 0.25 | 0.0206 | DONE |
| US-04 | As a librarian, I can calculate the fine for an overdue book so that I can charge the right amount (EUR 0.50 per overdue day). | 0.25 | 0.0281 | DONE |
| Shared review and live verification | Not separately estimated; included in the story estimates. | - | 0.0864 | DONE |
| Total | | 1.75 | 0.2106 | DONE |

## Tasks

- US-01: write the borrowing test; observe failure; implement the transactional service and POST endpoint; verify green.
- US-02: write return tests; observe failure; implement return date, availability restoration and fine; verify green.
- US-03: write filtering tests; observe failure; implement active-loan query and GET endpoint; verify green.
- US-04: write fine boundary tests; observe failure; implement the pure calculator and reuse it for returns; verify green.
- Review the implementation, apply at least two improvements, rerun the suite, and write the report.

## Time log

Times are recorded in UTC using the session clock. Review/report overhead is
tracked separately rather than silently assigned to individual stories.

- US-01: 13:57:27 to 14:00:53 UTC, 206 seconds.
- US-02: 14:00:53 to 14:01:59 UTC, 66 seconds.
- US-03: 14:01:59 to 14:03:13 UTC, 74 seconds.
- US-04: 14:03:13 to 14:04:54 UTC, 101 seconds.
- Review, regression tests and live verification: 14:04:54 to 14:10:05 UTC, 311 seconds.
- Date for every interval: 11 November 2026.
- Total: 758 seconds / 3600 = 0.210556 hours (rounded in the table).
- Velocity ratio: 0.210556 / 1.75 = 0.1203, below 1.0.

Story estimates were frozen in commit `6efbc4d` before implementation. The
initial US-01 interval also includes reading the lab and setting up the backlog.
Shared overhead is counted once. These observations compare an assisted session
with proposed ideal engineering hours, not a controlled measurement of a
student's productivity; personal study and review time must be recorded by the
student separately.
