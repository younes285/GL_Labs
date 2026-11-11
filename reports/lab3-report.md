# Lab 3 - Requirements and Specification

Date: 11 November 2026

> AI-assisted report draft for student review. The specification, plan, code and
> tests were produced in this session; the observations below describe those
> artifacts rather than claiming a separate human review occurred.

## 1. Spec quality audit

The specification and plan were committed as `fe786c0` before any reservation
source code. [docs/AGENTS.md](../docs/AGENTS.md) contains seven business rules,
three endpoints, six acceptance criteria, exact error messages, and explicit
constraints. The rule texts and their observed implementation are audited here.

| Rule text | Classification | Actual code evidence |
| --- | --- | --- |
| BR-1: Only an unavailable book (`available=false`) can be reserved. Otherwise return 409 with detail `Book is available for direct borrowing.` | Precise enough | `ReservationService.reserveBook` checks `book.isAvailable()` and throws that exact message. `ReservationExceptionHandler.conflict` maps it to 409. AC-1 tests verify no reservation is saved. |
| BR-2: The book and member must exist. Resolve book first, then member; missing records return 404 with detail `Book not found` or `Member not found`. These lookups precede the business-rule checks below. | Precise enough | The first two lookups in `reserveBook` appear in the specified order. AC-5 tests request both unknown IDs and then an available book with an unknown member, verifying error precedence. |
| BR-3: Only an active member may reserve. After BR-1, an inactive member gets 409 with detail `Member is inactive.` | Precise enough | The second business check uses `member.isActive()`. Service and controller AC-5 tests verify the exception/detail and absence of new rows. |
| BR-4: In sequential requests, at most one ACTIVE reservation may exist for a book/member pair. Check this after BR-3; a duplicate returns 409 with detail `An active reservation already exists.` Other members may reserve that book. A cancelled reservation does not prevent the member from reserving again. | Precise enough for its stated sequential scope | `existsByBookIdAndMemberIdAndStatus` checks ACTIVE only. AC-2 tests cover duplicate rejection and another member; AC-4 tests cover reserving again after cancellation. No concurrent uniqueness guarantee is claimed. |
| BR-5: Creation generates the ID, records the current server-local date, and sets status ACTIVE. Creating, listing or cancelling a reservation must not change book availability, member fields, or any borrowing. | Precise enough | `reserveBook` constructs a new Reservation with null ID, `LocalDate.now()` and ACTIVE. The service never sets Book/Member fields or accesses BorrowingRepository. Tests verify server fields and unchanged domain state. |
| BR-6: A member's list contains only their ACTIVE reservations, ordered by ID ascending. An existing member with none gets 200 and `[]`; a missing member gets the BR-2 member error. Returned-book availability does not itself change an existing reservation's status or inclusion in this list. | Precise enough | `getActiveReservations` validates the member, then calls `findByMemberIdAndStatusOrderByIdAsc` with ACTIVE. Tests verify filtering, order, empty/missing members and continued inclusion after availability changes. |
| BR-7: Cancellation preserves the record and changes ACTIVE to CANCELLED. Cancelling an already CANCELLED record is idempotent (200, still CANCELLED). An unknown reservation ID gets 404 with detail `Reservation not found`. | Precise enough | `cancelReservation` looks up the record, sets CANCELLED and saves it; it does not delete. AC-4 tests repeat cancellation and verify stored history, list removal and unchanged availability. AC-5 verifies unknown IDs. |

All seven business rules matched their specified sequential behavior. There was
no observed business-rule failure to conceal or repair during the gap audit.
The following are genuine policy gaps or ambiguities, not additional features
silently implemented after the audit:

- **Ambiguous input policy:** the spec defines the two POST fields and prevents
  callers from controlling server fields, but does not say whether additional
  JSON properties must be rejected. Boot's existing Jackson configuration ignores
  them. `ReservationControllerTest.serverOwnedFieldsCannotBeOverriddenByClientJson`
  demonstrates that submitted id/status/date values are ignored and cannot
  override generated state. A future spec should explicitly choose ignore or
  reject; no new JSON policy was added during this audit.
- **Missing concurrent-use requirement:** BR-4 deliberately covers sequential
  requests. `reserveBook` performs an existence check followed by save, with no
  lock or database uniqueness rule for ACTIVE pairs. Transactions alone do not
  prevent simultaneous duplicates. A multi-user extension needs a concurrency
  policy and corresponding tests before that guarantee can be made.
- **Missing caller identity/access policy:** a member ID is input, not proof of
  identity. `ReservationController` contains no authentication/ownership check,
  and it returns nested member data consistently with existing controllers.
  Deciding who may create, list or cancel another person's reservation remains
  a requirements task; it was explicitly outside this lab's contract.
- **Missing allocation and retention policies:** cancellation history exists,
  but no retention duration, expiry or returned-book allocation order is defined.
  BR-6 therefore keeps a reservation ACTIVE even when its book becomes available.
  This behavior is tested rather than inventing notification or queue semantics.

These findings were recorded without adding fixes for unspecified policies, as
requested by the lab's gap-observation step.

## 2. AI as requirements partner - plan confirmation

The plan was recorded in [docs/lab3-plan.md](../docs/lab3-plan.md) before source
implementation. Its confirmation text was:

