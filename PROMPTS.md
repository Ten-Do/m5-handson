# PROMPTS.md — Module 5 hands-on

PR: https://github.com/Ten-Do/m5-handson/pull/1

AI assistant: Claude (Opus 5.5). Coverage figures are for `TaxCalculator`, the
class under test. The JaCoCo report total also counts test classes, because the
Makefile passes `--classfiles build`.

---

# Session 5A — AI-generated tests

## Part A — Baseline coverage

- Line coverage: **48.8%** (20/41)
- Branch coverage: **34.1%** (15/44)
- Methods with < 50% branch coverage: `isEligibleForReturn` (0/10),
  `roundToPaise` (0/2), `computeVAT` (6/14)

Chosen method: `isEligibleForReturn`. It is tied for the lowest coverage at 0%
and has the most branches.

## Part B — Generate tests with constraints

### Prompt

```
Generate JUnit 5 tests for the following method.
Rules:
- Use AAA style with explicit comments.
- One assertion per test.
- Test names of the form
  <method>_<condition>_<expected>.
- Cover: (a) null input, (b) empty input if applicable,
  (c) boundary values, (d) one representative happy path.
- Do NOT use mocks.
- Do NOT include implementation-copying assertions; each
  assertion must state a contract, not restate the code.

public boolean isEligibleForReturn(BigDecimal grossIncome, int ageYears) {
    if (grossIncome == null || ageYears < 0) {
        return false;
    }
    BigDecimal threshold;
    if (ageYears >= 80) {
        threshold = new BigDecimal("500000");
    } else if (ageYears >= 60) {
        threshold = new BigDecimal("300000");
    } else {
        threshold = new BigDecimal("250000");
    }
    return grossIncome.compareTo(threshold) > 0;
}
```

### AI reply

Contract: a return must be filed when gross income is strictly above the
exemption limit for the age band (under 60: 2.5 L; 60–79: 3 L; 80+: 5 L).
Invalid input (null income, negative age) returns `false`. "Empty input" does
not apply to `BigDecimal`/`int`, so zero income is used instead.

```java
@Test
public void isEligibleForReturn_nullIncome_returnsFalse() {
    // Arrange
    TaxCalculator c = new TaxCalculator();
    // Act
    boolean eligible = c.isEligibleForReturn(null, 40);
    // Assert
    assertFalse(eligible);
}

@Test
public void isEligibleForReturn_negativeAge_returnsFalse() {
    // Arrange
    TaxCalculator c = new TaxCalculator();
    // Act
    boolean eligible = c.isEligibleForReturn(new BigDecimal("1000000"), -1);
    // Assert
    assertFalse(eligible);
}

@Test
public void isEligibleForReturn_zeroIncome_returnsFalse() {
    // Arrange
    TaxCalculator c = new TaxCalculator();
    // Act
    boolean eligible = c.isEligibleForReturn(BigDecimal.ZERO, 30);
    // Assert
    assertFalse(eligible);
}

@Test
public void isEligibleForReturn_under60IncomeAtLimit_returnsFalse() {
    // Arrange
    TaxCalculator c = new TaxCalculator();
    // Act
    boolean eligible = c.isEligibleForReturn(new BigDecimal("250000"), 30);
    // Assert
    assertFalse(eligible);
}

@Test
public void isEligibleForReturn_under60IncomeJustAboveLimit_returnsTrue() {
    // Arrange
    TaxCalculator c = new TaxCalculator();
    // Act
    boolean eligible = c.isEligibleForReturn(new BigDecimal("250000.01"), 30);
    // Assert
    assertTrue(eligible);
}

@Test
public void isEligibleForReturn_seniorIncomeAtLimit_returnsFalse() {
    // Arrange
    TaxCalculator c = new TaxCalculator();
    // Act
    boolean eligible = c.isEligibleForReturn(new BigDecimal("300000"), 65);
    // Assert
    assertFalse(eligible);
}

@Test
public void isEligibleForReturn_seniorIncomeJustAboveLimit_returnsTrue() {
    // Arrange
    TaxCalculator c = new TaxCalculator();
    // Act
    boolean eligible = c.isEligibleForReturn(new BigDecimal("300000.01"), 65);
    // Assert
    assertTrue(eligible);
}

@Test
public void isEligibleForReturn_superSeniorIncomeAtLimit_returnsFalse() {
    // Arrange
    TaxCalculator c = new TaxCalculator();
    // Act
    boolean eligible = c.isEligibleForReturn(new BigDecimal("500000"), 85);
    // Assert
    assertFalse(eligible);
}

@Test
public void isEligibleForReturn_superSeniorIncomeJustAboveLimit_returnsTrue() {
    // Arrange
    TaxCalculator c = new TaxCalculator();
    // Act
    boolean eligible = c.isEligibleForReturn(new BigDecimal("500000.01"), 85);
    // Assert
    assertTrue(eligible);
}

@Test
public void isEligibleForReturn_age60IncomeBetweenLimits_returnsFalse() {
    // Arrange
    TaxCalculator c = new TaxCalculator();
    // Act: 2.8 L is above the under-60 limit but within the senior limit
    boolean eligible = c.isEligibleForReturn(new BigDecimal("280000"), 60);
    // Assert
    assertFalse(eligible);
}

@Test
public void isEligibleForReturn_age80IncomeBetweenLimits_returnsFalse() {
    // Arrange
    TaxCalculator c = new TaxCalculator();
    // Act: 4 L is above the senior limit but within the super-senior limit
    boolean eligible = c.isEligibleForReturn(new BigDecimal("400000"), 80);
    // Assert
    assertFalse(eligible);
}

@Test
public void isEligibleForReturn_salariedAdultTwelveLakh_returnsTrue() {
    // Arrange
    TaxCalculator c = new TaxCalculator();
    // Act
    boolean eligible = c.isEligibleForReturn(new BigDecimal("1200000"), 35);
    // Assert
    assertTrue(eligible);
}
```

