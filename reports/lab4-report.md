# Lab 4 - Design, Implementation and Deployment

Date: 11 November 2026

> AI-assisted report draft for student review. Application changes and tests
> are verified; the Docker build/run remains unverified because the local engine
> is not responding. The Dockerfile is a prepared deployment artifact, not
> evidence that a container has run successfully.

## 1. Design review findings

Baseline: `lab-3` (`2254c02`). The review was recorded before implementation in
[docs/lab4-design-review.md](../docs/lab4-design-review.md). Its findings were:

| Concern | Severity | Findings |
| --- | --- | --- |
| Separation of concerns | IMPORTANT | BookController performs record lookup, creation, update-field copying and deletion; MemberController performs persistence operations. Move these use cases to BookService and MemberService. BorrowingService and ReservationService already own their domain rules. |
| Layer coupling | CRITICAL | BookController and MemberController inject repositories directly, bypassing the service abstraction. Changes to persistence or use-case rules therefore force HTTP-layer edits. |
| API contract | CRITICAL | Every controller returns JPA entities. BookController and MemberController also accept them as request bodies. Borrowing and reservation responses recursively expose whole Member records, including email and membership information, although clients only need a member ID. Database relationships therefore determine the JSON contract. |
| Error handling | IMPORTANT | BorrowingExceptionHandler and ReservationExceptionHandler are scoped advice with duplicate 404/409 ProblemDetail mappings. Book/Member use ResponseStatusException. There is no global, consistent validation/500 response. An IllegalStateException outside the scoped controllers has no shared mapping. |
| OpenAPI | IMPORTANT | SpringDoc generates basic documentation, but controllers have no Tag, Operation or explicit status-response annotations and DTO schemas do not exist. Documentation currently uses /v3/api-docs rather than the required /api-docs. |
| Cohesion and abstractions | MINOR | The services are small and focused; no god class was found. Constructor injection and JpaRepository are already useful abstractions. Handwritten mapping is sufficient for this small project; a mapper framework would add unnecessary dependencies. |
| Deployment | IMPORTANT | No Dockerfile or .dockerignore exists. There is no repeatable image build or defined runtime JVM memory budget. |

The two CRITICAL issues were coupling and an unstable abstraction boundary.
Direct repository dependencies gave the Book/Member HTTP controllers persistence
responsibilities, reducing cohesion and mixing transport with use-case logic.
BookService and MemberService now isolate those operations, while controllers
handle HTTP, validation and DTO mapping. Exposing JPA objects coupled clients to
the persistence model: adding a relationship or a sensitive Member field could
silently alter every loan response. Explicit DTOs create a separate API
abstraction so those changes require an intentional transport-contract decision.

The remaining IMPORTANT issues were addressed with a global handler, explicit
OpenAPI annotations, an exported design artifact, and the multi-stage Dockerfile.
The latter still requires verification on a functioning Docker engine.

## 2. DTO decision

DTOs decouple response data from ORM relationships and prevent persistence
annotations from defining the public API. Before this lab, returning Borrowing
or Reservation serialized the related Member, including email and membership
details. BorrowingDTO and ReservationDTO now carry only `bookId` and `memberId`
for those relationships. This reduces unnecessary data exposure and lets the
Member entity evolve without automatically changing borrowing/reservation JSON.
The Member endpoint still deliberately returns MemberDTO's email field; DTOs
alone do not provide authentication or access control.

BookDTO and MemberDTO define input validation and useful creation defaults.
BookMapper and MemberMapper implement both `toDTO` and `toEntity`; services
ignore client-supplied IDs on creation and preserve the path ID on updates.
Borrowing/reservation input already consists of small ID request records, so
their mappers map domain results to response DTOs rather than accepting
client-controlled dates, fines or statuses. Every controller method signature
now uses DTOs or request records and no controller injects a repository.

This is a deliberate contract change: relationship objects become IDs, error
`detail` becomes `error`, and `/v3/api-docs` becomes `/api-docs`. Existing tests
were updated for those requirements while keeping their domain-behavior
assertions. Historical Lab 3's restriction against controller edits applied to
that increment, not this expressly requested Lab 4 redesign.

## 3. Pattern identification

- **Repository:** BookRepository (and the other JpaRepository interfaces)
  abstracts persistence operations behind domain-oriented interfaces. Services
  can request books or reservations without constructing SQL or managing an
  EntityManager. Controllers no longer depend directly on that abstraction.
- **Facade / Service Layer:** BorrowingService exposes `borrowBook`,
  `returnBook` and `getActiveBorrowings` as a small API over book, member and
  borrowing repositories, state changes and fine calculation. It hides that
  orchestration and its transaction boundary from BorrowingController. The
  new BookService and MemberService apply the same separation to CRUD use cases.

DTOs and explicit mappers additionally separate transport representation from
the domain. No extra framework or unnecessary strategy/factory hierarchy was
introduced for these small use cases.

## 4. Deployment reflection

The build stage uses `maven:3.9-eclipse-temurin-17` to package the application;
the runtime stage copies only the executable JAR into
`eclipse-temurin:17-jre-alpine`. Maven, source files and build caches stay out
of the final image. This reduces runtime contents and lets compilation and
execution use different environments, as described in the
[Docker multi-stage build documentation](https://docs.docker.com/build/building/multi-stage/).
The runtime uses a non-root user, port 8080, and heap settings `-Xmx256m -Xms128m`;
these settings limit the Java heap, not total container memory.

PostgreSQL would require adding its JDBC driver, provisioning a persistent
database, and overriding the datasource URL, username, password and the currently
explicit H2 driver class. Credentials belong in runtime configuration, not Git.
A production profile should disable the H2 console and demo SQL initialization,
replace `create-drop` with validation of a managed schema, and adopt a controlled
schema-management approach. Do not mix multiple initialization mechanisms;
Spring's [database initialization documentation](https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html)
explains these choices. The image must be rebuilt with the new driver, and
database-specific integration tests should verify the real platform. Domain
services and DTOs should generally remain unchanged.

## Verification and remaining deployment step

- 45 tests passed: the 37 earlier tests adapted to the specified contract plus
  5 global-error tests, 2 DTO contract tests and 1 OpenAPI contract test.
- Tests verify the required POST /borrowings 404 and 409 mappings, field errors,
  malformed requests, safe 500 responses, no nested member data, and all 14
  documented business operations with DTO field examples.
- Executable-JAR packaging succeeded. The JAR started on port 8085 with the same
  heap options as the Dockerfile. Live requests verified Swagger, OpenAPI,
  catalogue/member data, DTO borrowing/reservation flows and 400/404/409 errors.
- [docs/lab4-openapi.json](../docs/lab4-openapi.json) contains the generated
  design artifact, with its server URL normalized to the default port 8080.
- Docker's Windows and Linux-context engine APIs timed out. Its Windows
  background service was stopped, and this session could not start it. The
  backend WSL distribution was running, but its bundled CLI explicitly does
  not support use from that distribution. No engine reset or data deletion was
  performed. Restarting Docker Desktop is the pending environmental step.
- Consequently, no successful Docker image build, image ID or container startup
  is claimed. Run the commands in [lab4-verification.md](lab4-verification.md)
  once the engine is healthy to complete the deployment verification.

Lab 5 has not been started.
