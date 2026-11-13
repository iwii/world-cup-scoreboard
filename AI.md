# AI Usage Documentation

## 1. AI Tool Usage Summary

AI (Claude) was used as a supporting tool for:
- Development environment and build setup (.gitignore, pom.xml, Maven in IntelliJ)
- Requirements analysis
- Design and architectural discussions
- Code generation and implementation suggestions
- Unit test design
- Code review and refactoring suggestions
- Documentation (README.md, AI.md)

Prompts are listed in English translation. All generated suggestions were reviewed and validated before being incorporated into the project.

## 2. Prompt History

### 2.1 Development Environment Setup

#### 2.1.1 gitignore
**Prompt:** What should be included in the .gitignore file for a Java Maven project developed using IntelliJ IDEA on macOS?

**Context:** The project is a Java library developed using Maven. IntelliJ IDEA is used as the IDE, and macOS is the development operating system.

**Outcome:** I used the proposed .gitignore for IntelliJ IDEA, Maven, compiled artifacts, logs and macOS files. I removed all Maven Wrapper entries because I build with my own Maven installation, and I kept the plugin-specific entries (release, versions, shade, buildnumber) as a harmless precaution.

#### 2.1.2 pom.xml template
**Prompt:** Prepare pom.xml template with the newest artifact for JUnit asserts, code coverage.

**Context:** Java 21, Maven. Versions were checked online: JUnit 6.1.2 (latest stable at the time) and JaCoCo 0.8.15 (newest version found). Versions of maven-compiler-plugin, maven-surefire-plugin and maven-jar-plugin come from model knowledge and were not verified.

**Outcome:** I used the proposed pom.xml as the base:
- Java 21 via maven.compiler.release and UTF-8 encoding
- JUnit 6.1.2 through junit-bom and the junit-jupiter aggregator (its built-in assertions are enough), and JaCoCo 0.8.15 with a report and an 80% line coverage check in the verify phase.
- maven-compiler-plugin, maven-surefire-plugin and maven-jar-plugin with versions pinned in properties
- left junit-platform-launcher commented out until `mvn clean verify` shows whether it is needed.

- The plugin versions were not verified online, so I will check them with `mvn versions:display-plugin-updates`.

#### 2.1.3 Using Homebrew Maven in IntelliJ
**Prompt:** How to check whether IntelliJ uses Maven installed via Homebrew, and how to set it.

**Context:** IntelliJ offered only Bundled Maven (3.9.16) and the Maven Wrapper in the Maven home path list, while the terminal used Maven 3.10.0.

**Outcome:** IntelliJ offered only the bundled Maven and the Wrapper, so I set the Maven home path manually to the Homebrew installation (libexec directory). The IDE now builds with the same Maven version (3.10.0) as the terminal.

#### 2.1.4 Trimming the configuration

**Prompt:** Why were the JaCoCo check execution and maven-jar-plugin removed from the slimmed-down pom.xml?

**Context:** A slimmed-down pom.xml was proposed after the decision not to use the Maven Wrapper. The check execution and maven-jar-plugin were dropped on the assumption they were optional.

**Outcome:** The slimmed-down pom.xml had dropped the JaCoCo check and maven-jar-plugin without my decision, so I restored both: the check enforces the 80% coverage threshold in `mvn verify`, and maven-jar-plugin keeps the plugin version pinned for reproducible builds.

#### 2.1.5 Automatic-Module-Name

**Prompt:** How is the JAR name determined in a Maven project, and how can I make the module name stable when using maven-jar-plugin?

**Context:** The library has no module-info.java and may be consumed by others. Without the manifest entry, the module name is derived from the JAR file name (artifactId and version), so it can change between versions.

**Outcome:** I chose Automatic-Module-Name in the JAR manifest (via maven-jar-plugin) instead of a full module-info.java, because it is simpler and enough for a small library. I replaced the placeholder from the template with `com.sportradar.worldcup.scoreboard` to match the main package, and I check the result with `jar --describe-module`.

