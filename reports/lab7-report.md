# Lab 7 — Software Testing

## Coverage before and after

The Lab 6 baseline contained 45 passing tests. It already had excellent service coverage, so Lab 7 focused on test strategy and oracle quality rather than inflating coverage with redundant assertions.

| Metric | Before Lab 7 | After Lab 7 |
|---|---:|---:|
| Tests | 45 | 62 |
| Overall line coverage | 259/277 (93.5%) | 259/277 (93.5%) |
| Overall branch coverage | 21/22 (95.5%) | 21/22 (95.5%) |
| Service line coverage | 82/82 (100%) | 82/82 (100%) |
| Service branch coverage | 14/14 (100%) | 14/14 (100%) |

The unchanged percentage is expected: the new tests cover the same production lines through isolated unit tests and real HTTP calls, providing different failure localization and stronger oracles rather than executing previously untouched lines.

| Service class | Covered lines | Covered branches |
|---|---:|---:|
| BookService | 17/17 | 0/0 |
| BorrowingService | 34/34 | 6/6 |
| MemberService | 7/7 | 0/0 |
| ReservationService | 24/24 | 8/8 |

## Test strategy added

`BorrowingServiceUnitTest` uses `@ExtendWith(MockitoExtension.class)` with mocked repositories. Its 14 cases execute without Spring or a database and verify results, state changes, error messages, lookup precedence, and absence of persistence on rejected operations.

`BorrowingHttpIntegrationTest` uses `@SpringBootTest(webEnvironment = RANDOM_PORT)` and `TestRestTemplate`. Its three tests exercise controller → service → repository → H2 for: creating a book/member/borrowing and observing `available=false`; a complete borrow/return cycle restoring `available=true`; and a duplicate borrow returning HTTP 409.

## Audit of the generated unit suite

The table classifies the final reviewed suite. “Meaningful” means the test asserts behavior required by the borrowing contract, not merely that a method returns.

| Test method | Classification | Reason / oracle |
|---|---|---|
| `borrowBook_availableBookCreatesFourteenDayLoan` | Meaningful | Verifies associations, availability transition, 14-day duration, zero fine, and persistence. |
| `borrowBook_missingBookThrowsAndStopsBeforeMemberLookup` | Meaningful | Verifies exact error, lookup precedence, and no save. |
| `borrowBook_missingMemberThrowsWithoutChangingAvailability` | Meaningful | Verifies exact error, unchanged book state, and no save. |
| `borrowBook_unavailableBookThrowsWithoutSaving` | Meaningful | Verifies conflict message and absence of persistence. |
| `returnBook_onTimeRestoresAvailabilityWithZeroFine` | Meaningful | Verifies return date, zero monetary value, availability, and save. |
| `returnBook_threeDaysLateChargesOneEuroFifty` | Meaningful | Verifies the fine formula, return date, and availability transition. |
| `returnBook_missingBorrowingThrowsWithoutSaving` | Meaningful | Verifies exact not-found behavior and no save. |
| `returnBook_alreadyReturnedThrowsWithoutSaving` | Meaningful | Covers the repeated-return guard and no mutation persistence. |
| `getActiveBorrowings_existingMemberReturnsRepositoryResult` | Meaningful | Verifies the active-only ordered repository query is used. |
| `getActiveBorrowings_missingMemberThrowsWithoutQueryingLoans` | Meaningful | Verifies member validation precedes the borrowing query. |
| `calculateFine_returnedBeforeDueDateIsZero` | Meaningful | Covers a negative day difference with an exact monetary oracle. |
| `calculateFine_returnedOnDueDateIsZero` | Meaningful | Covers the due-date boundary. |
| `calculateFine_oneDayLateIsFiftyCents` | Meaningful | Covers the first overdue boundary. |
| `calculateFine_sevenDaysLateIsThreeEurosFifty` | Meaningful | Covers multiplication across several overdue days. |

Final audit count: **14 Meaningful, 0 Weak, 0 Wrong Oracle**.

### Audit corrections

The initial generated draft had three weak patterns that were corrected before committing:

1. Exception tests originally asserted only the exception type. They now check the exact message and verify that `save` is never called.
2. The happy-path borrow test originally asserted only non-null output. It now verifies the exact 14-day relationship, zero fine, entity identity, availability mutation, and saved object.
3. Return tests originally checked only `fineAmount`. They now also assert return date, book availability, and persistence, which detects partial state transitions.

Scenarios still outside this focused suite include a clock crossing midnight during a call, repository infrastructure failures, and concurrent borrow attempts. The current service uses `LocalDate.now()` directly and the original specification explicitly scoped concurrency out; a future injectable clock and concurrency policy would make those tests meaningful.

## Deliberate bug experiments

### Bug 1 — fine rate changed from 0.50 to 1.00

The complete test run failed as expected: **6 failed and 56 passed**. The failing cases were:

- `BorrowingFineTest` parameter cases for one and seven days overdue.
- `BorrowingServiceTest.returnBook_chargesFiftyCentsForEachOverdueDay`.
- `BorrowingServiceUnitTest.returnBook_threeDaysLateChargesOneEuroFifty`.
- `BorrowingServiceUnitTest.calculateFine_oneDayLateIsFiftyCents`.
- `BorrowingServiceUnitTest.calculateFine_sevenDaysLateIsThreeEurosFifty`.

Early/on-time fine cases and tests unrelated to overdue calculation correctly did not fail. This shows that the fine tests use independent numeric oracles rather than reproducing the implementation constant.

### Bug 2 — zero fine changed to `null`

The complete run also failed: **10 failed and 52 passed**. The failures spanned layers:

- `BorrowingFineTest` early-return and exact-due-date cases.
- `BorrowingServiceUnitTest` early, exact-date, and on-time-return cases.
- `BorrowingServiceTest.returnBook_restoresAvailabilityAndRecordsReturnDate` and `activeBorrowings_excludesReturnedLoansAndOtherMembers`.
- Two `BorrowingControllerTest` borrowing/return lifecycle cases.
- `BorrowingHttpIntegrationTest.fullBorrowingCycleRestoresBookAvailability`.

Late-return cases and unrelated CRUD/reservation tests did not fail. Unit assertions detected the wrong value directly, while integration tests also exposed that `null` violates the non-null database constraint. This combination gives both precise diagnosis and end-to-end protection.

Both mutations were reverted. The final build is green with **62 tests, 0 failures, and 0 errors**.

## Acceptance criteria as evals

The Lab 3 acceptance criteria in `docs/AGENTS.md` function as evals because they turn a natural-language specification into observable pass/fail outcomes: status codes, exact error details, saved state, ordering, and forbidden side effects. A failing eval would be an implementation that looks plausible but violates one criterion—for example, allowing a duplicate active reservation, returning reservations in the wrong order, or changing book availability when cancelling. Automated tests are executable evals; human review checks whether those tests faithfully encode the acceptance criteria and use the correct oracle.

## Is AI-generated test code trustworthy?

AI-generated tests are useful scaffolding but are not trustworthy without review. First, the draft exception tests could have passed even if a rejected operation persisted data; adding `never().save(...)` made the no-side-effect requirement executable. Second, a non-null happy-path assertion would not detect a 7-day loan or a nonzero initial fine; exact date and monetary assertions were needed. The mutation experiment confirms the reviewed suite is effective: both deliberately plausible defects were caught, while unrelated tests remained green.
