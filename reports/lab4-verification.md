# Lab 4 verification

Date: 11 November 2026

## Application and contract checks

The final application suite passes 45 tests with no failures, errors or skips.
All business controllers delegate to services and expose DTOs/request records.
The tests retain the previous borrowing/reservation business assertions while
adopting Lab 4's flattened IDs and common error response.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\maven.ps1 test
powershell -ExecutionPolicy Bypass -File .\scripts\maven.ps1 package -DskipTests
```

The executable JAR was built successfully at
`target/library-api-0.0.1-SNAPSHOT.jar`. A Java 17 run with `-Xmx256m -Xms128m`
started successfully on port 8085. Live verification observed:

| Request / scenario | Result |
| --- | --- |
| GET /books | 200, 5 sample books |
| GET /members | 200, 3 sample members |
| GET /swagger-ui.html | 200 after redirect, Swagger UI HTML |
| GET /api-docs | 200, all 14 business operations |
| POST /borrowings with missing book | 404, error + ISO timestamp |
| POST /borrowings with missing input fields | 400, fieldErrors included |
| POST /borrowings, available book | 201, flat bookId/memberId, no nested entities |
| POST /borrowings, same book again | 409 |
| POST /reservations, unavailable book | 201, flat bookId/memberId |
| DELETE /reservations/{id} | 200, cancellation succeeds |
| PUT /borrowings/{id}/return | 200, return succeeds |

The OpenAPI test checks tags/summaries, success/error documentation, 500
responses, DTO schema field examples and absence of JPA entity schemas.

## Container artifact and blocker

The Dockerfile has the requested Maven Java 17 build stage and Alpine Java 17
JRE runtime stage, copies only the JAR, exposes 8080, applies the required heap
settings and runs as a non-root user. `.dockerignore` excludes Git, local tools,
caches, development metadata, reports, secrets files and build output.

Docker is installed, but its engine did not respond to bounded version checks
through either the Windows default context or desktop-linux. Docker Desktop
processes and its WSL distribution were running, while com.docker.service was
stopped; an attempt to start that service was denied by Windows service control.
The engine preflight timed out again before an image build could begin.

**Docker build and container startup are not verified.** The application/JAR
checks above do not substitute for these checks. No image ID is recorded.

After restarting Docker Desktop, verify from the project directory:

```powershell
docker --context desktop-linux version
docker build -t library-api:lab4 .
docker run --rm --name library-api-lab4 -p 8084:8080 library-api:lab4
```

In another terminal:

```powershell
Invoke-RestMethod http://localhost:8084/books
Invoke-RestMethod http://localhost:8084/actuator/health
Invoke-RestMethod http://localhost:8084/api-docs
```

Expected: 5 books, health UP, and 14 documented business operations. Port 8084
avoids other local services. Record the successful build/image/container evidence
here when it becomes available.
