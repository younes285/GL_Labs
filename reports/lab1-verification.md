# Lab 1 verification

Verified on 11 November 2026. Only Lab 1 is implemented.

## Environment

- Java: JetBrains OpenJDK 17.0.5, bundled with PyCharm Community Edition 2022.3.1.
- Maven: 3.9.16, downloaded from Maven Central and SHA-512 verified.
- Spring Boot: 3.5.16; SpringDoc: 2.8.17.
- `scripts/maven.ps1` selects the configured Java installation and project-local Maven.
- Local tools and dependency caches are excluded from Git.

## Automated verification

The integration suite passed initially and again during a clean build:

```text
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
Time elapsed: 25.46 s
```

The tests cover seeded collections, book CRUD, member creation and retrieval,
missing-record responses, validation without accidental inserts, persistence of
borrowing relationships and decimal fines, and OpenAPI generation.

Commands used through the project helper:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\maven.ps1 -B test
powershell -ExecutionPolicy Bypass -File .\scripts\maven.ps1 -B -ntp clean test spring-boot:run
$env:SERVER_PORT = '8081'
powershell -ExecutionPolicy Bypass -File .\scripts\maven.ps1 -B -ntp spring-boot:run
```

The clean build corrected missing compiled classes with stale incremental-build
metadata. Its tests passed, but the following run encountered an occupied port:
Apache (`httpd.exe`) already used 8080. The successful live run selected 8081;
the application's default remains 8080.

## Live HTTP verification

All requests below were made against the running application at
`http://localhost:8081`. Redirects were followed for browser pages.

| Request | Final status | Observed result |
| --- | --- | --- |
| GET `/books` | 200 | JSON array containing 5 books |
| GET `/members` | 200 | JSON array containing 3 members |
| GET `/books/1` | 200 | One book as JSON |
| GET `/members/1` | 200 | One member as JSON |
| GET `/swagger-ui.html` | 200 | Swagger UI HTML at `/swagger-ui/index.html` |
| GET `/h2-console` | 200 | H2 console HTML at `/h2-console/` |
| GET `/v3/api-docs` | 200 | OpenAPI JSON documenting both resources |
| GET `/actuator/health` | 200 | Health status `UP` |

First book returned:

```json
{"id":1,"isbn":"9780132350884","title":"Clean Code","author":"Robert C. Martin","genre":"Software Engineering","publishedYear":2008,"available":true}
```

First member returned:

```json
{"id":1,"name":"Maya Haddad","email":"maya.haddad@usj.edu.lb","membershipDate":"2026-09-01","active":true}
```

## Submission notes

The report contains an AI-assisted reflection draft requiring the student's
review, including comparison with the exact role-table row in the lecture slides.
The supplied remote is `https://github.com/younes285/GL_Labs.git`; it was public
when inspected, whereas the assignment requests a private repository.
