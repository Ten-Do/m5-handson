# m5-pub

A small Java codebase containing an Indian income-tax/GST calculator (`TaxCalculator`) and an in-memory order REST API (`OrderApi`), used for AI-assisted testing and documentation exercises.

## Description

- `TaxCalculator` computes old-regime income tax (slabs at ₹2.5 L / ₹5 L / ₹10 L at 5% / 20% / 30%), GST at 0/5/12/18/28%, an 80C-style exemption capped at ₹1.5 L, rounding to paise, and whether an income-tax return must be filed for a given income and age.
- `OrderApi` is a Spring `@RestController` exposing `/orders` endpoints to list, fetch, create and cancel orders, backed by an in-memory `OrderService`.

## Build

Requires a JDK 17+ and `make`; dependencies (JUnit 5, JaCoCo, PIT) are downloaded into `libs/` automatically.

```sh
make deps && make test   # compile and run the JUnit 5 suite
make coverage            # JaCoCo HTML report in coverage/index.html
make mutation            # PIT HTML report in build/reports/pitest/index.html
make clean
```

## Quick example

Start the application and create an order:

```sh
curl -X POST http://localhost:8080/orders \
     -H 'Content-Type: application/json' \
     -d '{"customerId": "c-42", "amount": 1500.00}'
```

```java
TaxCalculator calc = new TaxCalculator();
BigDecimal tax = calc.computeIncomeTax(new BigDecimal("700000")); // 52500
```

## Contributing

TODO

## License

This project is licensed under the MIT License. See `LICENSE` for details.
