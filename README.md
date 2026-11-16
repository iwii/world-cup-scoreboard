# world-cup-scoreboard

Java 21 library for managing live Football World Cup matches and scoreboard summaries.

## Table of Contents

1. [Requirements](#1-requirements)
2. [Build and test](#2-build-and-test)
3. [Usage](#3-usage)
4. [Assumptions](#4-assumptions)
5. [Design decisions](#5-design-decisions)
6. [Build decisions](#6-build decisions)
7. [Additional operation](#7-additional-operation) 
8. [Trade-offs](#8-trade-offs)
9. [AI usage](#9-ai-usage)

## 1. Requirements

**Build environment:**
- Java 21 (JDK)
- Maven 3.10+

**Testing:**
- JUnit 6.1.2
- JaCoCo 0.8.15 (code coverage reporting and verification)

## 2. Build and test

```
mvn clean verify
```

Runs the tests, generates the JaCoCo coverage report (`target/site/jacoco/index.html`) and enforces a minimum of 80% line coverage.

## 3. Usage

```java
Scoreboard scoreboard = new Scoreboard();

MatchSummary mexicoCanada = scoreboard.startMatch("Mexico", "Canada");
MatchSummary spainBrazil  = scoreboard.startMatch("Spain", "Brazil");
MatchSummary germanyFrance = scoreboard.startMatch("Germany", "France");
MatchSummary uruguayItaly = scoreboard.startMatch("Uruguay", "Italy");
MatchSummary argentinaAustralia = scoreboard.startMatch("Argentina", "Australia");

scoreboard.updateScore(mexicoCanada.id(), 0, 5);
scoreboard.updateScore(spainBrazil.id(), 10, 2);
scoreboard.updateScore(germanyFrance.id(), 2, 2);
scoreboard.updateScore(uruguayItaly.id(), 6, 6);
scoreboard.updateScore(argentinaAustralia.id(), 3, 1);

// Ordered by total score descending; ties broken by most recently started first.
scoreboard.getSummary().forEach(m ->
        System.out.printf("%s %d - %d %s%n",
                m.team1(), m.team1Score(), m.team2Score(), m.team2()));
```

EXpected output of the scenario above:
Uruguay 6 - 6 Italy
Spain 10 - 2 Brazil
Mexico 0 - 5 Canada
Argentina 3 - 1 Australia
Germany 2 - 2 France

Finishing a match removes it from the scoreboard and returns its final summary:

```java
MatchSummary finalResult = scoreboard.finishMatch(uruguayItaly.id());
```

Querying a single ongoing match:

```java

Optional<MatchSummary> match = scoreboard.getMatch(spainBrazil.id());
```


## 4. Assumptions

- The library is a standalone, in-memory component running within a single JVM. It does not provide persistence, external data provider integrations or event feed processing.
- A match is identified by a unique ID generated internally by the library, independently of team names.
- The same team may participate in multiple ongoing matches, and the same teams may meet in more than one match.
- Team names must not be null or blank after trimming. The original spelling is preserved. The home and away teams must differ; names are compared ignoring case.
- A match starts with the score 0-0. Negative scores are invalid. Score updates use absolute values rather than increments, allowing corrections (for example, after a VAR decision).
- Finishing a match removes it from the scoreboard. Any later operation on its ID, including a late score update, is rejected with MatchNotFoundException. Handling late or out-of-order events is the responsibility of the caller (e.g. a data provider layer).
- Deduplication of updates arriving from multiple sources is out of scope. The assignment does not require it; the caller is responsible for detecting duplicate or stale events (see AI.md 2.2.1).
- The summary contains only ongoing matches, ordered by total score descending and, for equal totals, by the most recently started match first.
- Public operations are thread-safe within a single JVM.

## Design decisions

- **API:** `startMatch`, `updateScore` and `finishMatch` return an immutable `MatchSummary`; `getSummary` returns an immutable list. The internal `MatchContext` is never exposed.
- **Domain model:** `Scoreboard`, a package-private mutable `MatchContext` guarded by a per-match `ReentrantLock`, the immutable records `MatchId` and `MatchSummary`. Team names are validated `String`s. No third-party runtime dependencies.
- **Storage:** `ConcurrentHashMap<MatchId, MatchContext>`. The map holds only ongoing matches; `finishMatch` removes the entry atomically via `remove(id, context)`.
- **Ordering:** individual scores are `int`; the total used for ordering is `long` to avoid overflow. Ties are broken by a monotonic `long` sequence assigned in `startMatch` and stored only in `MatchContext` (never exposed).
- **Concurrency:** a per-match `ReentrantLock` serializes `updateScore` and `finishMatch` on the same match; different matches proceed concurrently. Method  `getSummary` snapshots each match under its lock, so it is not a globally atomic snapshot (see Trade-offs).
- **Errors:** unchecked exceptions deriving from `ScoreboardException` (`MatchNotFoundException`, `InvalidTeamException`, `InvalidScoreException`, `InvalidMatchIdException`). (`Objects.requireNonNull`) is used for null `MatchId` arguments. Null or blank team names throw 'InvalidTeamException'.

## 6. Build decisions

- Java 21 is the target version (`maven.compiler.release`).
- JUnit is managed through `junit-bom`, and its built-in assertions are used. AssertJ is used in unit tests for fluent assertions.
- JaCoCo enforces the 80% line coverage threshold in the Maven `verify` phase.
- `Automatic-Module-Name` is set to `com.sportradar.worldcup`, giving a stable module name without a full `module-info.java`.
- No Maven Wrapper is included; the project uses an installed Maven distribution.
- - `assertj-core` is pinned to 3.27.7 to avoid CVE-2026-24400 (XXE in `isXmlEqualTo` / `xmlPrettyFormat`). The library never calls those methods and AssertJ is test-scoped, but the bump keeps dependency scanners clean.

## 7. Additional operation


## 8. Trade-offs

- `Automatic-Module-Name` instead of `module-info.java`: simpler and sufficient for a small library, but does not strongly encapsulate internal packages.
- An 80% line coverage threshold: encourages test coverage, but may fail the build when new code is added without tests.
- No Maven Wrapper: keeps the repository smaller, but contributors must install a compatible Maven version themselves.
- `HashMap` with on-demand sorting: updates and lookups stay simple and efficient (O(1) on average), but each summary requires sorting (O(nlogn)).
- Mutable `MatchContext` under a per-match lock, exposed only via immutable `MatchSummary` snapshots: 
  avoids allocating a new domain object on every update, at the cost of a small amount of locking code inside `MatchContext`. 
- Per-match locks: allow concurrent operations on different matches, but `getSummary` is not a globally atomic snapshot 
  and requires locking each context during snapshot.
- Finished matches are removed from the scoreboard: no memory growth and a simpler model (no lifecycle states), but 
  an unknown match ID cannot be told apart from an already finished one, and corrections after the final whistle are not possible.
- A team may play several ongoing matches at once: the assignment does not forbid it and it keeps validation simple, 
  but it does not reflect real-world rules.
- `getSummary` returns an immutable snapshot rather than a live view: callers get a consistent picture at the moment of the call, 
  but must call it again to see later changes.
- `startMatch` has one defensive branch for UUID collision (`putIfAbsent != null`) that is not covered by tests because the probability of a collision is negligible (~2^-122). The loop is kept as a safety net; it is not expected to execute in practice.
- `startMatch` has one defensive branch for a `UUID.randomUUID()` collision inside the `putIfAbsent` retry loop. The probability of a collision is negligible (~2^-122), so the branch is intentionally left without a test. Mocking `MatchId.random()` or excluding `Scoreboard` from JaCoCo would hide real coverage of the other methods, which is worse than a single uncovered defensive branch.

## 9. AI usage

See [AI.md](AI.md) for the history of AI-assisted prompts, design decisions and how the suggestions were reviewed.