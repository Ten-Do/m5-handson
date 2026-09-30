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
