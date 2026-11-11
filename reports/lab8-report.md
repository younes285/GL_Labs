# Lab 8 — Estimation and Cost

## Function-point count

I counted each endpoint once by its primary function. Read-only filters are external inquiries rather than also being counted as external outputs. H2 and the generated OpenAPI contract are counted as the two external interfaces suggested by the assignment.

### Element inventory and classification

| Type | Element | Complexity | Justification |
|---|---|---|---|
| EI | `POST /books` | Average | Six client fields, validation, defaults, mapping and persistence. |
| EI | `PUT /books/{id}` | Average | Path lookup plus six-field replacement and validation. |
| EI | `DELETE /books/{id}` | Simple | One identifier, existence check and deletion. |
| EI | `POST /members` | Average | Four client fields, email validation, defaults and persistence. |
| EI | `POST /borrowings` | Average | Two IDs plus availability, lookup precedence, date and state rules. |
| EI | `PUT /borrowings/{id}/return` | Average | Lookup, repeated-return guard, date/fine calculation and book state change. |
| EI | `POST /reservations` | Complex | Two IDs with ordered existence, availability, activity and duplicate rules. |
| EI | `DELETE /reservations/{id}` | Simple | One ID and an idempotent status transition. |
| EO | `GET /books` | Average | Returns a multi-record, seven-field catalogue collection. |
| EO | `GET /books/{id}` | Simple | One identifier and one book representation. |
| EO | `GET /members` | Average | Returns a multi-record, five-field member collection. |
| EO | `GET /members/{id}` | Simple | One identifier and one member representation. |
| EQ | `GET /members/{id}/borrowings` | Average | Validates a member and filters/sorts active loans. |
| EQ | `GET /members/{id}/reservations` | Average | Validates a member and filters/sorts ACTIVE reservations. |
| ILF | Book | Average | Seven fields and full catalogue lifecycle. |
| ILF | Member | Average | Five fields, validation and relationships from two transaction types. |
| ILF | Borrowing | Complex | Seven fields, two relationships, lifecycle state and derived fine. |
| ILF | Reservation | Average | Five fields, two relationships and status lifecycle. |
| EIF | H2 database | Simple | One local persistence interface managed through JPA. |
| EIF | OpenAPI contract | Simple | One generated external API description consumed by clients/tools. |

### Weighted UFP table

| Element type | Simple | Average | Complex | Weighted FP |
|---|---:|---:|---:|---:|
| External inputs (×3 / ×4 / ×6) | 2 × 3 = 6 | 5 × 4 = 20 | 1 × 6 = 6 | **32** |
| External outputs (×4 / ×5 / ×7) | 2 × 4 = 8 | 2 × 5 = 10 | 0 | **18** |
| External inquiries (×3 / ×4 / ×6) | 0 | 2 × 4 = 8 | 0 | **8** |
| Internal logical files (×7 / ×10 / ×15) | 0 | 3 × 10 = 30 | 1 × 15 = 15 | **45** |
| External interface files (×5 / ×7 / ×10) | 2 × 5 = 10 | 0 | 0 | **10** |
| **Total UFP** |  |  |  | **113** |

At the lecture midpoint productivity of 9 FP/person-day:

```text
Estimated days  = 113 / 9 = 12.56 person-days
Estimated hours = 12.56 × 7.5 = 94.17 person-hours
```

## Estimated versus actual effort

The estimate column uses each assignment section's stated expected duration. Exact personal time was not tracked for every lab, so the actual column is an honest reconstruction: Lab 2 uses its recorded 0.2106 hours; Labs 3–7 use Git/session intervals with inactive gaps excluded; Lab 1 is recalled setup time; Lab 8 is the current assisted technical session. These figures exclude later study and independent report revision, so they measure assisted execution rather than total learning effort.

| Lab | Estimated hours | Actual hours | Actual / estimate |
|---|---:|---:|---:|
| Lab 1 | 2.00 | 1.0000 | 0.5000 |
| Lab 2 | 2.50 | 0.2106 | 0.0842 |
| Lab 3 | 2.50 | 0.1760 | 0.0704 |
| Lab 4 | 3.00 | 0.4560 | 0.1520 |
| Lab 5 | 2.50 | 0.2620 | 0.1048 |
| Lab 6 | 2.50 | 0.1670 | 0.0668 |
| Lab 7 | 3.00 | 0.1260 | 0.0420 |
| Lab 8 | 2.50 | 0.1500 | 0.0600 |
| **Total** | **20.50** | **2.5476** | **0.1243** |

