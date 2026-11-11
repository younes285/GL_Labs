# Lab 3 - Plan confirmation

Recorded after the specification was written and before reservation source code.

I understand the specification as follows:

1. Create Reservation with Long id, Book book, Member member, LocalDate
   reservationDate, and ReservationStatus status (ACTIVE/CANCELLED). Map it to
   reservations, with generated identity and two required unidirectional
   ManyToOne relationships. Existing entities stay unchanged.
2. Add POST /reservations, GET /members/{id}/reservations, and DELETE
   /reservations/{id}. Creation returns 201, lists return 200, and cancellation
   returns the retained CANCELLED record with 200. Both input IDs are required
   and positive. Errors use the specified 400/404/409 responses.
3. Resolve book then member; require an unavailable book, then an active
   member, then no duplicate ACTIVE pair. Create with today's date and ACTIVE
   status. Lists filter by member and ACTIVE and sort by ID. Cancellation is
   idempotent, preserves history, and never changes book availability.
4. Add ReservationRepository, ReservationService, ReservationController, and
   separate reservation exception advice. Reuse existing repositories and
   dependencies without editing existing entities/controllers or pom.xml.
5. Verify AC-1 through AC-6 in ReservationServiceTest and
   ReservationControllerTest, run the full regression suite, and audit gaps
   without silently adding policies beyond the written spec.

No clarifying questions are needed for the specified single-user behavior.
Concurrent requests, fulfilment/queue priority, expiry, and caller authorization
remain explicit gaps rather than assumed requirements.

## Authorization record

The user explicitly requested: "push lab2, then start lab3, then push it also."
That authorizes implementation and publication in the existing public repository.
The assignment describes a separate post-plan approval exchange; this session
uses the user's direct authorization and does not claim that a separate approval
exchange occurred.