`make test`: 15 passed, 0 failed.

## Part C — Assertion review

Every assertion is `assertTrue`/`assertFalse` on inputs taken from the
exemption limits. None of them computes the expected value from the input.
Nothing was deleted or rewritten.

## Part D — Mutation testing

Mutation score for `isEligibleForReturn`: **10/11**. For the whole class it is
25/42; the baseline with only the seed tests was 0/11 and 15/42.

Surviving mutant: line 70, `ageYears < 0` → `ageYears <= 0` (changed
conditional boundary).

### Prompt

```
The mutant at line 70 (`ageYears < 0` -> `ageYears <= 0`) survived
the test suite. Write ONE new JUnit test that would kill this
mutant without changing any other test. Explain in one
sentence why this test kills the mutant.
```

### AI reply

```java
@Test
public void isEligibleForReturn_ageZeroIncomeAboveLimit_returnsTrue() {
    // Arrange
    TaxCalculator c = new TaxCalculator();
    // Act
    boolean eligible = c.isEligibleForReturn(new BigDecimal("400000"), 0);
    // Assert
    assertTrue(eligible);
}
```

Age 0 is valid and 4 L is above the under-60 limit, so the original returns
`true`, while the mutant treats age 0 as invalid and returns `false`.

Re-ran PIT: the mutant is **KILLED**.

## Part E — Reflect

| | Baseline | After |
|---|---|---|
| Line coverage | 48.8% (20/41) | 68.3% (28/41) |
| Branch coverage | 34.1% (15/44) | 56.8% (25/44) |

| Mutation score | Before targeted test | After targeted test |
|---|---|---|
| `isEligibleForReturn` | 10/11 (91%) | 11/11 (100%) |
| `TaxCalculator` | 25/42 (60%) | 26/42 (62%) |

Mutation testing was the more useful feedback: coverage already showed 100% of
the method's branches, but PIT still found an untested boundary (age 0).

---

# Session 5B — Auto-documentation

`OrderApi.java` was missing from the starter archive. Before any documentation
work, I added it without JavaDoc in a separate commit. It has four endpoints
over `OrderService`, a `CreateOrderRequest` record, and stub Spring
annotations with a stub `ResponseEntity`.

## Part A — JavaDoc for `POST /orders`

### Prompt (generation)

