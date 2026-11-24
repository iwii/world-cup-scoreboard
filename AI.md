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
     - [2.3.6 Simplification and Architectural Pivot](#236-simplification-and-architectural-pivot)
   - [2.4 Implementation](#24-implementation)
     - [2.4.1 Base Domain Model and TDD Stubs Setup](#241-base-domain-model-and-tdd-stubs-setup)
     - [2.4.2 Unit Tests for Core Lifecycle Operations](#242-unit-tests-for-core-lifecycle-operations)
     - [2.4.3 Maven Project Configuration and Build Setup](#243-maven-project-configuration-and-build-setup)
     - [2.4.4 Edge Cases Unit and Concurrency Tests](#244-edge-cases-unit-and-concurrency-tests)
     - [2.4.5 Core Business Logic and Concurrency Implementation](#245-core-business-logic-and-concurrency-implementation)
     - [2.4.6 Exception Handling and Validation Unit Tests](#246-exception-handling-and-validation-unit-tests)
     - [2.4.7 API Encapsulation and Access Modifier Refinement](#247-api-encapsulation-and-access-modifier-refinement)
     - [2.4.8 Additional Query Operation](#248-additional-query-operation)
   - [2.5 Testing](#25-testing)
     - [2.5.1 Test Scenarios and Validation Strategy](#251-test-scenarios-and-validation-strategy)
     - [2.5.2 Test Hardening and Flakiness Fixes](#252-test-hardening-and-flakiness-fixes)
     - [2.5.3 Defensive Branch Coverage and Dependency Security](#253-defensive-branch-coverage-and-dependency-security)
   - [2.6 AI Code Review and Refactoring](#26-ai-code-review-and-refactoring)
   - [2.7 Documentation Review](#27-documentation-review)
3. [Human Review and Validation](#3-human-review-and-validation)

## 1. AI Tool Usage Summary

Tools: 
- Claude (Anthropic) Sonnet 5.5 Medium; 
- ChatGPT-6 (OpenAI);
- Gemini 4 (Google);
- DeepSeek-V4.1-Flash;
All used through the chat interface.

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

**Status: [ADOPTED, with changes].** The initial template used JUnit 6.1.2 and JaCoCo 0.8.15, but the build failed with unresolved dependencies for these versions on Java 21. I switched to JUnit 5.11.4 and JaCoCo 0.8.12, which are the versions verified to work with Java 21 and the `junit-bom` already in the project. `pom.xml` and README now pin these versions.

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

**Outcome:** I chose `Automatic-Module-Name` in the JAR manifest (via maven-jar-plugin) instead of a full module-info.java: simpler and enough for a small library. I replaced the template placeholder with `com.sportradar.worldcup`, matching the main package, and check the result with `jar --describe-module`.

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

 **Status legend.** Entries in this section are a chronological record.
 Each entry is tagged with its final status: **[ADOPTED]**, **[ADOPTED, with changes]**,
 **[SUPERSEDED by X.Y]** or **[REJECTED]**. The authoritative description of the
 final design is in README.md.

#### 2.3.1 Domain Model and Immutability

**Prompt:** Propose a minimal domain model for the scoreboard. Evaluate the use of classes and records, the responsibilities of each component, and the benefits and trade-offs of an immutable domain model.

**Context:** The library manages multiple matches and their scores. The design should preserve encapsulation, avoid unnecessary abstractions and support thread-safe operations.

**Outcome:** I chose a minimal model:
- `Scoreboard` coordinates the operations, guards access and produces the summary.
- `Match` is an immutable class holding the match state; `withScore()` returns a new instance.
- `MatchId` (wraps a UUID), `Team` (validates and stores the name) and `MatchSummary` (returned by the public API) are immutable records.

The scoreboard keeps a private mutable `HashMap<MatchId, Match>` and replaces the stored instance on every change, so the collection is mutable but the domain objects are not. `Team` keeps the default `equals()`/`hashCode()` because teams are never used as keys; the home/away check uses `equalsIgnoreCase()` explicitly.

**Rejected:** a separate `Score` type (an extra abstraction without enough value) and conventional getters/setters.

**Status: [ADOPTED, with changes].** The final implementation replaced the immutable `Match` with a package-private, mutable `MatchContext` guarded by a per-match `ReentrantLock` (see 2.3.6), and replaced the `Team` record with a validated `String` (trimmed in `startMatch`, compared case-insensitively for the self-play check). `MatchId` and `MatchSummary` remain immutable records.
The reason for the change: with per-match locking, each match owns a lock and mutable scores that are updated in place under that lock; rebuilding an immutable `Match` on every update would allocate a new object per update without any concurrency benefit, because the lock already serializes access.

#### 2.3.2 Match Identification and Storage

**Prompt:** Evaluate how matches should be identified and stored. Compare team-based identifiers, composite keys and generated unique identifiers, considering multiple matches involving the same teams.

**Context:** The scoreboard must support multiple simultaneous matches; the same team may play several matches and the same two teams may meet more than once. No integration with external match providers is required.

**Outcome:** I chose a `MatchId` generated by `Scoreboard.startMatch()` with `UUID.randomUUID()`; clients use the returned ID in later operations. Matches are stored in a `HashMap<MatchId, Match>` (average O(1) lookup and insertion), and `startMatch()` must never overwrite an existing entry. Match identity is independent of team names.

**Rejected:** team-based IDs and composite keys (ambiguous for repeated pairings) and client-supplied IDs (relevant only with external providers, which are out of scope).

**Status: [ADOPTED].** `MatchId` is generated with `UUID.randomUUID()` in `startMatch`; storage is `ConcurrentHashMap<MatchId, MatchContext>` (see 2.3.6).

#### 2.3.3 Match Lifecycle and Finished Matches

**Prompt:** Define the match lifecycle and determine how score updates and finished matches should be handled in an immutable domain model.

**Context:** A match starts as ongoing, can be updated and is eventually finished. The assignment requires a summary of ongoing matches but does not say how finished matches are stored or how later operations behave.

**Follow-up prompt:** How would you solve this case: finished matches are never removed from the map, so memory grows over time?

**Outcome:** My first design kept finished matches in the map with a `FINISHED` status, which allowed clear errors on repeated operations but made memory grow without bound. I replaced it: `finishMatch` removes the match from the map and returns its final `MatchSummary`. Every later operation on that ID throws `MatchNotFoundException`. The map therefore holds only ongoing matches, there is no status enum and no lifecycle state machine, and the summary only sorts. Score updates use absolute values (so VAR corrections can lower a score) and reject negative values. Updates that arrive after the match is finished are rejected too; handling late events is the caller's responsibility (see 2.2.1).

**Rejected:** keeping finished matches with a status, expiry by TTL or size limit (needs a clock and configuration without a requirement), a set of finished IDs (still grows), a separate archive (history is out of scope, see 2.2.1), and accepting corrections after a match is finished (requires keeping finished matches).

**Status: [ADOPTED].** `finishMatch` removes the match from the map atomically; later operations throw `MatchNotFoundException`.

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

**Status: [ADOPTED, with changes].** All lifecycle operations return `MatchSummary`. Null arguments are rejected with `NullPointerException` (`Objects.requireNonNull`), not `IllegalArgumentException`; blank team names throw `InvalidTeamException` (see 2.3.4 discussion).

#### 2.3.5 Concurrency, Summary Ordering and Performance

**Prompt:** Evaluate how to ensure thread-safe access to the scoreboard and deterministic summary ordering. Compare synchronization and collection alternatives, and assess the performance implications of sorting matches when the summary is requested.

**Context:** In-memory, single JVM, possibly accessed by multiple threads. Scores change over time, which affects ordering. Unnecessary concurrency and performance optimizations should be avoided.

**Outcome:** A single shared `synchronized` monitor protects all public operations, so each operation is atomic and the summary is a consistent snapshot. The summary is built on request by sorting the matches on the scoreboard by total score descending. Ties are broken by a monotonically increasing `long` sequence number assigned in the synchronized `startMatch()`; it is internal and not exposed. Complexity: O(1) average lookup and insertion, O(n log n) sorting per summary.

**Rejected:** `ConcurrentHashMap`, `ReadWriteLock` and fine-grained locking (complexity not justified); a continuously ordered `TreeSet` (needs remove and re-insert on every score change); `PriorityQueue` (does not re-order after a score change and cannot be iterated in order); cached summaries (premature without evidence of a performance problem).

**SUPERSEDED by 2.3.6:**
- The single-monitor design was replaced by   `ConcurrentHashMap` with per-match `ReentrantLock` after the architectural pivot. Kept as a record of the earlier design.

#### 2.3.6 Simplification and Architectural Pivot

**Prompt:** Evaluate the transition from an over-engineered multi-source, delayed-update model to a streamlined, rock-solid core tailored strictly for the Sportradar standard assessment. Assess the shift from a globally synchronized `HashMap` to `ConcurrentHashMap` with per-match locking.

**Context:** Initial designs explored advanced features like multi-source priority merging (VAR vs. official API), out-of-order grace periods, and post-match retention windows. While robust for distributed data streaming, these add unnecessary complexity, background execution, and state management overhead for a single-JVM in-memory scoreboard library.

Based on the transition from Part I to Part II, please evaluate this simplified architecture. Does it form a clean, thread-safe, and idiomatic Java 21 foundation, and are there any edge cases (such as race conditions between `finishMatch` and `updateScore`, or how `matchId` / team uniqueness is handled) that I should refine before writing the code?

**Outcome:** I decided to store matches in a ConcurrentHashMap, with each MatchContext owning a dedicated ReentrantLock. The change from the previously considered HashMap follows from adopting a different synchronization model: instead of using a single global lock to protect all operations, I chose per-match locking. This allows operations on different matches to execute concurrently.

- **HashMap** would be suitable if a single global lock, such as synchronized, protected all operations on the map. In that design, both collection access and match state modifications would be protected by the same monitor. However, after switching to per-match locks, this protection is no longer sufficient. A lock associated with an individual MatchContext does not protect the shared HashMap structure against concurrent insertions, removals or lookups. All map operations would therefore still require an additional global lock, limiting the benefits of per-match synchronization.

- **ConcurrentHashMap** addresses this issue by providing thread-safe concurrent access to the collection and allowing operations on different entries to proceed concurrently. However, it does not make compound operations atomic or protect the state of an individual match. Therefore, the ReentrantLock in each MatchContext is still necessary to serialize operations on the same match, such as updateScore() and finishMatch(). An additional identity check ensures that the map still references the same MatchContext, preventing operations from being performed on a match that has already been removed.

- **getSummary()** builds consistent snapshots of individual matches on demand and sorts them by total score in descending order. Ties are broken by a monotonically increasing long sequence number assigned during startMatch(). The sequence number is internal and is not exposed to the client. The summary is not an atomic snapshot of the entire scoreboard, as individual matches may be read at different points in time.

**Rejected:** 
- **HashMap with a single global lock** — simpler, but prevents operations on different matches from executing concurrently.
- **ReadWriteLock** — additional complexity is not justified by the adopted per-match locking model.
- **A single global lock for all operations** — would provide atomic operations and a globally consistent snapshot, but would limit concurrency.
- **A continuously ordered TreeSet** — requires removing and reinserting a match after every score change.
- **PriorityQueue** — does not automatically reorder elements after a score change and does not guarantee iteration in priority order.
- **Cached summaries** — premature optimization without evidence of a performance problem.
  Complexity: O(1) average lookup and insertion, O(n) snapshot collection, and O(n log n) sorting per summary.


### 2.4 Implementation

#### 2.4.1 Base Domain Model and TDD Stubs Setup

**Prompt:** Define the minimal base domain structures, value objects, records, stub facade, and exception hierarchy required to establish the "red phase" of TDD for the World Cup Scoreboard library.

**Context:** Following the architectural pivot and the adoption of neutral team naming (`team1`/`team2`) and internal `UUID`-based identification, the project requires a clean structural foundation. To practice strict Test-Driven Development (TDD), the initial API facade methods must throw `UnsupportedOperationException`, allowing unit tests to compile and fail as expected.

**Outcome:** Established the core package structure and foundational types:
- **`MatchId` Value Object:** A record wrapping `java.util.UUID` with a compact constructor for null-safety validation, a `random()` factory method for internal ID generation, a `fromString()` parsing helper, and an overridden `toString()` returning a clean raw UUID string for readable logging.
- **`MatchSummary` Record:** An immutable data transfer object holding the state of a match (`id`, `team1`, `team2`, `team1Score`, `team2Score`) returned by the scoreboard API.
- **`Scoreboard` Facade Stubs:** The main entry point exposing public operations (`startMatch`, `updateScore`, `finishMatch`, `getSummary`), initially implemented as stubs throwing `UnsupportedOperationException`.
- **Domain Exception Hierarchy:** A dedicated unchecked exception structure rooted in `ScoreboardException`, containing specific runtime domain errors: `MatchNotFoundException`, `InvalidScoreException`, and `InvalidTeamException`.

**Rejected:** Using raw `String` primitives for identifiers (avoiding primitive obsession) and implementing full business logic before writing unit tests, in order to strictly adhere to the TDD methodology.

#### 2.4.2 Unit Tests for Core Lifecycle Operations

**Prompt:** Design and implement a comprehensive suite of unit tests using JUnit 5 and AssertJ to verify the core scoreboard operations (`startMatch`, `updateScore`, `finishMatch`), covering both happy paths and domain exception handling.

**Context:** With the base domain structures and stubbed facade in place (following TDD red phase), we need rigorous unit tests to drive the implementation of business logic and ensure thread-safe behavior, validation rules, and proper lifecycle management.

**Outcome:** Created `ScoreboardTest` covering:
- **Happy Paths:** Successful match initialization, absolute score updates (including downward corrections), and immediate removal upon finishing.
- **Validation Rules:** Rejection of null/blank team names, self-playing teams (case-insensitive), and negative scores.
- **Error Handling:** Verification that operations on non-existent or already finished match IDs correctly throw `MatchNotFoundException`.

#### 2.4.3 Maven Project Configuration and Build Setup

**Prompt:** Define and configure the project’s `pom.xml` for Java 21, ensuring zero external production dependencies while providing a robust testing and code-quality toolchain (JUnit 5, AssertJ, Maven Surefire, and JaCoCo coverage enforcement).

**Context:** The library must run on pure Java 21 with no external production overhead. However, rigorous testing requires modern assertion libraries and code coverage gates to satisfy enterprise coding standards.

**Outcome:** Configured the Maven build file with:
- **Java 21 Release Flag:** `<maven.compiler.release>21</maven.compiler.release>` ensuring strict language compliance.
- **Testing Toolchain:** Integrated JUnit 5 (`junit-bom` / `junit-jupiter`), `junit-platform-launcher` for Surefire test discovery, and AssertJ (`assertj-core`) for fluent assertions.
- **JPMS Support:** Configured `maven-jar-plugin` to include the `Automatic-Module-Name` manifest entry (`com.sportradar.worldcup`).
- **Quality Gate:** Integrated `jacoco-maven-plugin` configured to run unit tests and enforce a strict minimum line coverage threshold of 80% during the verify phase.

**Rejected:** Adding external runtime dependencies (such as Lombok or Spring) to keep the core library lightweight, isolated, and strictly single-JVM in-memory.

#### 2.4.4 Edge Cases Unit and Concurrency Tests

**Prompt:** Design and implement comprehensive unit and concurrency tests to verify critical architecture-driven edge cases: thread races between `finishMatch` and `updateScore`, strict whitespace trimming and case-insensitive self-play validation, `MatchId` collision safety via `putIfAbsent`, and accurate downward score corrections (VAR scenario).

**Context:** Having established baseline lifecycle tests, the library must be stress-tested against concurrency hazards, edge formatting issues, and complex domain state transitions to guarantee enterprise robustness.

**Outcome:** Created `ScoreboardTest`, `ScoreboardConcurrencyTest` and `ScoreboardRaceConditionTest` covering:
- **Concurrency / Race Conditions:** Using `CountDownLatch` and multi-threading (`ExecutorService`) to simulate simultaneous `finishMatch` and `updateScore` execution on the same match ID, proving safety against dead/removed matches.
- **Whitespace and Normalization:** Verifying that teams passed with leading/trailing spaces are correctly trimmed, and matching rules properly reject self-play regardless of casing or padding.
- **Collision Resilience:** Ensuring atomic insertion logic prevents ID overwrite.
- **Downward Corrections:** Verifying absolute score updates handle score reductions correctly.
- **Primary Sorting (Total Score):** Ensuring matches with a higher sum of goals for `team1` and `team2` appear first in the summary.
- **Secondary Sorting (Chronological Tie-Breaker):** Ensuring that in the event of a score tie, the match that started *most recently* appears above older matches.

#### 2.4.5 Core Business Logic and Concurrency Implementation

***Prompt:** Implement the thread-safe `Scoreboard` facade and internal `MatchContext` domain model based on strict domain-driven design, enforcing CQS, atomic map operations (`ConcurrentHashMap` & `putIfAbsent`), fine-grained per-match locking (`ReentrantLock`) with strict identity checks and conditional removal, input sanitization (whitespace trimming, case-insensitive self-play check), absolute score updates supporting VAR corrections, and dual-criteria summary ranking using `MatchSummary` as the source of truth with its authoritative `sequenceNumber`.

**Context:** Following the red phase of TDD and test suite establishment, the system requires production code that fulfills all functional and non-functional requirements without external dependencies, guaranteeing thread safety under high concurrency and accurate sorting semantics.

**Outcome:** Implemented:
- `MatchContext`: Encapsulates match state, scores, an internal sequenceNumber and a dedicated ReentrantLock.
- `Scoreboard`: Provides the public API and coordinates operations on the match registry:
  - `startMatch`: Validates and trims team names, ensures teams do not play against themselves (case-insensitively), assigns a monotonic sequence number, and safely registers matches using `putIfAbsent`.
  - `updateScore`: Validates non-negative scores, acquires the match lock, verifies exact instance identity post-lock (`matches.get(matchId) != context`), and updates absolute scores (supporting VAR corrections).
  - `finishMatch`: Safely removes the match from the registry under lock protection using atomic conditional removal (`matches.remove(matchId, context)`), preventing race conditions with concurrent updates.
  - `getSummary`: Streams active matches, mapping them to `MatchSummary` and sorting them primarily by total score descending, and secondarily by `sequenceNumber` descending (newest first).

**Status: [ADOPTED, with changes].** The prompt above still treats `sequenceNumber` as part of `MatchSummary`, and the first implementation did the same: the public record carried the sequence and `getSummary` sorted on that field. I dropped that later (see 2.6 and 3.1 item 1). The tie-break is an internal detail, not something a client should see or depend on, so the field does not belong in the object returned by the API. The sequence now lives only on `MatchContext`. For sorting, `toSnapshot()` builds a package-private `Snapshot` that pairs the public `MatchSummary` with that sequence; the list returned to the client is still only `MatchSummary`. Ordering is unchanged: total score descending, then the highest sequence first.

#### 2.4.6 Exception Handling and Validation Unit Tests

**Prompt:** Design and implement comprehensive exception handling and validation tests for the `Scoreboard` facade, verifying robust error management against edge cases such as null/blank team names, self-play validation (case-insensitive and whitespace-trimmed), negative score inputs, null match IDs, and attempts to update or finish non-existent or already terminated matches.

**Context:** Following core implementation, the library must guarantee defensive programming and precise exception throwing (`InvalidTeamException`, `InvalidScoreException`, `MatchNotFoundException`, and `IllegalArgumentException`) to maintain domain invariants and safe failure modes under invalid client input or concurrency races.

**Outcome:** Created `ScoreboardTest` covering:
- **Team Validation & Self-Play:** Ensuring null, empty, or whitespace-only team names are rejected, and teams cannot play against themselves regardless of casing or padding.
- **Score Validation:** Ensuring negative scores throw `InvalidScoreException`.
- **Match Lifecycle Faults:** Verifying that updating or finishing a null `MatchId`, a non-existent match, or a match that has already been finished throws `MatchNotFoundException` or `IllegalArgumentException`. Later changed to Objects.requireNonNull (NPE) – see 3.1.

#### 2.4.7 API Encapsulation and Access Modifier Refinement

**Prompt:** Refine the library's access modifiers to enforce strict encapsulation, ensuring that internal state models (`MatchContext`) remain package-private while defining necessary public data contracts (`MatchId` and `MatchSummary`) required by external client code to interact with the `Scoreboard` facade.

**Context:** While internal synchronization and mutable states must be hidden from consumers, a usable library API requires public value objects for method parameters (`MatchId` returned by `startMatch` and required by `updateScore`/`finishMatch`) and return types (`MatchSummary` returned by `getSummary`).

**Outcome:** Reviewed and adjusted visibility:
- **`MatchContext`:** Kept strictly package-private (`class MatchContext`) as an internal domain model.
- **`MatchId` & `MatchSummary`:** Maintained as `public` records because external clients outside the package must reference match handles and read summary data objects.
- **Exceptions:** Confirmed all domain exceptions are `public` for proper client-side error handling.

#### 2.4.8 Additional Query Operation

**Prompt:** The assignment requires exactly one additional operation beyond the four core ones. Propose a single operation that is useful for a live data feed consumer, fits the existing API and does not duplicate `getSummary`.
Evaluate `getMatch(MatchId)` and `getMatchesByTeam(String team)`.

**Context:** `getSummary` already returns all ongoing matches sorted by total score. A client that wants to render a single widget needs either one match or all matches for one team. The two candidate operations overlap: a single match can be found by filtering `getMatchesByTeam` results.

**Outcome:** I chose `getMatchesByTeam(String team)`: 
- It covers both "all matches for a team" and "find a match by ID" (via a stream filter over the result).
- It is a single public method, so it satisfies "exactly one additional operation" without ambiguity.
- Team names are normalized the same way as in `startMatch` (trimmed, case-insensitive, `Locale.ROOT`), so the API is consistent.
- Ordering reuses the same `SUMMARY_ORDER` comparator as `getSummary`, so the two methods never disagree on order.

**Rejected:**
- **`getMatch(MatchId)` as a second public method:** would be a second operation, which the assignment does not allow; the stream filter covers the use case.
- **`getMatches(Optional<MatchId>, Optional<String>)` with optional filters:**  Optional in parameters is an anti-pattern; two clear overloads would be two operations in disguise.
- **`reset()` / `getTopScoringMatches(int)`:** either outside the library's scope or a duplicate of `getSummary`.
- **`applyUpdate(MatchUpdate)` with idempotency:** interesting but a larger change (new public type, per-source sequence tracking, extra concurrency tests); documented as out of scope in the README.


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

**Context:** In-memory Java 21 Maven project with immutable domain objects, a private `HashMap`, internally generated match IDs and a shared `synchronized` monitor. Context written before the per-match locking pivot; see 2.3.6.

**Outcome:** The strategy covers the eight scenario groups above. Decisions:
- JUnit Jupiter and the Java 21 concurrency utilities only, no extra test dependencies.
- Concurrent tests use `ExecutorService` and `CountDownLatch` for a simultaneous start, with timeouts so that a failing test cannot hang.
- Tests assert guarantees (unique IDs, exactly one successful finish, consistent snapshots) and do not assume a thread scheduling order.
- Tests check observable API behavior, not internals.
- After a match is finished, updating or finishing it again is expected to fail with `MatchNotFoundException`; in concurrent finish attempts exactly one call succeeds and the others get `MatchNotFoundException`.
- Prompt items 3 and 5 were written before the decision to remove finished matches (see 2.3.3); in the final design they become "MatchNotFoundException on a finished match" and "a finished match is no longer in the summary".

#### 2.5.2 Test Hardening and Flakiness Fixes

**Prompt:** The first full `mvn clean verify` run failed on several tests. Help diagnose and fix:
1. `ScoreboardTest.shouldOrderSummaryCorrectly` – asserts `MatchId` against `MatchSummary`, and the comment "matches started sequentially with time gaps" suggests reliance on wall-clock timing even though the tie-breaker is `sequenceNumber`.
2. `ScoreboardConcurrencyTest.summaryReadDuringConcurrentUpdatesIsAlwaysConsistent` – fails intermittently with "readers never overlapped with writers".
3. `ScoreboardConcurrencyTest.updateRacingWithFinishHasOnlyTwoValidOutcomes` – a local `Scoreboard scoreboard = new Scoreboard()` shadows the field.
4. `ScoreboardTest` – a large commented-out block of duplicate tests remains at the bottom of the file.

**Context:** Implementation complete; tests green except the above. Local machine: M5 with 18 cores, but CI runs on 2–4 cores. In the concurrency test, the write loop runs 50 iterations per writer with only one `Thread.yield()` at the very end (`score % 50 == 0`), while the final assertion expects `team1Score == 500` – a deterministic mismatch independent of any race.

**Outcome:**
- **`shouldOrderSummaryCorrectly`:** replaced `MatchSummary matchN` with `MatchId matchN = scoreboard.startMatch(...).id()` so `isEqualTo(matchN)` compares `MatchId` to `MatchId`; removed the "time gaps" comment and replaced it with an explicit note that ordering depends on `sequenceNumber`, not the wall clock.
- **`summaryReadDuringConcurrentUpdatesIsAlwaysConsistent`:**
  - fixed the assertion/loop mismatch (`== 500` vs. `<= 50`) by changing the loop to `score <= 500`;
  - moved `Thread.yield()` from firing once at `score == 50` to firing every 50 iterations (10 times per writer);
  - re-ran the test 20 times without a single failure (previously flaky on 18 cores, deterministic failure on 2-core CI due to the 500 vs. 50 mismatch).
  - the `Thread.sleep` overlap window from this pass was replaced later: the final test uses two `CountDownLatch`es instead of sleeping (see the follow-up review in 2.6). `readersStarted` blocks the writers until every reader has taken its first snapshot, so readers are already running when updates begin. `writingStarted` is counted down by the first applied update, and a snapshot counts as overlap only if writing had started before it and had not finished after it. That keeps `snapshotsTakenWhileWriting > 0` from passing on the pre-write snapshot.
- **`ScoreboardTest`:** removed the commented-out block (it duplicated active tests: invalid team names, self-play, null `MatchId`, negative scores, missing/finished matches). Coverage from `ScoreboardTest` and `ScoreboardConcurrencyTest` is unaffected.

**Rejected:** replacing `CountDownLatch` with `Phaser` to force true phase synchronization between readers and writers. It would be fully deterministic but harder to read; the `sleep`-based overlap window is sufficient for a task assignment, and `assertTrue(snapshotsTakenWhileWriting > 0)` already fails loudly if overlap does not occur.

#### 2.5.3 Defensive Branch Coverage and Dependency Security

**Prompt:** Two follow-ups after the first green build:
1. JaCoCo reports missed branches in `updateScore` and `finishMatch` for the `"Match not found or already finished with ID"` paths. These branches are reachable only when the entry is removed between `matches.get(matchId)` and the subsequent `lock()`, which cannot be triggered from a single thread.
2. Dependabot flags `assertj-core` 3.27.3 for CVE-2026-24400 (XXE in `isXmlEqualTo` / `xmlPrettyFormat`, fixed in 3.27.7). The library does not use those methods and AssertJ is test-scoped, but the scanner still fails.

**Context:** Implementation complete; `mvn clean verify` green; 80% line coverage threshold in place. The defensive branches in `Scoreboard` are intentionally unreachable in normal flow, but the coverage report still lists them as missed.

**Outcome:**
- **Refactor for testability:** extracted `getMatchContext(MatchId)` and `removeMatchContext(MatchId, MatchContext)` in `Scoreboard` as package-private methods. `updateScore` and `finishMatch` now call these helpers, so the
  defensive branches can be exercised from a test without changing the public API or adding test-only fields.
- **`ScoreboardRaceConditionTest`:** covers both defensive branches by overriding the helpers in an anonymous `Scoreboard` subclass:
  - the first test makes the second `getMatchContext` call remove the entry (simulating a concurrent `finishMatch` that won the race between `get` and `lock`), which triggers `matches.get(matchId) != context`;
  - the second test overrides `removeMatchContext` to remove the entry and return `false`, which triggers `if (!removed)`.
- **Null `MatchId` tests:** added `updateScore(null, ...)` and `finishMatch(null)` tests to cover the `Objects.requireNonNull` branches; without them the branch coverage for these two methods stayed at 75%.
- **`assertj-core` bumped to 3.27.7** in `pom.xml` to address CVE-2026-24400. The library never calls `isXmlEqualTo` or `xmlPrettyFormat`, but the bump removes the scanner warning and keeps the build clean.

**Remaining uncovered branch:** `startMatch` still has one missed branch – the `putIfAbsent` collision path in the `do { ... } while (matches.putIfAbsent(...) != null)` loop. The probability of a `UUID.randomUUID()` collision is negligible (~2^-122), so the branch is left uncovered and documented in the README under Trade-offs, rather than mocked via a `MatchId.random()` override.

**Rejected:**
- **Reflection-based tests:** a first attempt manipulated the private `matches` map via reflection. It removed the entry *before* `getMatchContext`, so the code took the `context == null` branch and threw `"Match not found with ID"` instead of `"Match not found or already finished"`. Replaced with the anonymous-subclass approach.
- **Test-only hook field in `Scoreboard`:** adding a `Runnable beforeLockHook` to production code was rejected – it leaks test concerns into the API.
- **Excluding `Scoreboard` from JaCoCo:** would hide real coverage of `updateScore`/`finishMatch`; not worth it for a single defensive branch.
- **Mocking `MatchId.random()`:** would require Mockito (new dependency) and would not test the real `startMatch` method.

**Status: [ADOPTED, with changes].** The `assertj-core` bump and the null `MatchId` tests were adopted unchanged. The testability refactor (`getMatchContext` / `removeMatchContext` and the anonymous `Scoreboard` subclass) was **[SUPERSEDED by 2.6]**: the helpers were removed from `Scoreboard`, so the production code has no test-only seams. A subsequent `MatchStatus` lifecycle change replaced the identity check in `updateScore` with an in-progress status check under the match lock; this also handles operations that were waiting when `finishMatch` marked the match finished. The reflection-based registry manipulation was removed, and update/finish race outcomes are exercised through the public API in `ScoreboardConcurrencyTest`.

Results are verified with `mvn clean verify`.

### 2.6 AI Code Review and Refactoring

**Prompt:** Review the implementation of `Scoreboard`, `MatchContext` and `MatchSummary` for thread-safety, correctness and API consistency with the README. Point out races, overflow risks and documentation drift.

**Context:** Implementation complete; tests green.

**Outcome:** AI flagged:
- `(long)` cast missing in total-score comparison (int overflow).
- `toSummary()` reading scores without lock (torn read).
- `Collectors.toList()` returning a mutable list.
- `sequenceNumber` still exposed in `MatchSummary`, contrary to README.
- `MatchContext` as a private inner class – extract to top-level.

**Rejected:** AI suggested adding `equals`/`hashCode` to `MatchContext`; rejected – identity is by `MatchId`, and `MatchContext` never leaves the library.

### Code review of the finished project

**Prompt:** Please perform a code review of the attached project as a senior Java reviewer. It is a solution to the coding exercise "Live Football World Cup Scoreboard" (Java 21, Maven library, no production dependencies).

Assignment requirements:
- Operations: start a match, update the score, finish a match, get a summary of matches in progress
  (ordered by total score descending, ties: most recently started first), plus exactly ONE additional operation
  (mine: getMatchesByTeam) introduced in a distinct commit and explained in the README.
- Deliverables: Maven project, README.md (assumptions, reasoning, trade-offs), AI.md (AI usage, prompt history).
- Example scenario: Mexico 0-5 Canada, Spain 10-2 Brazil, Germany 2-2 France, Uruguay 6-6 Italy, Argentina 3-1 Australia
  -> Uruguay, Spain, Mexico, Argentina, Germany.

Design in brief: ConcurrentHashMap<MatchId, MatchContext>, per-match ReentrantLock, identity check under the lock,
immutable records MatchId/MatchSummary, library-generated UUID ids, finished matches are removed,
getSummary is weakly consistent across matches, multi-source/duplicate-update handling is out of scope.

**Context:** Already fixed since the previous review (please verify, do not just trust):
- JUnit/JaCoCo versions aligned between pom.xml, README and AI.md; fixed the #5-design-decisions anchor.
- Removed getMatch(...) from the README usage (only one additional operation is allowed).
- Removed the AI.md placeholder, the duplicated "- -" bullet and the duplicated UUID-collision trade-off.
- Trade-off now refers to ConcurrentHashMap (O(1) average lookups, O(n log n) sorting per summary).
- ScoreboardException has a (String, Throwable) constructor; added serialVersionUID to all exceptions.
- Renamed validateTeamNameAndTrimmed; getMatchesByTeam now uses equalsIgnoreCase like startMatch.
- Removed a redundant test and a Polish comment; removed the unused MatchAlreadyExistsException.
- ScoreboardRaceConditionTest rewritten: it holds the match lock, waits for the worker to queue on it, removes
  the entry and releases the lock, so the "already finished" defensive branches are covered deterministically.
- Added tests: assignment example scenario, empty summary, immutable getSummary list, snapshot-not-live-view,
  Integer.MAX_VALUE scores (long total), returned summaries of updateScore/finishMatch, MatchId.toString.
- Added English comments to MatchContext and class-level Javadoc to Scoreboard (thread-safety), MatchSummary, MatchId.

Please review and report by priority (must fix / should fix / optional):
1. Correctness and thread-safety (races, deadlocks, visibility, ordering guarantees).
2. Compliance with the assignment and consistency between code, tests, README and AI.md.
3. Test quality: coverage of requirements and edge cases, flakiness, duplicated or misleading tests.
4. API design, encapsulation, naming, Javadoc, exception design.
5. pom.xml and build configuration.

For every finding give file, location, reason and a concrete suggested fix. Do not change logic yourself;
show proposed changes and the reason first. Do not report style preferences as defects.

**Outcome:**
The review confirmed the core design (per-match lock, immutable records, deadlock-free locking,
`finishMatch` returning a consistent final summary) and found problems mostly in tests and documentation.

Applied after my own evaluation:
- README: removed `getMatch(...)` from the usage example (the assignment allows exactly one additional
  operation, and `getMatch` was my first idea), fixed the broken anchors, typos and duplicated trade-off bullets. Changed the
  `HashMap` trade-off to `ConcurrentHashMap` (O(1) average lookups, O(n log n) sorting per summary).
- Aligned the JUnit and JaCoCo versions between `pom.xml`, README and AI.md; removed the AI.md placeholder.
- `ScoreboardRaceConditionTest` did not cover the branches it claimed to: it removed the match before
  the call, so only the plain "Match not found" check fired. Rewritten deterministically: the test holds the
  match lock, waits until a worker thread is queued on it, removes the entry and releases the lock, so the
  "already finished" defensive branches are covered and asserted by message.
- Added tests: the example scenario from the assignment, empty summary, immutable `getSummary` list,
  snapshot-not-live-view, `Integer.MAX_VALUE` scores (total is `long`), returned summaries of
  `updateScore` / `finishMatch`, `MatchId.toString`.
- Added comments to `MatchContext` and class-level Javadoc to `Scoreboard` (including thread-safety),
  `MatchSummary` and `MatchId`.
- Removed the unused `MatchAlreadyExistsException`: ids are generated by the library and a UUID collision is
  handled by the retry loop, so the exception could never be thrown.
- `getMatchesByTeam` now uses `equalsIgnoreCase`, consistent with `startMatch`; renamed
  `validateTeamNameAndTrimmed`; `ScoreboardException` got a `(String, Throwable)` constructor;
  removed a redundant test and a Polish comment.

Evaluated and decided:
- `getSummary` / `getMatchesByTeam` stay weakly consistent across matches. Making the snapshot atomic would
  need a global read/write lock or copy-on-write state, which would stop or serialize updates, while matches
  are independent and a scoreboard is stale at display time anyway. Documented in README and Javadoc.
- `serialVersionUID` added to the exceptions as library hygiene (exceptions are `Serializable`);
  nothing in this project serializes them, so the practical benefit is small. I kept it because it is harmless and avoids lint warnings.

Not applied: making the summary an atomic snapshot (see the decision above). The optional items of this
review (the possibly flaky `snapshotsTakenWhileWriting > 0` assertion, micro-optimisations in
`getMatchesByTeam`, small style points in `Scoreboard`) were handled in the follow-up review below.

### Follow-up code review

**Prompt:** I applied your fixes. Do the code review again and tell me whether I missed anything.

**Context:** Updated code, tests, README, AI.md and `pom.xml`, compared again with the assignment and with the previous review.

**Outcome:** The second pass found no correctness or thread-safety problems in the production code; the findings were in documentation, tests and build settings.

Applied after my own evaluation:
- `AI.md`: filled the open placeholders of the previous entry, marked the race-test refactor in 2.5.3 and items 11-12 of 3.1 as superseded, replaced the unverified ChatGPT version in section 1, added the missing 2.4.8 entry to the table of contents and confirmed that 2.3.1 already carries a status line.
- `MatchIdTest`: added `@DisplayName` to every test.
- `Scoreboard.getMatchesByTeam`: added Javadoc, and it now filters by the (final) team names before taking snapshots, so matches of other teams are never locked.
- `Scoreboard`: the class is `final`, the `validate*` helpers are `static` and the comparator constant is declared before the instance fields.
- Flaky assertion in `summaryReadDuringConcurrentUpdatesIsAlwaysConsistent`: readers now take their first snapshot before the writers start (`readersStarted` latch). The reviewer suggested only that latch, but then the counter `snapshotsTakenWhileWriting` would already be true before any write happened, so I added a second latch (`writingStarted`) and count a snapshot only if writing had started before it and was not finished after it.
- New test: the same teams may play several ongoing matches, each with its own id (an assumption from the README that was only covered indirectly).
- `pom.xml`: `-Xlint:all` in `maven-compiler-plugin`, so compiler warnings (for example about `serial`) are visible.

### 2.7 Documentation Review

**Prompt:** Check the simplicity and correctness of the README and AI.md for a senior Java developer audience, whether they are over-engineered, and whether anything is missing.

**Context:** README and AI.md drafts written before the implementation.

**Outcome:** I applied the suggestions to the README: fixed the links, removed duplicated points, split "Reasoning" into build and design decisions, and added the missing assumptions (0-0 start, home and away teams must differ) and the ordering rule. I also added a trade-off for allowing a team in several simultaneous matches, and postponed the usage example until the API exists. In AI.md I normalized the headings, shortened the outcomes to decisions and rejected alternatives, and marked unfinished sections explicitly. I also replaced keeping finished matches in memory with removal on finish and documented how late updates are handled (see 2.3.3).

## 3. Human Review and Validation

All AI suggestions were reviewed before being incorporated. Suggestions I changed or corrected so far:
- Maven Wrapper entries removed (2.1.1).
- JaCoCo check and maven-jar-plugin restored (2.1.4).
- Initial design kept finished matches in the map with a status; replaced by removal on finish (2.3.3).

### 3.1 Post-review changes
1. Removed `sequenceNumber` from `MatchSummary`
2. Replaced `Thread.sleep`-based ordering test with a deterministic sequence-based one.
3. Chage updateScore() and finishMatch() to return statement instead of void.
4. Add lock() to snapshot in toSummary().
5. Change getSummary() to immutable.
6. Remove recuration from startMatch().
7. Replace manual null checks to Objects.requireNonNull().
8. Refactor team name validation to return trimmed strings directly.
9. Change random() to package-private.
10. `MatchId.fromString` wraps `IllegalArgumentException` in `InvalidMatchIdException`.
11. Extracted `getMatchContext` / `removeMatchContext` in `Scoreboard` to make
    the defensive "not found or already finished" branches testable. Removed again later
    (see 2.5.3 and 2.6): the production code has no test-only seams.
12. Added `ScoreboardRaceConditionTest` and null `MatchId` tests for `updateScore` and `finishMatch`.
    The first version used an anonymous `Scoreboard` subclass; the final version holds the match lock and
    removes the entry while a worker thread is queued on it (see 2.5.3).
13. Bumped `assertj-core` to 3.27.7 to address CVE-2026-24400.
14. Added `getMatchesByTeam(String team)` as the single additional operation;
    README explains why it covers both "by team" and "by ID" use cases and why `getMatch(MatchId)` was not added as a second public method.
15. Removed the unused `MatchAlreadyExistsException`.
16. `getMatchesByTeam` filters by team name before locking and snapshotting matches; added its Javadoc.
17. `Scoreboard` made `final`; `validate*` helpers made `static`; comparator constant moved above the instance fields.
18. Hardened the snapshot-overlap check in `summaryReadDuringConcurrentUpdatesIsAlwaysConsistent` with two latches.
19. Added a test for the same teams in several ongoing matches and `@DisplayName` for all `MatchIdTest` tests.
20. Added `-Xlint:all` to `maven-compiler-plugin`.

Verification:
- Verified with `mvn clean verify`: all tests pass, JaCoCo ≥ 80%.
- `jar --describe-module` for the module name.
- `mvn dependency:tree | grep assertj` shows `assertj-core:3.27.7:test`.

### 3.2 Review Follow-up (2026-11-24)

**Prompt:** Review concurrency, thread safety, Java Memory Model, API design and test coverage without modifying files.

**Outcome:** No production-code race or Java Memory Model defect was identified. The review found that concurrent query behavior for `getMatchesByTeam` was not covered and that the README incorrectly described the null-team exception.

**Applied:** Updated the README to state that null team names throw `NullPointerException`, while blank names throw `InvalidTeamException`, matching `Objects.requireNonNull` and existing tests. Clarified that both query methods return per-match consistent but non-atomic snapshots. Added a concurrency test for `getMatchesByTeam` during concurrent score updates; it checks result membership, ordering, and per-match score consistency.
