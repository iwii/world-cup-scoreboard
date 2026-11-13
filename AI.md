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

**Prompt:**
[Original prompt]

**Context:**
[Additional context]

**Outcome:**
[How the response influenced the implementation]

---

### 2.3 Design Decisions

**Prompt:**
[Original prompt]

**Outcome:**
[Decision and reasoning]

---

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