The observed assisted velocity multiplier is `20.50 / 2.5476 = 8.05×` relative to the assignment estimates. This is not a controlled human-versus-AI experiment: environment familiarity, cached dependencies, short feedback loops, and excluded study/review time all affect the ratio.

### Estimation Quality Factor

DeMarco's EQF is the area under the actual curve divided by the area between the estimate and actual curves. A true EQF needs estimates revised over time. Because this project retained only one estimate per lab, I use the one-estimate aggregate approximation:

```text
EQF = actual effort / |estimated effort − actual effort|
    = 2.5476 / |20.50 − 2.5476|
    = 2.5476 / 17.9524
    = 0.1419
```

The low value correctly reflects systematic overestimation relative to the narrowly measured assisted execution time. It should not be presented as an EQF for total student effort. The definition follows DeMarco's original estimation-quality concept; the one-estimate approximation is explicitly a limitation of the available data.

## Fine Payment module requirements and COCOMO estimate

### Requirements outline

The extension would introduce a `FinePayment` record linked to a member and the borrowing that produced the fine, with amount, payment date/time and payment status/reference. Its use cases would be:

1. `GET /members/{id}/fines` returns unpaid positive fines for an existing member, with borrowing and amount information; missing members return 404.
2. `POST /fines/{id}/pay` marks one unpaid fine paid, records the server time, and prevents double payment through an explicit idempotency/conflict rule.
3. `GET /reports/fines?month=YYYY-MM` validates the month and returns totals collected, outstanding amounts and payment counts for that calendar month.

The implementation needs an entity/DTO, repository, service rules, controller mappings, error handling, tests and OpenAPI updates. Authorization, payment-provider integration, partial payments, currency conversion and refunds remain out of scope unless separately specified.

### Three-point size estimate

| Estimate | LOC | Assumption |
|---|---:|---|
| Minimum | 220 | Reuses current patterns; simple paid/unpaid state and focused tests. |
| Best fit | 320 | Complete validation, reporting query, DTOs and unit/integration tests. |
| Maximum | 450 | Extra edge handling, richer report query and persistence/test complexity. |

```text
Expected size = (min + 4 × best + max) / 6
              = (220 + 4 × 320 + 450) / 6
              = 325 LOC = 0.325 KLOC
```

### COCOMO 81 organic mode

```text
Effort HM = 2.4 × 0.325^1.05 = 0.737 person-months
TDEV      = 2.5 × 0.737^0.38 = 2.227 months
Team size = 0.737 / 2.227 = 0.331 people
```

The fractional team means this small extension is best treated as part-time work for one developer; COCOMO is being extrapolated below the project sizes on which its constants were calibrated.

## LOC and FP productivity

The current `src/main/java` tree contains **29 Java files and 914 physical lines** (PowerShell-equivalent counts of the assignment's `find`/`wc` commands).

```text
LOC/hour = 914 / 2.5476 = 358.77
FP/hour  = 113 / 2.5476 = 44.36
```

The LOC/hour number looks spectacular but is misleading: generated accessors, annotations, DTO declarations and test-supporting structure increase line count without proportionally increasing delivered capability. FP/hour is fairer because it describes user-visible inputs, outputs, inquiries and maintained data. It is still sensitive to subjective complexity classification, but it better reflects the library functions delivered.

## Proposed AI-assisted COCOMO alpha

Assuming 150 working hours per person-month, observed effort is:

```text
Actual HM = 2.5476 / 150 = 0.01698
Project size = 0.914 KLOC
alpha = Actual HM / KLOC^1.05
      = 0.01698 / 0.914^1.05
      = 0.0187
```

For this narrowly measured, AI-assisted project, replacing 2.4 with **α = 0.0187** reproduces the observed technical execution. Classical COCOMO would predict `2.4 × 0.914^1.05 = 2.184 person-months`, roughly 129 times the recorded assisted execution. The calibrated value must not be generalized: it is based on one tiny project, excludes learning and human review, and includes unusually strong reuse of generated conventions and automated tooling. A defensible organizational coefficient requires multiple projects with complete time records.

## Final reflection

Across the eight labs, the lifecycle remained recognizable: requirements were made explicit, architecture and DTO boundaries were designed, features were implemented, refactoring improved internal structure, tests protected behavior, metrics quantified the result, and estimation translated scope into expected effort. AI changed the speed and mechanics of each stage—it generated repetitive code, searched the repository, ran feedback loops, and assembled evidence quickly. It did not remove the need to choose scope, resolve ambiguous policies, judge whether a metric is meaningful, review test oracles, or accept responsibility for the submitted result. The most useful pattern was therefore not “prompt then trust,” but specify → generate → inspect → measure → correct.
