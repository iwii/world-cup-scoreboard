# AI Usage Documentation

## Table of Contents

1. [AI Tool Usage Summary](#1-ai-tool-usage-summary)
2. [Prompt History](#2-prompt-history)
   - [2.1 Development Environment Setup](#21-development-environment-setup)
     - [2.1.1 gitignore](#211-gitignore)
     - [2.1.2 pom.xml template](#212-pomxml-template)
     - [2.1.3 Using Homebrew Maven in IntelliJ](#213-using-homebrew-maven-in-intellij)
     - [2.1.4 Trimming the configuration](#214-trimming-the-configuration)
     - [2.1.5 Automatic-Module-Name](#215-automatic-module-name)
     - [2.1.6 README.md](#216-readmemd)
   - [2.2 Requirements Analysis](#22-requirements-analysis)
     - [2.2.1 Scope and Library Responsibilities](#221-scope-and-library-responsibilities)
     - [2.2.2 Functional and Non-functional Requirements](#222-functional-and-non-functional-requirements)
   - [2.3 Design Decisions](#23-design-decisions)
     - [2.3.1 Domain Model and Immutability](#231-domain-model-and-immutability)
     - [2.3.2 Match Identification and Storage](#232-match-identification-and-storage)
     - [2.3.3 Match Lifecycle and Finished Matches](#233-match-lifecycle-and-finished-matches)
     - [2.3.4 Public API and Error Handling](#234-public-api-and-error-handling)
     - [2.3.5 Concurrency, Summary Ordering and Performance](#235-concurrency-summary-ordering-and-performance)
   - [2.4 Implementation](#24-implementation)
   - [2.5 Testing](#25-testing)
     - [2.5.1 Test Scenarios and Validation Strategy](#251-test-scenarios-and-validation-strategy)
     - [2.5.2 Test Implementation (TDD)](#252-test-implementation-tdd)
   - [2.6 Code Review and Refactoring](#26-code-review-and-refactoring)
     - [2.6.1 Changing Automatic-Module-Name](#261-changing-automatic-module-name)
     - [2.6.2 Decoupling MatchNotFoundException from MatchId](#262-decoupling-matchnotfoundexception-from-matchid)
   - [2.7 Documentation Review](#27-documentation-review)
3. [Human Review and Validation](#3-human-review-and-validation)

## 1. AI Tool Usage Summary

Tool: Claude (Anthropic), used through the chat interface, model/version: Sonnet 5.5 Medium and ChatGPT version 6.

AI was used as a supporting tool for:
- Development environment and build setup (.gitignore, pom.xml, Maven in IntelliJ)
- Requirements analysis
- Design and architectural discussions
- Code generation and implementation suggestions
- Unit test design
- Code review and refactoring suggestions
- Documentation (README.md, AI.md)

Prompts are listed in English translation. In sections 2.2 and 2.3 the prompts are consolidated: several related exchanges are merged into one entry. Each entry lists the prompt, its context and the outcome, including what I changed or rejected. The rationale for the final design is in the README.

## 2. Prompt History

### 2.1 Development Environment Setup

#### 2.1.1 gitignore

**Prompt:** What should be included in the .gitignore file for a Java Maven project developed using IntelliJ IDEA on macOS?

**Context:** Java library built with Maven; IntelliJ IDEA on macOS.

**Outcome:** I used the proposed .gitignore for IntelliJ IDEA, Maven, compiled artifacts, logs and macOS files. I removed all Maven Wrapper entries because I build with my own Maven installation, and kept the plugin-specific entries (release, versions, shade, buildnumber) as a harmless precaution.

#### 2.1.2 pom.xml template

**Prompt:** Prepare pom.xml template with the newest artifact for JUnit asserts, code coverage.

**Context:** Java 21, Maven. Versions checked online: JUnit 6.1.2 (latest stable at the time) and JaCoCo 0.8.15 (newest found). Versions of maven-compiler-plugin, maven-surefire-plugin and maven-jar-plugin came from model knowledge and were not verified.

**Outcome:** I used the proposed pom.xml as the base:
- Java 21 via `maven.compiler.release`, UTF-8 encoding.
- JUnit 6.1.2 through `junit-bom` and the `junit-jupiter` aggregator (built-in assertions are enough).
- JaCoCo 0.8.15 with a report and an 80% line coverage check in the `verify` phase.
- Compiler, surefire and jar plugin versions pinned in properties.
- `junit-platform-launcher` left commented out until `mvn clean verify` shows whether it is needed.

Open item: verify the plugin versions with `mvn versions:display-plugin-updates`.

#### 2.1.3 Using Homebrew Maven in IntelliJ

**Prompt:** How to check whether IntelliJ uses Maven installed via Homebrew, and how to set it.

**Context:** IntelliJ offered only Bundled Maven (3.9.16) and the Maven Wrapper, while the terminal used Maven 3.10.0.

**Outcome:** I set the Maven home path manually to the Homebrew installation (libexec directory), so the IDE and the terminal build with the same Maven version (3.10.0).

#### 2.1.4 Trimming the configuration

**Prompt:** Why were the JaCoCo check execution and maven-jar-plugin removed from the slimmed-down pom.xml?

**Context:** A slimmed-down pom.xml was proposed after the decision not to use the Maven Wrapper. The check execution and maven-jar-plugin were dropped on the assumption they were optional.

**Outcome:** The AI had dropped both without my decision, so I restored them: the check enforces the 80% coverage threshold in `mvn verify`, and maven-jar-plugin keeps its version pinned for reproducible builds.

#### 2.1.5 Automatic-Module-Name

**Prompt:** How is the JAR name determined in a Maven project, and how can I make the module name stable when using maven-jar-plugin?

**Context:** The library has no module-info.java and may be consumed by others. Without a manifest entry the module name is derived from the JAR file name, so it can change between versions.

**Outcome:** I chose `Automatic-Module-Name` in the JAR manifest (via maven-jar-plugin) instead of a full module-info.java: simpler and enough for a small library. I replaced the template placeholder with `com.sportradar.worldcup.scoreboard`, matching the main package, and check the result with `jar --describe-module`.

#### 2.1.6 README.md

**Prompt:** Prepare a README file with assumptions, reasoning and trade-offs now.

**Context:** The README is required. Implementation had not started, but the build decisions (Java 21, Maven without Wrapper, JUnit, JaCoCo, Automatic-Module-Name) were already made.

**Outcome:** I created a README skeleton with assumptions, reasoning and trade-offs, pre-filled with the build decisions. Domain assumptions, API reasoning and implementation trade-offs were added after the design work (see 2.7).

### 2.2 Requirements Analysis

#### 2.2.1 Scope and Library Responsibilities

**Prompt:** Define the responsibilities and boundaries of a standalone, in-memory Football World Cup Scoreboard library. Identify which concerns belong to the library and which should remain outside its scope.

**Context:** The assignment requires a Java library with operations to start matches, update scores, finish matches and retrieve a summary of ongoing matches, working independently without external services or persistent storage.

**Outcome:** I defined the library as a standalone component that manages only the current in-memory scoreboard state. The public API exposes the match operations and the necessary domain types; storage, synchronization and validation stay internal.

Out of scope: persistence, external integrations, event processing and provider-level concerns (duplicate event detection, event ordering, conflict resolution between providers). The design targets a single-JVM library, not a production sports data platform.

#### 2.2.2 Functional and Non-functional Requirements

**Prompt:** Identify the functional and non-functional requirements, including relevant edge cases and assumptions, for the Football World Cup Scoreboard library.

**Context:** The assignment specifies the core operations but leaves some edge cases open to interpretation.

**Outcome:** The full list of assumptions is in the README. The additions I made to the assignment text:
- Validation: team names not null or blank after trimming, original spelling preserved, home and away teams different (ignoring case), no negative scores.
- Score updates use absolute values, so corrections are possible.
- Finishing a match removes it from the scoreboard; later operations on its ID are rejected as unknown (see 2.3.3).
- A team may take part in several ongoing matches, and the same teams may meet again, so team names cannot identify a match.
- Non-functional: in-memory, single JVM, thread-safe public operations, immutable result objects, encapsulated internals.
- Scores are `int`; the total used for ordering is `long` to avoid overflow. No maximum score is imposed because the assignment defines none.

### 2.3 Design Decisions

The design was explored with AI assistance and some topics were revisited as the requirements became clearer. Each entry below consolidates the final decisions.

#### 2.3.1 Domain Model and Immutability

**Prompt:** Propose a minimal domain model for the scoreboard. Evaluate the use of classes and records, the responsibilities of each component, and the benefits and trade-offs of an immutable domain model.

**Context:** The library manages multiple matches and their scores. The design should preserve encapsulation, avoid unnecessary abstractions and support thread-safe operations.

**Outcome:** I chose a minimal model:
- `Scoreboard` coordinates the operations, guards access and produces the summary.
- `Match` is an immutable class holding the match state; `withScore()` returns a new instance.
- `MatchId` (wraps a UUID), `Team` (validates and stores the name) and `MatchSummary` (returned by the public API) are immutable records.

The scoreboard keeps a private mutable `HashMap<MatchId, Match>` and replaces the stored instance on every change, so the collection is mutable but the domain objects are not. `Team` keeps the default `equals()`/`hashCode()` because teams are never used as keys; the home/away check uses `equalsIgnoreCase()` explicitly.

**Rejected:** a separate `Score` type (an extra abstraction without enough value) and conventional getters/setters.

#### 2.3.2 Match Identification and Storage

**Prompt:** Evaluate how matches should be identified and stored. Compare team-based identifiers, composite keys and generated unique identifiers, considering multiple matches involving the same teams.

**Context:** The scoreboard must support multiple simultaneous matches; the same team may play several matches and the same two teams may meet more than once. No integration with external match providers is required.

**Outcome:** I chose a `MatchId` generated by `Scoreboard.startMatch()` with `UUID.randomUUID()`; clients use the returned ID in later operations. Matches are stored in a `HashMap<MatchId, Match>` (average O(1) lookup and insertion), and `startMatch()` must never overwrite an existing entry. Match identity is independent of team names.

**Rejected:** team-based IDs and composite keys (ambiguous for repeated pairings) and client-supplied IDs (relevant only with external providers, which are out of scope).

#### 2.3.3 Match Lifecycle and Finished Matches

**Prompt:** Define the match lifecycle and determine how score updates and finished matches should be handled in an immutable domain model.

**Context:** A match starts as ongoing, can be updated and is eventually finished. The assignment requires a summary of ongoing matches but does not say how finished matches are stored or how later operations behave.

**Follow-up prompt:** How would you solve this case: finished matches are never removed from the map, so memory grows over time?

**Outcome:** My first design kept finished matches in the map with a `FINISHED` status, which allowed clear errors on repeated operations but made memory grow without bound. I replaced it: `finishMatch` removes the match from the map and returns its final `MatchSummary`. Every later operation on that ID throws `MatchNotFoundException`. The map therefore holds only ongoing matches, there is no status enum and no lifecycle state machine, and the summary only sorts. Score updates use absolute values (so VAR corrections can lower a score) and reject negative values. Updates that arrive after the match is finished are rejected too; handling late events is the caller's responsibility (see 2.2.1).

**Rejected:** keeping finished matches with a status, expiry by TTL or size limit (needs a clock and configuration without a requirement), a set of finished IDs (still grows), a separate archive (history is out of scope, see 2.2.1), and accepting corrections after a match is finished (requires keeping finished matches).

#### 2.3.4 Public API and Error Handling

**Prompt:** Design a simple public API for the scoreboard. Determine what each operation should return, how clients should access match information and how invalid operations should be reported.

**Context:** The library must expose the required operations without exposing internal storage or mutable state. The design uses immutable domain objects and internally generated IDs.

**Outcome:** All lifecycle operations return a `MatchSummary` (ID, teams, scores) instead of `void`, so clients get the resulting state without another call:
- `startMatch()` returns the new match with its generated `MatchId`.
- `updateScore()` returns the updated match.
- `finishMatch()` returns the final result and removes the match from the scoreboard.
- `getSummary()` returns an immutable list of ongoing matches (empty when there are none).

Errors are unchecked exceptions deriving from `ScoreboardException`: `MatchNotFoundException` (unknown or already finished ID), `InvalidTeamException` (blank name, identical teams) and `InvalidScoreException` (e.g. negative score). Clients can catch the base type or a specific one. `NullPointerException` is kept for null arguments.

**Rejected:** exposing `Match` objects, returning `void` or missing values to signal errors, and a custom exception for every null input.

#### 2.3.5 Concurrency, Summary Ordering and Performance

**Prompt:** Evaluate how to ensure thread-safe access to the scoreboard and deterministic summary ordering. Compare synchronization and collection alternatives, and assess the performance implications of sorting matches when the summary is requested.

**Context:** In-memory, single JVM, possibly accessed by multiple threads. Scores change over time, which affects ordering. Unnecessary concurrency and performance optimizations should be avoided.

**Outcome:** A single shared `synchronized` monitor protects all public operations, so each operation is atomic and the summary is a consistent snapshot. The summary is built on request by sorting the matches on the scoreboard by total score descending. Ties are broken by a monotonically increasing `long` sequence number assigned in the synchronized `startMatch()`; it is internal and not exposed. Complexity: O(1) average lookup and insertion, O(n log n) sorting per summary.

**Rejected:** `ConcurrentHashMap`, `ReadWriteLock` and fine-grained locking (complexity not justified); a continuously ordered `TreeSet` (needs remove and re-insert on every score change); `PriorityQueue` (does not re-order after a score change and cannot be iterated in order); cached summaries (premature without evidence of a performance problem).

### 2.4 Implementation

_To be completed after the implementation._

### 2.5 Testing

#### 2.5.1 Test Scenarios and Validation Strategy

**Prompt:** Based on the requirements and the proposed API, identify unit and concurrency test scenarios for the scoreboard. Cover:

1. Normal operation: starting a match, updating its score and finishing it.
2. Input validation: null or blank team names, identical home and away teams, negative scores and null arguments.
3. Invalid lifecycle transitions: updating a finished match and finishing it more than once.
4. Missing matches: operations using an unknown `MatchId`.
5. Summary behavior: sorting by total score, tie-breaking by start order, empty scoreboard and exclusion of finished matches.
6. Immutability: verify that returned summary lists and previously retrieved snapshots are not affected by subsequent scoreboard changes.
7. Concurrent operations: simultaneous match creation (including matches involving the same teams), concurrent score updates and concurrent attempts to finish the same match.
8. Concurrent summary retrieval while other threads update matches.

For each scenario, describe the expected result and recommend a testing approach using JUnit Jupiter and Java 21 without unnecessary dependencies. Explain how to coordinate concurrent tests deterministically, for example with `ExecutorService`, `CountDownLatch` and appropriate timeouts. Distinguish between guaranteed outcomes and results that depend on the order in which concurrent operations acquire the synchronization monitor.

**Context:** In-memory Java 21 Maven project with immutable domain objects, a private `HashMap`, internally generated match IDs and a shared `synchronized` monitor.

**Outcome:** The strategy covers the eight scenario groups above. Decisions:

- JUnit Jupiter (including parameterized tests) and the Java 21 concurrency utilities only, no extra test dependencies.
- The test suite was prepared before production implementation. The implementation is intended to follow the Red-Green-Refactor cycle, with tests guiding incremental development.
- Concurrent tests use `ExecutorService` and `CountDownLatch` for a simultaneous start, with timeouts so that a failing test cannot hang.
- Tests assert guarantees (unique IDs, exactly one successful finish, atomic score updates without partially applied scores, consistent snapshots) and do not assume a thread scheduling order. Where the outcome depends on which operation acquires the monitor first (an update racing with a finish), the test accepts exactly the two valid outcomes and checks that each is coherent.
- Tests check observable API behavior, not internals.
- After a match is finished, updating or finishing it again is expected to fail with `MatchNotFoundException`; in concurrent finish attempts exactly one call succeeds and the others get `MatchNotFoundException`.

Prompt items 3 and 5 were written before the decision to remove finished matches (see 2.3.3); in the final design they become "`MatchNotFoundException` for a finished match" and "a finished match is no longer in the summary".

Results will be verified with `mvn clean verify`.


#### 2.5.2 Test Implementation (TDD)

**Prompt:** Based on the finalized requirements and design decisions, review the test suite prepared following the TDD approach. Verify that section 2.5.1 accurately describes the testing strategy, covered scenarios and implementation choices. Identify any missing edge cases or inconsistencies with the established requirements.

**Context:** The README.md and AI.md design decisions are finalized. The first iteration of the tests is complete. Production implementation has not started; the public API signatures are being established through the tests.

**Outcome:** I wrote two test classes before implementing the production code: `ScoreboardTest` (functional tests grouped by operation) and `ScoreboardConcurrencyTest` (concurrency tests with a small helper coordinating simultaneous task execution). The tests establish the expected public API contract.

Edge cases covered, beyond the basic scenarios:

- **Names:** trimming, whitespace-only names according to `String.isBlank()`, identical teams ignoring case and surrounding whitespace.
- **Scores:** `Integer.MIN_VALUE` (rejected as negative) and `Integer.MAX_VALUE` in both scores (the `long` total must not overflow when ordering).
- **Atomicity:** a rejected update or a rejected start leaves the scoreboard unchanged.
- **Ordering:** ties broken by start order and not by update order, 50 matches with equal scores (verifying deterministic ordering independent of `HashMap` iteration order or UUID values), equal totals with different score splits, and the assignment's reference example.
- **IDs:** an ID from another `Scoreboard` instance counts as unknown, and a match can be restarted after being finished.
- **Immutability:** unmodifiable summary list (including when empty), snapshots unaffected by subsequent changes.
- **Concurrency:** unique IDs on simultaneous starts, atomic score updates without partially applied scores, exactly one successful finish, an update racing with a finish, and consistent snapshots (size, ordering and tie-break) while other threads update matches.

**Rejected:** Mocking and additional assertion libraries (unnecessary for an in-memory library); `Thread.sleep()` for synchronization (replaced by latches and `Future.get()` with timeouts).

### 2.6 Code Review and Refactoring

This section records my own review findings and decisions, not prompts. Where an issue was first raised by an AI review, the entry says so and points to the prompt.

#### 2.6.1 Changing Automatic-Module-Name

**Finding:** After the exceptions were placed in `com.sportradar.worldcup.exception`, `Automatic-Module-Name` (`com.sportradar.worldcup.scoreboard`) was no longer the prefix of all packages.

**Decision:** I changed the package layout accordingly and set `Automatic-Module-Name` to `com.sportradar.worldcup`, the common root of both packages, in the maven-jar-plugin configuration and in the README (Build decisions). I will check the result with `jar --describe-module`.

**Considered:** moving the exceptions into `com.sportradar.worldcup.scoreboard.exception`, which would have kept the old module name.

#### 2.6.2 Decoupling MatchNotFoundException from MatchId

**Finding:** `MatchNotFoundException` imported `MatchId` from a package that does not exist. It would also have made the `exception` package depend on `scoreboard` while `scoreboard` depends on `exception`. 

**Decision:** The constructor takes a `String` message and `Scoreboard` builds it, so the dependency goes only from `scoreboard` to `exception`. The tests are not affected, because they never construct this exception.

_More entries will be added during the implementation._

### 2.7 Documentation Review

**Prompt:** Check the simplicity and correctness of the README and AI.md for a senior Java developer audience, whether they are over-engineered, and whether anything is missing.

**Context:** README and AI.md drafts written before the implementation.

**Outcome:** I applied the suggestions to the README: fixed the links, removed duplicated points, split "Reasoning" into build and design decisions, and added the missing assumptions (0-0 start, home and away teams must differ) and the ordering rule. I also added a trade-off for allowing a team in several simultaneous matches, and postponed the usage example until the API exists. In AI.md I normalized the headings, shortened the outcomes to decisions and rejected alternatives, and marked unfinished sections explicitly. I also replaced keeping finished matches in memory with removal on finish and documented how late updates are handled (see 2.3.3).

## 3. Human Review and Validation

All AI suggestions were reviewed before being incorporated. Suggestions I changed or corrected so far:
- Maven Wrapper entries removed (2.1.1).
- JaCoCo check and maven-jar-plugin restored (2.1.4).
- Initial design kept finished matches in the map with a status; replaced by removal on finish (2.3.3).

Verification:
- `mvn clean verify` (tests and 80% line coverage).
- `jar --describe-module` for the module name.
- [To be completed after the implementation: tests reviewed by hand, results of the concurrency tests, known limitations.]