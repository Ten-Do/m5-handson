# m5-pub

A teaching starter with a simplified Indian income-tax/GST calculator and an
in-memory order controller. It is used to practise AI-assisted unit testing
(JUnit 5, JaCoCo, PIT) and documentation (JavaDoc, README, OpenAPI).

## Description

- **`TaxCalculator`** (`src/TaxCalculator.java`), all amounts in INR as `BigDecimal`:
  - `computeIncomeTax`: old-regime slabs, 0% up to ₹2.5 L, 5% up to ₹5 L,
    20% up to ₹10 L, 30% above. Throws `IllegalArgumentException` for null or
    negative income.
  - `computeVAT`: GST at 0, 5, 12, 18 or 28%, rounded to 2 decimals. Any
    other rate throws.
  - `applyExemption`: subtracts an exemption capped at ₹1.5 L and never goes
    below zero.
  - `roundToPaise`: rounds half-up to 2 decimals.
  - `isEligibleForReturn`: true when income is above the age-band limit
    (₹2.5 L under 60, ₹3 L for 60–79, ₹5 L for 80+).
- **`OrderApi`** (`src/OrderApi.java`): a controller written in Spring MVC
  style over the in-memory `OrderService`, with four endpoints:
  `GET /orders`, `GET /orders/{id}`, `POST /orders` and `DELETE /orders/{id}`
  (cancel). The HTTP contract is described in [`openapi.yaml`](openapi.yaml).
  Spring is **not** a dependency: the annotations and `ResponseEntity` are
  local stubs (`src/SpringStubs.java`, `src/ResponseEntity.java`), so the
  controller compiles but is not served over HTTP.

## Build

Needs JDK 17 or newer, `make` and `curl`. JUnit 5, JaCoCo and PIT jars are
downloaded into `libs/` on first use.

```sh
make deps && make test   # compile src/ + test/ and run the JUnit 5 suite
make coverage            # JaCoCo HTML report in coverage/index.html
make mutation            # PIT report for TaxCalculator in build/reports/pitest/index.html
make clean               # remove build/, libs/, coverage/
```

## Quick example

```java
TaxCalculator calc = new TaxCalculator();
calc.computeIncomeTax(new BigDecimal("700000"));       // 52500.00 (12,500 + 40,000)
calc.computeVAT(new BigDecimal("1000"), 18);           // 180.00
calc.isEligibleForReturn(new BigDecimal("280000"), 65); // false (senior limit is 3 L)

OrderApi api = new OrderApi(new OrderService());
ResponseEntity<OrderDto> r =
        api.create(new CreateOrderRequest("c-42", new BigDecimal("1500.00")));
r.getStatusCode();   // 201
r.getLocation();     // /orders/<generated id>
```

## Contributing

1. Put production code in `src/` and tests in `test/`. Test classes must be
   named `*Test` so that `make mutation` picks them up.
2. Follow the existing test style: AAA comments, one assertion per test, names
   of the form `<method>_<condition>_<expected>`, and no mocks.
3. Before you submit, run `make test`, `make coverage` and `make mutation`, and
   check that coverage and mutation score have not dropped.
4. Record any AI prompts you used in `PROMPTS.md`.

## License

MIT License — Copyright (c) <YEAR> <COPYRIGHT HOLDER>. (Placeholder: add a
`LICENSE` file with the full MIT text before you distribute.)
