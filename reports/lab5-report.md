# Lab 5 — Refactoring

## Regression baseline

The baseline was run on commit `876679d` with Java 17 and Maven 3.9.16. Maven reported **45 tests run, 0 failures, 0 errors, 0 skipped (100% pass)**. The same complete suite was run after each refactoring and remained green at every checkpoint.

The fine calculation had already been extracted into the public `calculateFine(LocalDate, LocalDate)` method during Lab 2 because that lab explicitly required it as a public pure method. Lab 5 therefore preserved that tested API and applied a further Extract Method refactoring to the repository lookups instead of making `calculateFine` private and breaking earlier requirements.

## Smell catalog

| Priority | Smell | Severity | Evidence in the pre-refactoring code | Recommended refactoring |
|---|---|---|---|---|
| 1 | Duplicate Code | High | `BorrowingController.activeBorrowings` and `ReservationController.activeReservations` both used `service...().stream().map(mapper::toDTO).toList()`. The same collection-conversion policy was repeated outside the mapper layer. | Move Method / Extract Method: add `toDTOs` to each mapper and let controllers delegate. |
| 2 | Long Method | Medium | `BorrowingService.borrowBook` performed three responsibilities in sequence: resolve two entities, validate availability, and construct/persist the loan. `returnBook` similarly combined lookup, state validation, mutation, fine calculation, and persistence. | Extract Method for named entity lookup operations; retain transaction orchestration in the public methods. |
| 3 | Magic Numbers | Medium | `BorrowingService` used `14`, `"0.50"`, and a second `"0.00"` directly in loan/fine construction. Although two literals already had names, the duration name `LOAN_DAYS` did not communicate that it was the default and the zero-fine literal remained inline. | Replace Magic Number with `DEFAULT_LOAN_DAYS`, `DAILY_FINE_RATE`, and `ZERO_FINE`. |
| 4 | Feature Envy | Medium | `BorrowingService.returnBook` reaches through `Borrowing` with `borrowing.getBook().setAvailable(true)` and coordinates both a borrowing and a book state transition. A future change to book availability rules would require service knowledge of the entity's internals. | Move Method, such as a domain-level `markReturned`, if the domain grows. Not selected because it would change the entity API and broaden this behavior-preserving lab. |
| 5 | Primitive Obsession | Low | Book/member/borrowing identifiers are passed throughout the service layer as interchangeable `Long` values, for example `borrowBook(Long bookId, Long memberId)`. The compiler cannot prevent argument reversal. | Introduce small typed ID value objects or a parameter object. Not selected because it would change public method signatures and controller binding for limited current benefit. |
| 6 | God Class tendency | Low | `GlobalExceptionHandler` handles validation, malformed input, not-found, conflicts, data-integrity failures, framework errors, and unexpected logging. It is cohesive today, but every new error policy increases its responsibility. | Extract specialized advice classes once error policies diverge. |

The highest-severity issue was duplicated mapping because changes to list conversion could easily become inconsistent across endpoints. The lookup and literal smells were medium: they did not cause incorrect behavior, but they made business operations harder to scan and future policy changes easier to miss. Primitive IDs and the exception handler are low-severity in this small API; extracting them now would add more types and indirection than the present maintenance benefit justifies.

## Refactoring log

### 1. Extract Method — entity lookups in `BorrowingService`

**Before**

```java
Book book = bookRepository.findById(bookId)
        .orElseThrow(() -> new EntityNotFoundException("Book not found"));
Member member = memberRepository.findById(memberId)
        .orElseThrow(() -> new EntityNotFoundException("Member not found"));
```

**After**

```java
Book book = findBook(bookId);
Member member = findMember(memberId);

private Book findBook(Long bookId) {
    return bookRepository.findById(bookId)
            .orElseThrow(() -> new EntityNotFoundException("Book not found"));
}
```

Equivalent `findMember` and `findBorrowing` methods were added. This makes each public operation read as business orchestration and gives lookup/error policy one named location per entity. It reduces the cost of adding logging, authorization, or a different lookup rule later. After this refactoring, all 45 tests passed.

### 2. Replace Magic Number — loan and fine policy constants

**Before**

```java
private static final int LOAN_DAYS = 14;
// ...
today.plusDays(LOAN_DAYS), null, new BigDecimal("0.00")
```

**After**

```java
private static final BigDecimal ZERO_FINE = new BigDecimal("0.00");
private static final int DEFAULT_LOAN_DAYS = 14;
// ...
today.plusDays(DEFAULT_LOAN_DAYS), null, ZERO_FINE
```

`DAILY_FINE_RATE` remains the single named source for `0.50`. The new names communicate policy rather than representation and eliminate the remaining inline monetary literal. Future changes to the default duration or initial fine now require one edit. After this refactoring, all 45 tests passed, including due-date and fine boundary tests.

### 3. Remove Duplicate Code / Move Method — list conversion into mappers

**Before**

```java
return borrowingService.getActiveBorrowings(id).stream()
        .map(borrowingMapper::toDTO).toList();
```

**After**

```java
return borrowingMapper.toDTOs(borrowingService.getActiveBorrowings(id));

public List<BorrowingDTO> toDTOs(List<Borrowing> borrowings) {
    return borrowings.stream().map(this::toDTO).toList();
}
```

The equivalent reservation conversion moved to `ReservationMapper`. Controllers now express HTTP orchestration while mappers own both single-object and collection conversion. This prevents collection mapping behavior from diverging when DTO conversion changes. After this refactoring, all 45 tests passed, including controller and OpenAPI contract tests.

## AI-generated debt

The generated code was functional and had a strong green baseline, so it did not contain more *critical* debt than careful human code. It did contain the kind of small duplication and mixed responsibilities common in fast feature delivery: controllers repeated list mapping, `BorrowingService` mixed repository mechanics with business flow, and monetary/default values were not all expressed consistently. The evidence suggests moderate preventive-maintenance debt rather than poor correctness: 45 tests already passed, but the structure made future changes more likely to be repeated or placed in the wrong layer.

## Regression safety

After every refactoring I ran the complete `mvn test` suite, not only the closest unit test. The Extract Method checkpoint would have caught changed lookup precedence/messages or missed entities through service and controller tests. The constants checkpoint exercised the 14-day due date and the fine cases `0.00`, `0.50`, `1.50`, and `3.50`, so a changed literal or scale would fail. The mapper checkpoint exercised JSON/DTO controller contracts and OpenAPI generation, so missing fields, ordering changes, or a conversion error would be detected. Each checkpoint returned the original result: **45 run, 45 passed**.
