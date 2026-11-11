# Lab 2 - XP pair review

Review performed after the four user stories passed 16 tests, before applying
the improvements below. This is Codex's self-review, not an independent human
review; the student remains responsible for evaluating it.

## Review output

1. **Error handling:** `BorrowingService` throws `EntityNotFoundException` and
   `IllegalStateException`, but no controller advice translates them. Missing
   records and unavailable books could surface as server errors. Map them to
   HTTP 404 and 409, preserving readable error details, and verify at API level.
2. **Edge case:** `returnBook` accepts an already returned loan. Returning an old
   loan after the same book has been borrowed again would incorrectly set the
   book available. Reject repeated returns before changing dates, fines, or book
   availability. Test this exact re-borrowing sequence.
3. **Coverage gaps:** service tests cover the happy paths, but the borrowing
   controller needs full request-flow coverage, missing-resource checks, invalid
   request checks, and assertions about state after rejected operations.
4. **Readability:** the fine rate is already a named decimal constant; give the
   14-day borrowing period a name too. Keep the fine calculator pure and reused
   by the return operation.
5. **Concurrency:** transactions make a single operation atomic, but two
   concurrent borrowers can both observe `available=true` without locking.
   Pessimistic locking or optimistic versioning needs a policy and a concurrency
   test before multi-user production use.
6. **XP practice:** each story had committed tests and an observed red phase
   before implementation. The red phases were compilation failures, which prove
   the required API was absent; they do not alone prove a test detects a wrong
   implementation. The final green assertions verify behavior. AI-generated
   tests still require human review, and these are assisted-session timings
   rather than a measured student-only velocity.

## Decisions

- Apply 1: add advice scoped to `BorrowingController`, using problem-detail
  responses for 404/409. Keep the established Lab 1 resource behavior intact.
- Apply 2: reject a second return with `IllegalStateException`; preserve the
  original loan and availability of any newer loan.
- Apply 3: add real-controller/real-database MockMvc tests for the full flow,
  repeat borrowing, repeat returning, missing IDs, and request validation.
- Apply 4: name the borrowing-period constant without changing its 14-day value.
- Defer 5: do not add locking in this simulated single-user sprint. This remains
  a documented limitation; multi-user safety is not claimed. A suitable locking
  strategy must be specified and tested before production deployment.

This completes the required review and applies more than two suggestions.
