# Lab 4 - Design review before implementation

Baseline: `lab-3` (`2254c02`). Reviewed the application, entity, repository,
service, controller and test packages before making Lab 4 changes.

| Concern | Severity | Findings |
| --- | --- | --- |
| Separation of concerns | IMPORTANT | BookController performs record lookup, creation, update-field copying and deletion; MemberController performs persistence operations. Move these use cases to BookService and MemberService. BorrowingService and ReservationService already own their domain rules. |
| Layer coupling | CRITICAL | BookController and MemberController inject repositories directly, bypassing the service abstraction. Changes to persistence or use-case rules therefore force HTTP-layer edits. |
| API contract | CRITICAL | Every controller returns JPA entities. BookController and MemberController also accept them as request bodies. Borrowing and reservation responses recursively expose whole Member records, including email and membership information, although clients only need a member ID. Database relationships therefore determine the JSON contract. |
| Error handling | IMPORTANT | BorrowingExceptionHandler and ReservationExceptionHandler are scoped advice with duplicate 404/409 ProblemDetail mappings. Book/Member use ResponseStatusException. There is no global, consistent validation/500 response. An IllegalStateException outside the scoped controllers has no shared mapping. |
| OpenAPI | IMPORTANT | SpringDoc generates basic documentation, but controllers have no Tag, Operation or explicit status-response annotations and DTO schemas do not exist. Documentation currently uses /v3/api-docs rather than the required /api-docs. |
| Cohesion and abstractions | MINOR | The services are small and focused; no god class was found. Constructor injection and JpaRepository are already useful abstractions. Handwritten mapping is sufficient for this small project; a mapper framework would add unnecessary dependencies. |
| Deployment | IMPORTANT | No Dockerfile or .dockerignore exists. There is no repeatable image build or defined runtime JVM memory budget. |

## Resulting decisions

- Route every controller through services and explicit DTO mappers. Add services
  for book/member CRUD; keep the existing borrowing/reservation business rules.
- Add BookDTO, MemberDTO, BorrowingDTO and ReservationDTO. Keep Book/Member
  request field names stable. Flatten borrowing/reservation relationships to
  bookId/memberId so nested entity graphs and member emails are not exposed.
- Replace scoped advice with one GlobalExceptionHandler and ErrorResponse DTO.
  The Lab 4 error contract is error + timestamp, with fieldErrors for validation.
  This deliberately supersedes the historical Lab 3 ProblemDetail contract.
- Add endpoint/status annotations, examples for every DTO field, explicit
  Swagger paths, and an OpenAPI regression test.
- Add the specified two-stage Java 17 Docker build and verify the image/runtime
  if the local Docker engine is available. Record actual results, not assumptions.

Lab 3's docs/AGENTS.md remains a historical specification. Its own introduction
states that it does not prescribe later labs; the user's Lab 4 instruction
authorizes the DTO/controller and error-contract changes required here.
