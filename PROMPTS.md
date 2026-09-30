# PROMPTS.md — Module 5 hands-on

Toolchain: OpenJDK 21 (`javac --release 17`), JUnit 5.10.0, JaCoCo 0.8.11, PIT 1.17.4.
AI assistant used for every "paste into chat" step: Claude (Opus 5.5).

Note on the JaCoCo report: the Makefile passes `--classfiles build`, so the
report's grand total also counts the test class and the (untested) `Order*`
classes. The figures below are for the class under test, **`TaxCalculator`**
(the same scope PIT uses); report-wide totals are given in brackets.

---

# Session 5A — AI-generated tests

## Part A — Baseline coverage (3 seed tests)

`make coverage` → `coverage/index.html`

| Scope | Line coverage | Branch coverage |
|---|---|---|
| `TaxCalculator` | 20/41 = **48.8%** | 15/44 = **34.1%** |
| (report total) | (33/83 = 39.8%) | (15/48 = 31.3%) |

Per-method branch coverage (`TaxCalculator`):

| Method | Branches covered | % |
|---|---|---|
| `computeIncomeTax` | 5/10 | 50% |
| `computeVAT` | 6/14 | 42.9% |
| `applyExemption` | 4/8 | 50% |
| `roundToPaise` | 0/2 | 0% |
| `isEligibleForReturn` | 0/10 | 0% |

**Methods with < 50% branch coverage:** `isEligibleForReturn` (0%),
`roundToPaise` (0%), `computeVAT` (42.9%). (Outside `TaxCalculator`,
`OrderService.cancel` is also at 0%.)

Chosen method: **`isEligibleForReturn`**: it has the lowest branch coverage
(tied at 0% with `roundToPaise`, but it has 10 branches instead of 2 and is never called at all).

## Part B — Generate tests with constraints

### Prompt

````
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
````

### AI reply (verbatim)

> The contract I'm testing against: a return must be filed when gross income is
> **strictly above** the basic exemption limit for the taxpayer's age band
> (below 60: ₹2.5 L; 60–79: ₹3 L; 80+: ₹5 L). Invalid input (null income,
> negative age) yields `false` rather than an exception.
> "Empty input" doesn't apply to a `BigDecimal`/`int` pair, so I used zero income as the closest equivalent.
>
> ```java
> @Test
> public void isEligibleForReturn_nullIncome_returnsFalse() {
>     // Arrange
>     TaxCalculator c = new TaxCalculator();
>     // Act
>     boolean eligible = c.isEligibleForReturn(null, 40);
>     // Assert
>     assertFalse(eligible);
> }
>
> @Test
> public void isEligibleForReturn_negativeAge_returnsFalse() {
>     // Arrange
>     TaxCalculator c = new TaxCalculator();
>     // Act
>     boolean eligible = c.isEligibleForReturn(new BigDecimal("1000000"), -1);
>     // Assert
>     assertFalse(eligible);
> }
>
> @Test
> public void isEligibleForReturn_zeroIncome_returnsFalse() {
>     // Arrange
>     TaxCalculator c = new TaxCalculator();
>     // Act
>     boolean eligible = c.isEligibleForReturn(BigDecimal.ZERO, 30);
>     // Assert
>     assertFalse(eligible);
> }
>
> @Test
> public void isEligibleForReturn_under60IncomeAtLimit_returnsFalse() {
>     // Arrange
>     TaxCalculator c = new TaxCalculator();
>     // Act
>     boolean eligible = c.isEligibleForReturn(new BigDecimal("250000"), 30);
>     // Assert
>     assertFalse(eligible);
> }
>
> @Test
> public void isEligibleForReturn_under60IncomeJustAboveLimit_returnsTrue() {
>     // Arrange
>     TaxCalculator c = new TaxCalculator();
>     // Act
>     boolean eligible = c.isEligibleForReturn(new BigDecimal("250000.01"), 30);
>     // Assert
>     assertTrue(eligible);
> }
>
> @Test
> public void isEligibleForReturn_seniorIncomeAtLimit_returnsFalse() { ... 300000, age 65 → assertFalse }
>
> @Test
> public void isEligibleForReturn_seniorIncomeJustAboveLimit_returnsTrue() { ... 300000.01, age 65 → assertTrue }
>
> @Test
> public void isEligibleForReturn_superSeniorIncomeAtLimit_returnsFalse() { ... 500000, age 85 → assertFalse }
>
> @Test
> public void isEligibleForReturn_superSeniorIncomeJustAboveLimit_returnsTrue() { ... 500000.01, age 85 → assertTrue }
>
> @Test
> public void isEligibleForReturn_age60IncomeBetweenLimits_returnsFalse() {
>     // Arrange
>     TaxCalculator c = new TaxCalculator();
>     // Act: 2.8 L is above the under-60 limit but within the senior limit
>     boolean eligible = c.isEligibleForReturn(new BigDecimal("280000"), 60);
>     // Assert
>     assertFalse(eligible);
> }
>
> @Test
> public void isEligibleForReturn_age80IncomeBetweenLimits_returnsFalse() {
>     // Arrange
>     TaxCalculator c = new TaxCalculator();
>     // Act: 4 L is above the senior limit but within the super-senior limit
>     boolean eligible = c.isEligibleForReturn(new BigDecimal("400000"), 80);
>     // Assert
>     assertFalse(eligible);
> }
>
> @Test
> public void isEligibleForReturn_salariedAdultTwelveLakh_returnsTrue() {
>     // Arrange
>     TaxCalculator c = new TaxCalculator();
>     // Act
>     boolean eligible = c.isEligibleForReturn(new BigDecimal("1200000"), 35);
>     // Assert
>     assertTrue(eligible);
> }
> ```
> (The elided tests have the same Arrange/Act/Assert body as the ones shown. The full code is in `test/TaxCalculatorTest.java`.)

