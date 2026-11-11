# Library API — Software Engineering Labs 2026–2027

Lab 1: a Java 17 / Spring Boot 3 Maven REST API for a university library.
Later labs will extend the same project, one at a time.

## Run on this Windows machine

From the `library-api` directory:

```powershell
$env:SERVER_PORT = '8081'
powershell -ExecutionPolicy Bypass -File .\scripts\maven.ps1 spring-boot:run
```

Apache already occupies port 8080 on this machine, so the commands here select
8081. The application's default port remains 8080 when `SERVER_PORT` is unset.

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
| http://localhost:8081/books | Five sample books as JSON |
| http://localhost:8081/members | Three sample members as JSON |
| http://localhost:8081/swagger-ui.html | Interactive API documentation |
| http://localhost:8081/v3/api-docs | OpenAPI JSON |
| http://localhost:8081/h2-console | Development database console |
| http://localhost:8081/actuator/health | Application health |

H2 login: driver `org.h2.Driver`, JDBC URL
`jdbc:h2:mem:librarydb`, username `sa`, empty password.
The H2 console and unauthenticated endpoints are for this local development lab.

```powershell
Invoke-RestMethod http://localhost:8081/books
Invoke-RestMethod http://localhost:8081/members

$book = @{
    isbn = '9780000000001'
    title = 'A New Book'
    author = 'Example Author'
    genre = 'Reference'
    publishedYear = 2026
    available = $true
} | ConvertTo-Json
Invoke-RestMethod http://localhost:8081/books -Method Post -ContentType 'application/json' -Body $book
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

Missing records return 404; invalid request fields return 400. IDs are generated
by the database, and create operations ignore IDs supplied by clients.

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
- **Controllers** (`controller/`): map HTTP paths and methods to repository calls,
  validate incoming JSON, select HTTP status codes, and return objects that Spring
  serializes as JSON. Constructor injection makes their dependencies explicit.

The request flow is **HTTP → controller → repository → H2**, with entities carrying
the data. Lab 1 uses direct repository calls for its small CRUD scope; Lab 2 will
introduce a service for borrowing rules and transactions. No borrowing endpoint or
borrowing business rule is implemented in Lab 1.

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
and OpenAPI generation. Each test runs in a rolled-back transaction.

## Lab artifacts

- [Lab 1 report](reports/lab1-report.md) — evidence and reflection draft to review.
- [Lab 1 verification](reports/lab1-verification.md) — actual build and HTTP checks.
- Git tag: `lab-1` on `main`.

The report's personal reflections need the student's review before submission.
