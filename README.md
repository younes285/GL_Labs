# Library API — Software Engineering Labs 2026–2027

Java 17 / Spring Boot 3 Maven REST API for a university library, through Lab 4.
Later labs will extend the same project, one at a time.

## Run on this Windows machine

From the `library-api` directory:

```powershell
$env:SERVER_PORT = '8082'
powershell -ExecutionPolicy Bypass -File .\scripts\maven.ps1 spring-boot:run
```

Existing services occupy ports 8080 and 8081 on this machine, so the commands
here select 8082. The default remains 8080 when `SERVER_PORT` is unset.

The script uses project-local Maven and a Java 17 installation selected through
the ignored `.tools/java-home.txt` file (or a JDK extracted into `.tools`). On this
machine the selected Java 17 is bundled with PyCharm Community Edition 2022.3.1.
It sets `JAVA_HOME` and `PATH` only for the command, so the existing Java 8
installation is unaffected. Maven dependencies stay in the ignored `.m2`
directory. Machine-specific tool files are not included in Git.

On another machine, install JDK 17 (or 21) and Maven 3.8+ and use:

```sh
java -version
mvn -version
mvn spring-boot:run
```

Stop the application with `Ctrl+C`. Every application restart recreates the
in-memory database with five books and three members.

If Maven says it cannot find a suitable main class after compiled files were
removed, clear its incremental-build metadata and restart:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\maven.ps1 clean spring-boot:run
```

## Try it

| Address | Purpose |
| --- | --- |
| http://localhost:8082/books | Five sample books as JSON |
| http://localhost:8082/members | Three sample members as JSON |
| http://localhost:8082/swagger-ui.html | Interactive API documentation |
| http://localhost:8082/api-docs | OpenAPI JSON |
| http://localhost:8082/h2-console | Development database console |
| http://localhost:8082/actuator/health | Application health |

H2 login: driver `org.h2.Driver`, JDBC URL
`jdbc:h2:mem:librarydb`, username `sa`, empty password.
The H2 console and unauthenticated endpoints are for this local development lab.

```powershell
Invoke-RestMethod http://localhost:8082/books
Invoke-RestMethod http://localhost:8082/members

