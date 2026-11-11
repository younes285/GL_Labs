# AGENTS.md - Library API: Book Reservation Feature

## Project context

This is a Java 17 / Spring Boot 3 REST API for a university library. Existing
entities are Book, Member and Borrowing. Packages are under `com.library.api`.
This document is the contract for Lab 3; it does not prescribe later labs.

## Feature: Book Reservation

### Purpose

An active member may want to express interest in a book that someone else has
borrowed. A reservation records that interest so members can see and cancel
their own requests. This lab does not automatically allocate returned books,
send notifications, expire reservations, or implement a reservation queue.

### Business rules

- BR-1: Only an unavailable book (`available=false`) can be reserved. Otherwise
  return 409 with detail `Book is available for direct borrowing.`
- BR-2: The book and member must exist. Resolve book first, then member; missing
  records return 404 with detail `Book not found` or `Member not found`.
  These lookups precede the business-rule checks below.
- BR-3: Only an active member may reserve. After BR-1, an inactive member gets
  409 with detail `Member is inactive.`
- BR-4: In sequential requests, at most one ACTIVE reservation may exist for a
  book/member pair. Check this after BR-3; a duplicate returns 409 with detail
  `An active reservation already exists.` Other members may reserve that book.
  A cancelled reservation does not prevent the member from reserving again.
- BR-5: Creation generates the ID, records the current server-local date, and
  sets status ACTIVE. Creating, listing or cancelling a reservation must not
  change book availability, member fields, or any borrowing.
- BR-6: A member's list contains only their ACTIVE reservations, ordered by ID
  ascending. An existing member with none gets 200 and `[]`; a missing member
  gets the BR-2 member error. Returned-book availability does not itself change
  an existing reservation's status or inclusion in this list.
- BR-7: Cancellation preserves the record and changes ACTIVE to CANCELLED.
  Cancelling an already CANCELLED record is idempotent (200, still CANCELLED).
  An unknown reservation ID gets 404 with detail `Reservation not found`.

The scope is the single-user development workflow. Concurrency guarantees,
identity-based authorization, queue priority, fulfilment and expiry are not
specified. Record these as gaps rather than silently inventing policies.

### New entity: Reservation

Table name: `reservations`. Required Java fields:

| Field | Java type | Mapping / meaning |
| --- | --- | --- |
| id | Long | Generated identity primary key |
| book | Book | Required unidirectional ManyToOne, book_id foreign key |
| member | Member | Required unidirectional ManyToOne, member_id foreign key |
| reservationDate | LocalDate | Required server-local creation date |
| status | ReservationStatus | Required enum stored as STRING: ACTIVE or CANCELLED |

Use a no-argument constructor, an all-fields constructor, and explicit accessors
as in existing entities. No new seed reservations are required.

### REST endpoints required

| Method | Path | Request body | Response | HTTP status codes |
| --- | --- | --- | --- | --- |
| POST | `/reservations` | `{"bookId":1,"memberId":1}` | Saved Reservation as JSON, including nested book/member | 201, 400, 404, 409 |
| GET | `/members/{id}/reservations` | None | Array of ACTIVE Reservation objects | 200, 404 |
| DELETE | `/reservations/{id}` | None | CANCELLED Reservation object, retained in the database | 200, 404 |

POST accepts only bookId and memberId as its defined fields; both must be
non-null positive Long values. Missing, nonpositive, or malformed inputs get
400 under the existing framework validation behavior. Server-owned fields are
not accepted through the request record. Error responses for 404/409 use
ProblemDetail with the exact business message in `detail`.

### Acceptance criteria

- AC-1: Given an available book and an existing active member, POST returns
  409 with detail `Book is available for direct borrowing.` and saves nothing.
- AC-2: Given an unavailable book and an active member, POST returns 201 with
  a generated ID, today's reservationDate and status ACTIVE. The book remains
  unavailable. Repeating the pair returns 409 without another reservation;
  another active member can reserve the same book.
- AC-3: Given active and cancelled reservations for different members, GET
  returns only the specified member's ACTIVE records in ascending ID order.
  A member with none gets `[]`; an unknown member gets 404.
- AC-4: Given an ACTIVE reservation, DELETE returns 200 and status CANCELLED;
  the record remains stored and disappears from the active list. Repeating
  DELETE returns 200. The book remains unchanged, and a new reservation for
  that pair is allowed while the book remains unavailable. An unknown ID
  returns 404.
- AC-5: Missing book/member IDs return 404 with BR-2 details and precedence;
  an inactive member reserving an unavailable book gets 409. No rejected
  operation persists a reservation or changes book availability.
- AC-6: Missing or nonpositive POST IDs and malformed JSON return 400 without
  changing reservation data.

### What Codex must NOT do

- Do not modify any existing entity or controller, including the existing
  BorrowingExceptionHandler. Add a separate reservation controller and advice.
- Do not use any library not already in pom.xml or change pom.xml.
- Do not add Flyway or Liquibase migrations.
- Do not implement a queue, expiry, fulfilment or authorization policy without
  an additional specification. Do not implement Lab 4.

### Code style conventions observed from existing code

1. Put entities, repositories, services and controllers in their existing
   `com.library.api` subpackages. Use singular PascalCase class names and
   lowerCamelCase fields/methods.
2. Use constructor injection and final dependencies. Repository interfaces
   extend JpaRepository. Service mutations are transactional; lists are read-only.
3. Validate a request record with Jakarta constraints and `@Valid`. Use
   EntityNotFoundException and IllegalStateException for service errors and
   controller-scoped advice for 404/409 ProblemDetail responses, as in Lab 2.