Pasted all 12 tests into `test/TaxCalculatorTest.java`. `make test` → **15 tests successful, 0 failed**.

## Part C — Read every assertion

For each assertion I asked whether it encodes a contract or restates the code:

| Test | Expected value derived from | Verdict |
|---|---|---|
| `nullIncome_returnsFalse` | documented "invalid input → not eligible" rule | contract, kept |
| `negativeAge_returnsFalse` | same rule (an age cannot be negative) | contract, kept |
| `zeroIncome_returnsFalse` | zero income is under every limit | contract, kept |
| `under60IncomeAtLimit_returnsFalse` | ₹2.5 L limit; filing is required only *above* the limit | contract, kept |
| `under60IncomeJustAboveLimit_returnsTrue` | ₹2.5 L limit + 1 paisa | contract, kept |
| `seniorIncomeAtLimit/JustAbove` | ₹3 L senior limit | contract, kept |
| `superSeniorIncomeAtLimit/JustAbove` | ₹5 L super-senior limit | contract, kept |
| `age60IncomeBetweenLimits_returnsFalse` | age 60 belongs to the senior band | contract, kept |
| `age80IncomeBetweenLimits_returnsFalse` | age 80 belongs to the super-senior band | contract, kept |
| `salariedAdultTwelveLakh_returnsTrue` | ₹12 L is well above any limit | contract, kept |

Every assertion is a literal `assertTrue`/`assertFalse` on hand-picked inputs.
No expected value is computed from the input, and none of them re-evaluates
something like `income.compareTo(threshold) > 0` inside the test. **Nothing
was deleted or rewritten.** The prompt's "no implementation-copying" rule
plus a boolean return type left little room for tautologies.

## Part D — Mutation testing

`make mutation` → `build/reports/pitest/index.html`

| Run | `isEligibleForReturn` | Whole `TaxCalculator` |
|---|---|---|
| Seed tests only | 0/11 killed (all NO_COVERAGE) | 15/42 = 36% |
| + Part B tests | **10/11** killed (91%) | 25/42 = 60% |
| + Part D targeted test | **11/11** killed (100%) | 26/42 = 62% |

Surviving mutant in the chosen method after Part B:

- line 70: `ageYears < 0` → `ageYears <= 0` (*changed conditional boundary*) — SURVIVED

### Prompt

```
The mutant at line 70 (`ageYears < 0` -> `ageYears <= 0`) survived
the test suite. Write ONE new JUnit test that would kill this
mutant without changing any other test. Explain in one
sentence why this test kills the mutant.
```

### AI reply (verbatim)