> I understand the specification as follows:
>
> 1. Create Reservation with Long id, Book book, Member member, LocalDate
>    reservationDate, and ReservationStatus status (ACTIVE/CANCELLED). Map it to
>    reservations, with generated identity and two required unidirectional
>    ManyToOne relationships. Existing entities stay unchanged.
> 2. Add POST /reservations, GET /members/{id}/reservations, and DELETE
>    /reservations/{id}. Creation returns 201, lists return 200, and cancellation
>    returns the retained CANCELLED record with 200. Both input IDs are required
>    and positive. Errors use the specified 400/404/409 responses.
> 3. Resolve book then member; require an unavailable book, then an active
>    member, then no duplicate ACTIVE pair. Create with today's date and ACTIVE
>    status. Lists filter by member and ACTIVE and sort by ID. Cancellation is
>    idempotent, preserves history, and never changes book availability.
> 4. Add ReservationRepository, ReservationService, ReservationController, and
>    separate reservation exception advice. Reuse existing repositories and
>    dependencies without editing existing entities/controllers or pom.xml.
> 5. Verify AC-1 through AC-6 in ReservationServiceTest and
>    ReservationControllerTest, run the full regression suite, and audit gaps
>    without silently adding policies beyond the written spec.

No clarifying questions were asked because the stated single-user behavior was
sufficiently concrete to implement. That does not mean the specification covers
all real-world needs: the audit above identifies missing policies and the
unknown-property ambiguity. The user directly authorized implementing and
pushing Lab 3; no separate post-plan approval exchange was collected, unlike the
interactive approval exercise described in the assignment.

## 3. Use-case connection

The main actor is a library member. The functional use cases are **reserve an
unavailable book**, **view active reservations**, and **cancel a reservation**.
Their preconditions, normal outcomes and alternative/error flows are expressed
as the business rules and acceptance criteria.

| Specification requirement | Classification | Reason |
| --- | --- | --- |
| Purpose: record, view and cancel interest in a borrowed book | Functional | Defines capabilities offered to the actor. |
| BR-1 through BR-7, including date, status, filtering and idempotency | Functional | Defines permitted operations and observable domain outcomes. |
| Entity field values and relationships | Functional data requirements | Defines the information the feature records and associates. |
| Generated ID, table/foreign-key mapping and STRING enum persistence | Non-functional implementation constraints | Constrains how that information is persisted. |
| Three HTTP paths, methods, bodies, response objects and status codes | Functional interface requirements | Defines how clients invoke use cases and observe success/failure. |
| Required positive IDs and exact ProblemDetail messages | Functional validation/error requirements | Defines alternative flows for invalid requests. |
| AC-1 through AC-6 | Functional acceptance requirements | Defines observable pass/fail examples for those flows. |
| Java 17, Spring Boot 3 and existing dependencies only | Non-functional compatibility constraints | Limits supported technology and integration choices. |
| No edits to existing entities/controllers/pom.xml; no migration tools | Non-functional change/architecture constraints | Limits the implementation's impact on existing code. |
| Package naming, explicit accessors, constructor injection and repository style | Non-functional maintainability constraints | Keeps the feature consistent and reviewable. |
| Transactional mutations and read-only list transactions | Non-functional reliability/architecture constraints | Defines transaction boundaries and atomicity of mutations. |
| No queue, expiry, fulfilment, authorization or Lab 4 implementation | Functional scope exclusions | Bounds the use cases this increment promises. |

Functional requirements were easier to specify precisely here: an exact status,
message, state change or ordered list becomes a direct test oracle. The supplied
non-functional constraints are also checkable, but production performance,
concurrent use, security and durability need additional measurable targets rather
than a broad claim such as "reliable" or "secure".

## 4. Spec-driven versus vibe-driven

Lab 1 already supplied a fairly detailed scaffold prompt, so it was not entirely
open-ended. It still left decisions such as error handling and package structure
to the implementation. Lab 3 made error precedence, cancellation idempotency,
record retention, duplicate behavior, ordering and exact messages explicit
before source code existed. That gave the implementation and tests a shared
contract, making review more objective and exposing what remained unspecified.
The output is better specified and easier to verify; passing tests alone does
not prove production readiness, particularly for the deliberately excluded
concurrency and access policies.

## Acceptance evidence

Tests use the existing `@SpringBootTest` / transactional style, with MockMvc for
controller requests. These are integration-style tests against real repositories
and H2, rather than isolated mocked unit tests.

| Criterion | Service evidence | Controller evidence |
| --- | --- | --- |
| AC-1 | `ac1_availableBookIsRejectedWithoutSaving` | `ac1_availableBookReturns409WithExactMessage` |
| AC-2 | `ac2_creationSetsServerFieldsWithoutChangingExistingDomainData`, `ac2_duplicatePairIsRejectedButAnotherMemberCanReserve` | `ac2_createReturns201AndDuplicateReturns409` |
| AC-3 | `ac3_listFiltersMemberAndStatusAndOrdersById` | `ac3_listExcludesCancelledAndOtherMemberReservations` |
| AC-4 | `ac4_cancelIsIdempotentRetainsHistoryAndPermitsReservingAgain` | `ac4_cancelPreservesRecordAndCanBeRepeated` |
| AC-5 | `ac5_missingResourcesUseSpecifiedLookupPrecedence`, `ac5_inactiveMemberCannotReserve` | `ac5_missingResourcesReturn404WithSpecifiedPrecedence`, `ac5_inactiveMemberReturns409` |
| AC-6 | Validation belongs at the request boundary | `ac6_invalidAndMalformedInputsReturn400` |

Additional tests verify server-owned fields cannot be overridden and returning
a book does not silently fulfil or cancel an existing reservation.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\maven.ps1 -o -B -ntp clean test
```

Result: **37 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**.
This includes all 21 Lab 1/2 tests and 16 new reservation tests. The build took
23.226 seconds. No existing entity/controller or pom.xml was changed; no new
dependency or migration tool was added. Lab 4 has not been started.
