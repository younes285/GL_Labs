# Lab 1 — Introduction to AI-Assisted Engineering

Date: 11 November 2026

Project: Library Management API

Scope: Lab 1 only

> AI-assisted reflection draft. Review the code, run the examples in the README,
> and revise the judgments below so the submitted report expresses your own
> understanding. The lecture slides were not supplied; the role discussion is
> a paraphrase, not a quotation of a specific lecture-table row.

## 1. Observation: three decisions

Codex organized the code under `com.library.api` with `entity`, `repository`, and
`controller` packages; this is a sensible choice because each responsibility is
easy to locate and Spring Boot scans all three beneath the application class.
It used `LocalDate` for borrowing and membership dates, and `BigDecimal` with
database scale 2 for fines; these choices suit date-only records and decimal
money, so they are worth retaining.
It mapped the entities to explicit plural table names (`books`, `members`, and
`borrowings`) using `@Table`; this keeps the Java mappings consistent with
`data.sql` and is another defensible decision.
These are proposed agreement judgments to confirm after inspecting the actual
classes, rather than assuming generated choices are automatically correct.

## 2. Engineering judgment

Accepting the project without checking the machine's tools would have left it
unable to run: the terminal originally selected Java 8 and had no Maven command.
Another detail that required verification was SQL initialization order: Hibernate
must create the tables before `data.sql` inserts the sample books and members,
which is why `spring.jpa.defer-datasource-initialization=true` is configured.
The integration checks also verify that missing IDs return 404, invalid requests
return 400, and a client-supplied ID cannot overwrite a record during POST.
A later startup attempt exposed stale Maven incremental metadata with missing
compiled classes, requiring a clean rebuild before the live run.

## 3. Chapter connection: small and large programming

Codex handles many programming-in-the-small tasks here, such as writing entity
accessors, repository declarations, and HTTP handler methods.
The project also involves programming-in-the-large decisions: choosing package
boundaries, checking the runtime environment, defining observable API behavior,
and recording a reproducible Git milestone.
AI makes it faster to produce implementation code but does not decide whether
the implementation satisfies the course requirements or the library's needs.
The distinction therefore remains useful: a human still owns the specification,
review, and lifecycle decisions even when a tool writes much of the code.

## 4. Role reflection: junior versus senior in the AI era

The closest role description is the engineer who specifies the desired result,
reviews generated code, and verifies the outcome instead of only typing it.
In this session the student set the scope to one lab at a time, while Codex
generated the scaffold and performed the technical checks.
To make the reflection personal, the student should inspect the initialization
setting and one controller, then explain why those choices are acceptable.
The exact matching row from the lecture's “Junior vs Senior in the AI era” table
still needs to be checked against the course slides, which were not attached.

## Evidence and workflow

See [lab1-verification.md](lab1-verification.md) for the build and live endpoint
results, and [README.md](../README.md) for the architecture explanation required
by Step D and the commands to repeat the exercise.

The observed workflow was document and environment inspection, a scope update,
file creation, execution, and verification. Routine file creation used the
session's existing authorization; no claim is made that the student approved
each file individually. Network-dependent setup required execution outside the
restricted sandbox. Lab 2 has not been started.