#### 2.1.6 README.md

**Prompt:** Prepare a README file with assumptions, reasoning and trade-offs now.

**Context:** The project requires a README describing assumptions, reasoning and trade-offs. The implementation has not started yet, but the build setup decisions (Java 21, Maven without Wrapper, JUnit, JaCoCo, Automatic-Module-Name) are already made.

**Outcome:** I created a README skeleton with Assumptions, Reasoning and Trade-offs, pre-filled with the build decisions already made (no runtime dependencies, JUnit, JaCoCo threshold, Automatic-Module-Name, no Wrapper). I will fill in the domain assumptions, API reasoning and implementation trade-offs after the implementation.

The proposed configuration was reviewed and adapted to the project requirements.

### 2.2 Requirements Analysis

#### 2.2.1 Understanding Real-world Sports Data Systems
**Prompt:** I am working on a Java library that manages a live Football World Cup scoreboard. Before designing it, I want to understand how live football data is handled in real-world sports data platforms, such as those serving sports betting services and live score applications. Please explain:
- What are the typical sources of live match data, and can several independent providers report the same match?
- How are events such as goals, red cards and match status changes communicated (push, polling, event streams)?
- How do systems ensure that the displayed information is accurate and up to date?
- What challenges arise when processing live sports data?

**Context:** The assignment is to implement a simple Java 21 Maven library that manages a live Football World Cup scoreboard. It must support multiple simultaneous matches and provide operations for starting matches, updating scores, finishing matches and retrieving a summary. The assignment intentionally leaves some design decisions open, so the goal is to understand the domain before designing a simplified library.

**Outcome:** Summarized the key findings about live sports data systems and the typical flow of match information, used as background for the scope decisions.

#### 2.2.2 Concurrent Updates and Data Consistency
**Prompt:** Based on how real-world sports data platforms operate, explain how they typically handle the following scenarios:
1. Two independent providers report the same goal, and due to network latency the notification reaches the scoreboard twice, almost simultaneously.
2. A goal is reported, but a few seconds later it is disallowed by VAR and the score must be corrected.
3. Two providers report different scores for the same match because of differences in update timing.
4. A "match finished" update arrives before an earlier score update.

Please cover duplicate detection and idempotency, event ordering and delayed messages, conflict resolution between providers, data consistency, and score corrections with match status transitions. Finally, distinguish mechanisms used in production distributed systems from those relevant to a simple in-memory Java library.

**Context:** The project is a simplified scoreboard library, not a distributed sports data processing platform. The goal is to understand real-world challenges before deciding which of them should influence the library design.

**Outcome:** Summarized the production-level challenges and typical approaches, and identified which concerns stay out of scope (providers, event ordering, conflict resolution) and which influence the design (absolute score updates for corrections, thread safety within a single JVM).

#### 2.2.3 Library Responsibilities and Scope
**Prompt:** Analyze the responsibilities and boundaries of the Football World Cup Scoreboard library. Which features belong in the library and which stay outside (persistence, external data sources and APIs, event processing)? Which implementation details should stay internal and which belong in the public API? Which future extensions are worth noting without implementing them now? Distinguish requirements of a small reusable library from concerns of a production sports data platform.

**Context:** The project is a standalone in-memory Java 21 Maven library supporting starting matches, updating scores, finishing matches and retrieving a summary of ongoing matches. It should stay simple, maintainable and independent of external infrastructure.

**Outcome:** Documented the responsibilities, boundaries and assumptions of the library, including features intentionally excluded from the initial implementation.