> ```java
> @Test
> public void isEligibleForReturn_ageZeroIncomeAboveLimit_returnsTrue() {
>     // Arrange
>     TaxCalculator c = new TaxCalculator();
>     // Act
>     boolean eligible = c.isEligibleForReturn(new BigDecimal("400000"), 0);
>     // Assert
>     assertTrue(eligible);
> }
> ```
> The mutant rejects age 0 as invalid and returns `false`. Age 0 is a valid age,
> though, and ₹4 L is above the under-60 limit, so the original code returns
> `true` and the mutant fails the assertion.

Added the test and re-ran PIT: line 70 mutant now **KILLED**, and `isEligibleForReturn` is at 11/11.

Other survivors elsewhere in the class that were left untouched, since they are outside the chosen method:
the conditional-boundary mutants on lines 13, 20, 23 and 26 (`computeIncomeTax`), 38 and 41 (`computeVAT`), and 55 and 57 (`applyExemption`).

## Part E — Reflect

| | Baseline | After Part B + D |
|---|---|---|
| `TaxCalculator` line coverage | 20/41 = 48.8% | 28/41 = **68.3%** |
| `TaxCalculator` branch coverage | 15/44 = 34.1% | 25/44 = **56.8%** |
| `isEligibleForReturn` branch coverage | 0/10 | 10/10 |
| (report total line / branch) | (39.8% / 31.3%) | (68.9% / 52.1%) |
| Mutation score (`TaxCalculator`) | 15/42 = 36% | 25/42 = 60% → **26/42 = 62%** after the targeted test |
| Mutation score (`isEligibleForReturn`) | 0/11 | 10/11 → **11/11** after the targeted test |

Mutation testing was the more useful feedback: coverage already showed
`isEligibleForReturn` at 100% branches after Part B, but PIT still found an
untested boundary (age 0), and that gap would have hidden an off-by-one bug.

---

# Session 5B — Auto-documentation

**Starter gap:** `m5-pub.tgz` (downloaded 2026-09-30) does not contain
`OrderApi.java`, even though the handout and the starter README both describe
it. I added it in a separate commit, before any documentation work, with no
JavaDoc:

- `src/OrderApi.java`: `@RestController @RequestMapping("/orders")` with four
  endpoints over `OrderService`: `GET /orders`, `GET /orders/{id}` (404 if
  unknown), `POST /orders` (400 on invalid body, 201 + Location on success),
  and `DELETE /orders/{id}` (cancel: 204, 404 unknown, 409 already cancelled).
- `src/CreateOrderRequest.java`: request record `(customerId, amount)`.
- `src/SpringStubs.java`, `src/ResponseEntity.java`: fake Spring annotations
  and a minimal `ResponseEntity`, so the code compiles without Spring (as the
  starter README describes).

## Part A — JavaDoc for `POST /orders` (`OrderApi.create`)

### Prompt 1 (generation)

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

### AI reply (first draft, verbatim)

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
> No `@throws`: the method declares no checked exceptions, and invalid input is
> reported as a 400 response rather than thrown.

### Prompt 2 (verification, pass 1)

```
Given the JavaDoc above and the method body below,
identify any statement in the JavaDoc that is inconsistent
with the code. Do not fix; just list.

<method body as above>
```

### AI reply (pass 1)

> 1. "deserialized from the JSON request body": nothing in the code fixes the
>    media type (no `consumes`, and the annotations are stubs with no message
>    converter), so "JSON" is not supported by the code.
> 2. "server-generated UUID id and the initial status NEW": neither the id
>    format nor the initial status appears in this method body. They come from
>    `OrderService.create` / the `Order` constructor, so as written the
>    JavaDoc states behaviour this method does not show.

**Fixes:** (1) replaced "deserialized from the JSON request body" with "taken
from the request body". (2) Checked `OrderService.create` (`UUID.randomUUID()`)
and the `Order` constructor (`status = "NEW"`). The claim is true, so I kept
it but attributed it with `{@link OrderService#create}` and weakened "UUID" to
"unique id", because the UUID format is a detail of the service
implementation. While editing I also added two contract facts the caller should know:
"amount (INR)", "nothing is created in that case", and "customer id is not
checked against any customer registry".

### Verification, pass 2

I sent the same prompt with the revised JavaDoc and the method body, plus
`OrderService.create` and the `Order` constructor, since the JavaDoc now links to them.

> No inconsistencies.