```
Generate JavaDoc for the following method. Rules:
- One-line summary in imperative mood.
- Describe the contract, not the implementation.
- @param for each parameter with type and constraints.
- @return with what is returned.
- @throws for each declared or unchecked exception the
  caller should be aware of.
- If any behaviour is not evident from the code, write
  "TODO" and skip.

@PostMapping
public ResponseEntity<OrderDto> create(@RequestBody CreateOrderRequest req) {
    if (req == null || req.customerId() == null || req.customerId().isBlank()
            || req.amount() == null || req.amount().signum() <= 0) {
        return ResponseEntity.badRequest();
    }
    Order o = service.create(req.customerId(), req.amount());
    return ResponseEntity.created(URI.create("/orders/" + o.getId()), OrderDto.from(o));
}
```

### AI reply

```java
/**
 * Create a new order for a customer ({@code POST /orders}).
 *
 * <p>The new order gets a server-generated UUID id and the initial status
 * {@code "NEW"}.
 *
 * @param req the order to create, deserialized from the JSON request body;
 *            must be non-null, with a non-blank {@code customerId} and a
 *            strictly positive {@code amount}
 * @return {@code 201 Created} whose body is the created order and whose
 *         {@code Location} is {@code /orders/{id}}; or {@code 400 Bad Request}
 *         with an empty body if {@code req} violates any constraint above
 */
```

No `@throws`: the method throws nothing, and invalid input returns a 400
response.

### Prompt (verification)

```
Given the JavaDoc above and the method body below,
identify any statement in the JavaDoc that is inconsistent
with the code. Do not fix; just list.
```

**Pass 1 reply:**
1. "JSON request body": nothing in the code sets the media type.
2. "UUID id and initial status NEW": the method body does not show this; it
   comes from `OrderService.create` and the `Order` constructor.

**Fixes:**
1. Changed the wording to "taken from the request body".
2. Checked the source: the claim is true. I kept it as a "unique id", with a
   reference to `{@link OrderService#create}`.

**Pass 2 reply:** "No inconsistencies."

The final JavaDoc is in `src/OrderApi.java`.

## Part B — README

### Prompt

```
Draft a README.md for this repository with sections:
description, build, quick example, contributing, license.
Use MIT license placeholder.
Rules:
- Do NOT invent features not present in the code.
- If a section has no evidence in the code, write "TODO" and
  skip.
- The one-line description must be a factual summary of what
  the code does, not marketing copy.

<file tree + Makefile, README.md, TaxCalculator.java, OrderApi.java>
```

The raw reply is in `README.raw.md`. The edited version is in `README.md`.

Invented features deleted (confirmed by grep: no `main`, no server, no
`LICENSE`):
1. "Start the application" and a `curl http://localhost:8080/orders` example
   with a JSON body. There is no entry point and no HTTP server.
2. "A Spring `@RestController` exposing `/orders` endpoints". The annotations
   are stubs, Spring is not on the classpath, and nothing is exposed.
3. "See `LICENSE` for details". The file does not exist.

## Part C — OpenAPI spec

### Prompt

```
Read the following Spring @RestController and generate an
OpenAPI 3.0 YAML spec covering:
- Every endpoint (path, method, summary from JavaDoc).
- Request body schemas for POST/PUT.
- Response schemas for 2xx and 4xx.
- Referenced DTO schemas in the components section.
If any endpoint's behaviour is unclear, add a TODO comment
in the spec at that location.

<OrderApi.java, OrderDto.java, CreateOrderRequest.java>
```

The result is saved as `openapi.yaml`.

Checks:
- All 4 endpoints are present with the correct path and method.
- `OrderDto` and `CreateOrderRequest` are defined in `components`.
- `GET /orders` had no 4xx response, so I added `406 Not Acceptable` by hand.

## Part D — Reflect

- **JavaDoc inconsistencies caught on the first pass:** 2.
- **Invented features in the README:** 3.
- **Most hand editing: the README.** JavaDoc and OpenAPI are generated from a
  single file and can be checked directly against it. The README has to
  describe the whole repository and how to run it. The model filled the gaps
  with typical Spring-project assumptions: a server, curl, a LICENSE file. I
  had to rewrite the Quick example and Contributing sections by hand.