#### 2.2.4 Functional and Non-functional Requirements and Edge Cases
**Prompt:** Translate the Football World Cup Scoreboard assignment into explicit functional and non-functional requirements and identify edge cases to consider. Cover starting and identifying matches, updating scores, finishing matches, retrieving and sorting summaries, invalid input (null or blank names, identical teams, negative or overflowing scores, name normalization such as surrounding whitespace and letter case), operations on non-existent matches, invalid state transitions, a team taking part in two ongoing matches at once, home/away order, multiple simultaneous matches, thread safety, and consistent summaries during concurrent modifications.

For each case: explain the potential problem, recommend the expected behavior, indicate whether it belongs in this assignment or is out of scope, and avoid requirements that are unnecessary for a simple in-memory library.

**Context:** The required operations are:
1. Start a new match.
2. Update the score.
3. Finish a match.
4. Get a summary of matches in progress, ordered by total score (descending) and, for equal totals, by the most recently started match first.

The implementation uses Java 21 and Maven.

**Outcome:** Documented the functional and non-functional requirements, the identified edge cases and the final scope of the library.

### 2.3 Design Decisions

#### 2.3.1 Domain Model and Object Design
**Prompt:** Propose a domain model for the Football World Cup Scoreboard library. Which concepts (Scoreboard, Match, Score, Team, MatchStatus) should be classes, records or enums in Java 21? How should responsibilities be split between the domain objects and the scoreboard? How should encapsulation prevent clients from modifying internal match state, and should immutable snapshots be exposed? Suggest a class structure and avoid unnecessary abstractions.

**Context:** The library is an in-memory Java 21 Maven project supporting multiple simultaneous matches, score updates, match completion and summary retrieval. The design should be easy to test and maintain, with complexity proportional to the assignment.

**Outcome:** Documented the domain model, class responsibilities and object design decisions used in the implementation.

#### 2.3.2 Match Identification and Storage
**Prompt:** Evaluate how matches should be uniquely identified and stored. Are team names sufficient, or should a unique identifier such as a UUID be used? How should multiple matches involving the same teams be supported, which data structure fits best, and how should duplicate registration (including the same team in two ongoing matches) be handled?

**Context:** The library operates entirely in memory and supports multiple simultaneous matches. The same teams may meet more than once over time, so identification must not rely solely on team names.

**Outcome:** Documented the selected match identification strategy, storage structure and duplicate registration policy.

#### 2.3.3 Match Lifecycle and State Management
**Prompt:** Determine how the match lifecycle should be represented. Which states and transitions are allowed? Should score updates be possible only while a match is in progress? Should finished matches remain stored? How should score corrections, including reductions, be supported, and which component enforces the lifecycle rules?

**Context:** A match is created when it starts and can then be updated or finished. The summary contains only ongoing matches. Score updates use absolute values rather than increments, which allows corrections such as VAR decisions.

**Outcome:** Documented the selected lifecycle model, allowed state transitions and score update policy.

#### 2.3.4 Public API and Error Handling
**Prompt:** Propose a minimal public API for the four required operations, with method names, parameters and return types. How should matches be identified by the caller? How should invalid input (null or blank names, identical teams, negative scores), a missing match and invalid state (modifying a finished match) be reported? When should standard Java exceptions be used instead of custom ones, and should the API expose immutable snapshots or Optional values? Provide example signatures and reasoning.

**Context:** The project is a reusable in-memory Java 21 Maven library. The API should clearly distinguish invalid input, invalid match state and missing matches without unnecessary complexity.

**Outcome:** Documented the public API, method contracts and error-handling strategy adopted in the implementation.

#### 2.3.5 Thread Safety and Concurrent Access
**Prompt:** Evaluate how thread safety should be ensured when multiple threads use the scoreboard. What race conditions can occur during match creation, score updates and completion? Are synchronized methods sufficient, or are thread-safe collections or more advanced mechanisms justified? How can summary retrieval return a consistent snapshot, and how can clients be prevented from modifying internal state? Distinguish thread safety within a single JVM from distributed concurrency across application instances.

**Context:** The library is an in-memory Java 21 implementation running within a single JVM. It should support concurrent access while keeping the implementation simple.