Final JavaDoc is in `src/OrderApi.java` above `create`.

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

<file tree: Makefile, README.md, .gitignore, src/{TaxCalculator, Order, OrderDto,
OrderService, OrderApi, CreateOrderRequest, ResponseEntity, SpringStubs}.java,
test/TaxCalculatorTest.java>
<contents of Makefile, starter README.md, TaxCalculator.java, OrderApi.java>
```

The raw reply is saved verbatim as `README.raw.md`.

### Grep verification of claims in the raw draft

| Claim in `README.raw.md` | Check | Result |
|---|---|---|
| "Start the application", `curl … http://localhost:8080/orders` with JSON | `grep -rnE "main\(\|SpringApplication\|8080\|server\|json"` | **no match**: there is no entry point, no HTTP server and no JSON binding. **Invented, deleted.** |
| "`OrderApi` is a Spring `@RestController` exposing `/orders` endpoints" | `src/SpringStubs.java`, Makefile classpath | **Misleading**: the annotations are local stubs and Spring is not on the classpath, so nothing is exposed. **Invented capability, rewritten** as "a controller written in Spring MVC style… not served over HTTP". |
| "See `LICENSE` for details" | `ls` | **no `LICENSE` file**. **Deleted**, replaced with an explicit MIT placeholder. |
| Tax slabs, GST rates, exemption cap, make targets, report paths | read `TaxCalculator.java`, `Makefile` | correct, kept |
| `computeIncomeTax(700000)` → 52500 | ran it | correct (prints `52500.00`) |

**Invented features deleted: 3.** These were the runnable HTTP server and curl
example, the claim that Spring actually serves the endpoints, and the
`LICENSE` file.

### Hand edits for `README.md`

- Wrote the one-line description myself.
- Expanded the description to cover each `TaxCalculator` method's contract.
- Replaced the curl example with a Java example that calls `OrderApi`
  directly. I compiled and ran it to confirm it gives 52500.00, 180.00,
  false, 201 and `/orders/<uuid>`.
- Replaced the `TODO` under **Contributing** with real guidance taken from
  the Makefile's `--targetTests=*Test`, the test conventions used in this
  module and `PROMPTS.md`.

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

<full OrderApi.java, plus OrderDto.java and CreateOrderRequest.java for the DTO shapes>
```

The output is saved as `openapi.yaml`. The AI added two TODOs: the unspecified
ordering of `GET /orders`, and `status` being a free-form String in `Order`.

### Verification

The browser check in editor.swagger.io is still to be done by hand. In the
meantime I ran a script that parses `openapi.yaml` and cross-checks it
against the `@*Mapping` annotations in `OrderApi.java`:

| Check | Result |
|---|---|
| Every endpoint present with correct path + method | 4/4 (`GET /orders`, `POST /orders`, `GET /orders/{id}`, `DELETE /orders/{id}`) |
| Every `$ref` defined in `components` | `OrderDto`, `CreateOrderRequest` both defined |
| ≥1 2xx and ≥1 4xx per operation | first AI output: **`GET /orders` had only `200`**, the other 3 passed |

**Hand fix:** added `406 Not Acceptable` to `GET /orders`. The controller has no
failure path of its own, so the framework-level content-negotiation failure
is the only honest 4xx for that endpoint. After the fix all four operations pass.
4xx responses have no body schema because every 4xx in `OrderApi` returns an
empty body. An invented `Error` schema would contradict the code.

## Part D — Reflect

- **JavaDoc inconsistencies caught by the AI self-check on the first pass:
  2.** One was a real overclaim ("JSON"). The other was true but was not
  backed by the method body (UUID/NEW come from the service). The second pass
  found none.
- **Invented features in the README pass: 3.** These were the runnable HTTP
  server with a curl example, Spring actually serving the endpoints, and a
  `LICENSE` file.
- **Most hand editing: the README.** JavaDoc and OpenAPI are generated
  from a single file and can be checked mechanically against it, with the
  self-check prompt and the mapping and `$ref` script. A README has to
  summarise the whole repository, including how to *run* it. The model filled
  that gap with the usual assumptions for a Spring project (a server on 8080,
  curl, a LICENSE file), and each of those had to be grep-checked and
  rewritten by hand. Its Quick example and Contributing sections were mostly
  rewritten.
