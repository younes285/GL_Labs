# GQM Measurement Plan

## Goal

Analyse the service layer in order to improve maintainability with respect to decision complexity and regression protection from the point of view of the developer.

## Questions

Q1: Are any public service methods complex enough to make changes difficult to reason about?

Q2: Does the current test suite execute enough of the service layer to protect a maintainability refactoring?

## Metrics

M1 (answers Q1): Cyclomatic complexity per public service method, measured manually as `decision points + 1`, where decisions include `if`, loops, `case`, `catch`, boolean `&&`/`||`, and ternary expressions.

M2 (answers Q2): Service-package line and branch coverage, measured by JaCoCo 0.8.11 after `mvn test jacoco:report`.

## Results

M1 result: The measured `BorrowingService` methods are `borrowBook` CC 2, `returnBook` CC 2, and `calculateFine` CC 1. No measured method exceeds CC 5; all are in the Simple risk band.

M2 result: All four service classes have 100% line coverage (82/82 lines). `BorrowingService` covers 6/6 branches and `ReservationService` covers 8/8 branches, so service-package branch coverage is also 100% (14/14).

## Interpretation

The current service layer is easy to reason about at method level and has strong regression protection. No complexity-driven extraction is justified now. The recommended action is to preserve these baselines as features are added, investigate any future method that rises above CC 5, and add branch-focused tests before accepting new conditional business rules.