**Outcome:** Documented the selected synchronization strategy, consistency guarantees and limitations.

#### 2.3.6 Summary Ordering and Deterministic Results
**Prompt:** Determine how ongoing matches should be ordered in the summary. Is a timestamp alone enough to guarantee deterministic ordering for matches started within a very short interval, or should a monotonically increasing sequence number be used as a tie-breaker? How should finished matches be excluded?

**Context:** Matches are sorted by total score (descending), and for equal totals the most recently started match comes first. Results must be deterministic.

**Outcome:** Documented the sorting criteria and the mechanism used to guarantee deterministic ordering.

#### 2.3.7 Performance and Scalability Considerations
**Prompt:** Analyze the performance characteristics relevant to the library: the cost of starting, updating and finishing a match, the cost of building a sorted summary, likely bottlenecks as the number of simultaneous matches grows, and which optimizations are reasonable and which would be premature. Distinguish the needs of an in-memory library from those of a distributed production system.

**Context:** The library runs within a single JVM, and the implementation should prioritize correctness, readability and maintainability over optimizing for an unspecified workload.

**Outcome:** Documented the expected performance characteristics, selected data structures, limitations and optimization decisions.

#### 2.3.8 Additional Feature Selection
**Prompt:** Evaluate possible additional features beyond the mandatory requirements, for example retrieving a match by its identifier, filtering ongoing matches by team name, tracking score change history, and providing match statistics or metadata. For each, evaluate usefulness, fit with the domain, implementation complexity, impact on the API and model, testability and risk of unnecessary complexity. Recommend one feature that is small, independently testable and appropriate for the scope, and explain why it is preferable to the alternatives.

**Context:** The assignment requires one additional feature, clearly separated from the mandatory functionality and introduced in a dedicated Git commit.

**Outcome:** Documented the evaluated features, the selected feature and the reasoning behind the decision.

#### 2.3.9 Additional Feature Design
**Prompt:** Define the design of the selected feature: its purpose and expected behavior, public API and parameters, interaction with existing domain objects and scoreboard operations, input validation and error scenarios, thread-safety considerations, the unit tests required, and how to keep it independent of the mandatory functionality.

**Context:** The feature must add meaningful value without changing the core responsibilities of the library or introducing unnecessary complexity.

**Outcome:** Documented the selected feature, its API, integration approach and validation strategy.

### 2.4 Implementation

...

### 2.5 Testing

#### 2.5.1 Test Scenarios and Validation Strategy

**Prompt:** Based on the requirements and the proposed API, identify unit and concurrency test scenarios for the scoreboard. Cover normal operation, input validation, invalid lifecycle transitions, missing matches, summary sorting including tied totals, an empty scoreboard, summary snapshots not being affected by later changes, concurrent score updates, concurrent attempts to start or finish the same match, and concurrent summary retrieval during updates. For each scenario describe the expected result and recommend a testing approach using JUnit Jupiter and Java 21 without unnecessary dependencies, including how to make concurrency tests deterministic (for example ExecutorService with CountDownLatch).

**Context:** The library is an in-memory Java 21 Maven project that should be simple, thread-safe and independently testable.

**Outcome:** Documented the test scenarios and the testing strategy used to validate the implementation.

### 2.6 Code Review and Refactoring

...

## 3. Additional Context and Artifacts

- Original assignment requirements
- Design notes
- Relevant technical documentation
- Other references used during implementation

## 4. Human Review and Validation

AI-generated suggestions were reviewed, tested and adapted. Examples so far:
- The Automatic-Module-Name and `<name>` in pom.xml were changed from the template placeholders to match the project (`com.sportradar.worldcup.scoreboard`, `world-cup-scoreboard`).
- Prompts were revised to remove obvious questions and to add missing edge cases.
- The JaCoCo coverage check and maven-jar-plugin were kept after review.

...