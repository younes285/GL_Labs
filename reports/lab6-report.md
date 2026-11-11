# Lab 6 — Measures and Metrics

## Analysis configuration and commands

The Maven build now configures PMD 3.21.0 (PMD 6.55.0 engine), SpotBugs Maven Plugin 4.8.3.1, and JaCoCo 0.8.11. I ran:

```text
mvn pmd:pmd
mvn spotbugs:spotbugs
mvn test jacoco:report
mvn pmd:check
```

The generated local artifacts are `target/pmd.xml`, `target/spotbugsXml.xml`, and `target/site/jacoco/index.html`. The repository intentionally ignores `target/` because these are reproducible build outputs.

## Coverage report

JaCoCo measured **259 covered and 18 missed lines: 93.5% overall line coverage**. The service package has **82/82 lines (100%)** and **14/14 branches (100%)** covered.

| Class | Lines covered / total | Line coverage | Branches covered / total |
|---|---:|---:|---:|
| BookService | 17 / 17 | 100% | 0 / 0 |
| BorrowingService | 34 / 34 | 100% | 6 / 6 |
| MemberService | 7 / 7 | 100% | 0 / 0 |
| ReservationService | 24 / 24 | 100% | 8 / 8 |
| BookController | 11 / 11 | 100% | 0 / 0 |
| BorrowingController | 7 / 7 | 100% | 0 / 0 |
| MemberController | 8 / 8 | 100% | 0 / 0 |
| ReservationController | 7 / 7 | 100% | 0 / 0 |
| GlobalExceptionHandler | 13 / 17 | 76.5% | 1 / 2 |
| LibraryApiApplication | 1 / 3 | 33.3% | 0 / 0 |

`LibraryApiApplication` has the lowest reported line coverage at 33.3%. Its uncovered lines are the generated constructor and the `main` bootstrap call; normal Spring integration tests load the application configuration without invoking `main`, so this is expected and does not represent untested business behavior. Among business classes, `GlobalExceptionHandler` is lowest at 76.5%, because the generic unexpected-error branch is not exercised.

## Cyclomatic complexity

The manual count uses `CC = decision points + 1`. A direct `if` is a decision; the library call `Math.max` is not a source-code decision point in `calculateFine`.

| Method | Decision points | CC | Risk level |
|---|---:|---:|---|
| `borrowBook` | 1 (`if (!book.isAvailable())`) | 2 | Simple |
| `returnBook` | 1 (`if (returnDate != null)`) | 2 | Simple |
| `calculateFine` | 0 | 1 | Simple |

No measured method has CC greater than 5. The current methods do not need complexity-driven refactoring; extracting more methods would add indirection without reducing decision count.

## Coupling analysis

For a repeatable source-level measure, Ce counts every explicitly imported class in each service (project, framework, and JDK). Ca counts production classes that explicitly import that service. The instability formula is `I = Ce / (Ce + Ca)`.

| Class | Ce | Ca | Instability |
|---|---:|---:|---:|
| BookService | 6 | 1 | 0.857 |
| BorrowingService | 13 | 1 | 0.929 |
| MemberService | 6 | 1 | 0.857 |
| ReservationService | 12 | 1 | 0.923 |

`BorrowingService` is the most unstable at 0.929. This is appropriate for an application service: it orchestrates three domain/repository types plus date and money types, while only its controller depends directly on it. The number is also sensitive to the chosen import-based definition—Spring annotations and JDK value types increase Ce even though they are stable dependencies—so it should be treated as a trend indicator rather than a quality verdict.

## PMD and SpotBugs results

The initial PMD run reported five priority-3 findings:

- `UseUtilityClass` on `LibraryApiApplication`.
- `UncommentedEmptyConstructor` on `Book`, `Borrowing`, `Member`, and `Reservation`.

SpotBugs reported 15 rank-18/priority-2 `EI_EXPOSE_REP` or `EI_EXPOSE_REP2` warnings. Fourteen concern intentional JPA associations between `Borrowing`/`Reservation` and mutable managed entities; one concerns constructor injection in `BookController`. These are low-confidence architectural warnings in this Spring/JPA design, not evidence that callers receive unsafe collection or array internals.

### Quality improvement

I selected the four `UncommentedEmptyConstructor` findings as the important actionable violation. Empty no-argument constructors can look accidental, but JPA requires them for reflective entity construction. Each constructor now contains:

```java
// Required by JPA for reflective entity construction.
```

After the change, PMD fell from five findings to one; all four constructor findings are gone. The remaining `UseUtilityClass` item is a false positive for the Spring Boot configuration/entry class, which is not a utility class. Making it abstract or private solely to satisfy the rule would obscure or risk framework behavior.

## AI and metrics

The repository history describes the production code as AI-assisted, so there is no honest, clean partition of production classes into “AI-written” and “hand-written” groups. JaCoCo also measures executed bytecode, not authorship. The actual measurements show that test focus explains the variation better than origin: the AI-assisted service package is at 100% line coverage (82/82), while the generated bootstrap class is at 33.3% (1/3). Likewise, `BorrowingService` has low method complexity (CC 1–2) despite the highest coupling value (0.929), because orchestration requires dependencies without requiring complex control flow. These values do not support a claim that AI code has a single distinct metric pattern; attribution data would be required for that comparison.

## GQM plan

The completed plan is stored in `docs/gqm.md`. Its conclusion is that the service layer currently has low decision complexity and complete line/branch coverage, so the next action is to monitor these baselines rather than refactor speculatively.
