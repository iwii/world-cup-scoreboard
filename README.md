# world-cup-scoreboard
Java library for managing live Football World Cup matches and scoreboard summaries.

## Requirements

- Java 21
- Maven 3.10.+

## Build and test

```bash
mvn clean verify
```

Runs the tests, generates the coverage report (`target/site/jacoco/index.html`)
and enforces a minimum of 80% line coverage.

## 1. Assumptions

- TODO: assumptions about match data (team names, score values, duplicates, ordering of matches with equal total score)
- TODO: assumptions about concurrency (single-threaded or thread-safe)
- TODO: assumptions about invalid input handling (null or blank names, negative scores, unknown match)
- The library is in-memory only, with no persistence and no external runtime dependencies.

## 2. Reasoning

- Java 21 as the target (`maven.compiler.release`), with no third-party runtime dependencies to keep the library simple.
- JUnit 6.1.2 (via `junit-bom`) for tests and its built-in assertions.
- JaCoCo with a coverage check in the `verify` phase, so the quality gate fails the build.
- `Automatic-Module-Name` set to `com.sportradar.worldcup.scoreboard`, giving a stable module name without a full `module-info.java`.
- No Maven Wrapper. The project is built with an installed Maven.
- TODO: reasoning behind the chosen data structures and public API

## 3. Trade-offs

- `Automatic-Module-Name` instead of `module-info.java`: simpler and enough for a small library, but gives no strong encapsulation of internal packages.
- 80% line coverage threshold: enforces discipline, but can fail the build when code is added before tests.
- No Maven Wrapper: less files in the repository, but contributors must install a compatible Maven themselves.
- TODO: trade-offs of the implementation (for example simplicity vs. performance, immutability vs. mutability of match objects)

## AI usage

See [AI.md](AI.md) for the history of AI-assisted prompts and how the suggestions were reviewed.