$book = @{
    isbn = '9780000000001'
    title = 'A New Book'
    author = 'Example Author'
    genre = 'Reference'
    publishedYear = 2026
    available = $true
} | ConvertTo-Json
Invoke-RestMethod http://localhost:8082/books -Method Post -ContentType 'application/json' -Body $book
```

| Method | Path | Success |
| --- | --- | --- |
| GET | `/books` | 200, book list |
| GET | `/books/{id}` | 200, one book |
| POST | `/books` | 201, saved book and Location header |
| PUT | `/books/{id}` | 200, updated book |
| DELETE | `/books/{id}` | 204 |
| GET | `/members` | 200, member list |
| GET | `/members/{id}` | 200, one member |
| POST | `/members` | 201, saved member and Location header |
| POST | `/borrowings` | 201, borrowing with a 14-day due date |
| PUT | `/borrowings/{id}/return` | 200, returned borrowing and calculated fine |
| GET | `/members/{id}/borrowings` | 200, only that member's active borrowings |
| POST | `/reservations` | 201, an ACTIVE reservation for an unavailable book |
| GET | `/members/{id}/reservations` | 200, that member's ACTIVE reservations ordered by ID |
| DELETE | `/reservations/{id}` | 200, retained CANCELLED reservation; repeatable |

Missing records return 404; invalid request fields return 400. IDs are generated
by the database, and create operations ignore IDs supplied by clients.

Borrow a book with `POST /borrowings` and JSON `{"bookId":1,"memberId":1}`.
Returning it requires no body. Borrowing an unavailable book or returning a loan
twice returns 409 with the error response described below. The overdue fine is EUR 0.50
per day; early and on-time returns cost 0.00. Borrowing and return operations
update the loan and book in a single transaction.

Reserve an unavailable book with `POST /reservations` and JSON
`{"bookId":1,"memberId":2}`. The member must be active. Available books,
inactive members and duplicate ACTIVE pairs return 409. Cancellation preserves
history and does not change book availability. Cancelling again returns 200;
the member may reserve that book again while it remains unavailable. This lab
does not allocate returned books, expire reservations or enforce a queue.

The [historical Lab 3 specification](docs/AGENTS.md) defines the reservation
rules, lookup precedence and AC-1 through AC-6. Lab 4 changes response formatting
to DTOs and a common error body while retaining those domain rules. Caller
authentication and multi-user concurrency guarantees are outside this increment.

## API contract after Lab 4

Every controller accepts or returns DTOs, never JPA entities. BookDTO and
MemberDTO retain their catalogue/member fields. BorrowingDTO and ReservationDTO
contain `bookId` and `memberId` rather than nested book/member objects; retrieving
a loan no longer reveals the member's email or membership record.

Errors consistently contain `error` and an ISO timestamp:

```json
{"error":"Book not found","timestamp":"2026-11-11T14:00:00Z"}
```

Validation errors also include `fieldErrors`, keyed by field name. Missing
records return 404, conflicting operations return 409, malformed/invalid requests
return 400, and unexpected failures return a generic 500 message. Clients of
Lab 2/3 must change `detail` to `error`, nested `book.id` to `bookId`, and nested
`member.id` to `memberId`. OpenAPI JSON moved from `/v3/api-docs` to `/api-docs`.
Earlier Git tags preserve the previous contracts.

## Understand the project (Lab 1, Step D)

`LibraryApiApplication` starts Spring Boot. Its `com.library.api` package is above
the other packages, so Spring discovers the controllers and repositories and JPA
discovers the entities without extra scanning configuration.

- **Entities** (`entity/`): `Book`, `Member`, and `Borrowing` map Java objects to
  database tables. Each has a generated `Long` ID and a no-argument constructor
  for JPA. `LocalDate` represents dates without a time zone; `BigDecimal` represents
  fines without binary floating-point rounding. `Borrowing` has two `ManyToOne`
  relationships because many historical loans can refer to the same book/member.
- **Repositories** (`repository/`): each interface extends `JpaRepository`, which
  supplies database operations such as `findAll`, `findById`, `save`, and `delete`.
  Spring Data creates the implementation at runtime.
- **Controllers** (`controller/`): validate HTTP input, delegate to services, map
  domain objects to DTOs, and select HTTP status codes. They do not inject repositories.
- **Services** (`service/`): implement use cases and transaction boundaries.
- **DTOs and mappers** (`dto/`, `mapper/`): define the transport contract and
  translate between that contract and domain objects.

The original Lab 1 CRUD flow is **HTTP → controller → repository → H2**, with
entities carrying the data. Lab 2 adds **BorrowingController → BorrowingService →
repositories**, placing borrowing rules and transaction boundaries in the
service. The service also provides a pure `calculateFine` method. Lab 4 replaces
scoped handlers with GlobalExceptionHandler, used by every controller.
Simultaneous borrowing requests are
not serialized by a locking policy in this simulated sprint; see the code review.

Lab 3 adds Reservation, ReservationStatus, its repository/service, and a separate
controller and, at that tag, a scoped exception handler. Reservation status is stored as a string; its relationships
to Book and Member are unidirectional.

The current request flow is **HTTP -> controller + mapper -> service ->
repository -> H2**. All four controllers now have explicit operation, tag and
response-code documentation. [The exported OpenAPI artifact](docs/lab4-openapi.json)
was generated from the running packaged JAR; its server URL is normalized to the
default port 8080 for portability.

`application.properties` configures H2 and schema creation. The
`spring.jpa.defer-datasource-initialization=true` setting ensures tables exist
before `data.sql` inserts the sample records. Explicit plural table names keep
those SQL statements aligned with JPA mappings. Relationships are unidirectional,
which also avoids recursive book/member/borrowing JSON graphs.

## Verify

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\maven.ps1 test
```

Or, with Java 17+ and Maven on PATH: `mvn test`. Integration tests exercise real
controllers, repositories, validation, H2 initialization, entity relationships,
and OpenAPI generation. Database mutation tests run in rolled-back transactions.

## Docker (Lab 4)

Start Docker Desktop with its Linux engine, then run from the project directory:

```powershell
docker build -t library-api:lab4 .
docker run --rm --name library-api-lab4 -p 8084:8080 library-api:lab4
```

In another terminal:

```powershell
Invoke-RestMethod http://localhost:8084/books
Invoke-RestMethod http://localhost:8084/api-docs
```

Swagger UI is at http://localhost:8084/swagger-ui.html. Host port 8084 avoids the
existing local services; the container listens on 8080. Stop the foreground
container with `Ctrl+C`. The two-stage Dockerfile builds with Maven/Java 17 and
copies only the executable JAR into a non-root Java 17 JRE image. JVM heap limits
are `-Xmx256m -Xms128m`. The H2 data remains temporary.

See [Lab 4 verification](reports/lab4-verification.md) for what was actually run
and any Docker-engine limitation; a working JAR is not proof of a working image.

## Lab artifacts

- [Lab 1 report](reports/lab1-report.md) — evidence and reflection draft to review.
- [Lab 1 verification](reports/lab1-verification.md) — actual build and HTTP checks.
- [Lab 2 report](reports/lab2-report.md) — TDD evidence, velocity and review.
- [Sprint backlog](docs/sprint-backlog.md) — estimates, actuals and completed stories.
- [XP code review](docs/lab2-code-review.md) — concerns and applied improvements.
- [Lab 3 report](reports/lab3-report.md) — rule audit, requirement classifications and acceptance evidence.
- [Lab 3 specification](docs/AGENTS.md) and [plan confirmation](docs/lab3-plan.md).
- [Lab 4 report](reports/lab4-report.md), [design review](docs/lab4-design-review.md),
  and [verification](reports/lab4-verification.md).
- Git tags: `lab-1` through `lab-4`, each identifying its lab's submission state.

The reports' personal reflections need the student's review before submission.
