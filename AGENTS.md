# Rules for AI contributors

These rules apply to the whole repository.

## Project facts

- This is a Java library for an in-memory World Cup match scoreboard.
- Compile against Java 21 (`maven.compiler.release=21`). The Maven Enforcer requires JDK 21+ and Maven 3.10+.
- Build with Maven. Tests use JUnit Jupiter 5.11.4 and AssertJ 3.27.7. JaCoCo enforces at least 80% line coverage during `mvn verify`.
<!-- INACTIVE RULE — do not follow:
- Before finishing any changes, run `mvn verify` from the repository root and report whether it succeeded. If Maven cannot be run in the current environment, report that instead of relying on an earlier run.
-->
- Follow test-driven development (TDD): for behavior changes, first add or update a focused test, run it to establish the expected failure, implement the smallest change, and run the relevant tests. Run the full suite when practical. Do not claim a command passed unless it was run and passed.

## Preserve behavior

- Do not change existing behavior, business rules, public API, concurrency guarantees, or exception semantics unless the user explicitly asks for that change.
- If the prompt starts with a request to add or write test(s) (for example, "add test", "add tests", or "test"), limit the changes to adding or updating test code. Do not modify production/business classes, implementation logic, or README as part of that request. If a test cannot be written without a production change, explain the limitation instead of changing production code.
- Treat a request to simplify or refactor as behavior-preserving. Keep current validation, ordering, thread-safety, and exception behavior.
- Before editing production code, check whether the request can be satisfied in tests or documentation alone. Do not add production hooks, constructors, setters, or dependencies solely to make a test easier unless explicitly requested.
- Prefer tests through the public API. Use reflection or other internal access only when a test specifically needs to cover an internal branch that cannot be reached through the public API; keep such tests deterministic and narrow.
- Avoid unrelated edits. Preserve existing names and style unless the requested change requires otherwise.
- After every change that adds or modifies functionality, update `README.md` in the same task to document the user-visible behavior and relevant usage. Keep examples and stated behavior accurate; documentation-only changes do not require unrelated README edits.
- Ask for clarification only when the requested behavior is genuinely ambiguous and cannot be inferred from this file or the existing code.

## TDD and test conventions

- Tests live under `src/test/java` and mirror the production package structure.
- Use JUnit Jupiter annotations (`@Test`, parameterized tests where suitable) and AssertJ assertions.
- Give tests descriptive names and `@DisplayName` descriptions consistent with nearby tests.
- Cover success, validation failures, lifecycle behavior, ordering, and concurrency behavior relevant to a change.
- Cover relevant edge cases, including boundary values, missing or null data, blank input, duplicate or stale operations, and state transitions where applicable. Choose cases that match the behavior under change; avoid adding unrelated edge-case tests.
- Keep tests deterministic. Avoid timing assumptions, random outcomes, and concurrency unless the behavior under test requires them.
- Do not weaken or delete an existing assertion just to make a test pass. Update expectations only when the user explicitly requests a behavior change and the implementation matches it.

## Domain model and responsibilities

| Type | Purpose and important members |
| --- | --- |
| `Scoreboard` | Public, thread-safe API. `startMatch(team1, team2)` validates names, assigns a monotonically increasing sequence, generates a random ID, and registers the match with `putIfAbsent` retry. `updateScore(matchId, team1Score, team2Score)` updates an active match. `finishMatch(matchId)` returns its final summary and removes it. `getSummary()` returns active matches sorted by total score descending, then newest first. `getMatchesByTeam(team)` filters active matches and applies the same ordering. `involves(context, team)` checks team membership; `validateScore(score)` rejects negative scores. `matches` stores active contexts; `sequenceGenerator` assigns start order; `SNAPSHOT_ORDER` defines summary ordering. Preserve per-match locking and map identity/removal behavior. |
| `MatchContext` | Package-private mutable state for one match. `id`, `team1`, `team2`, and `sequenceNumber` are immutable identity/order data. `team1Score`, `team2Score`, `status`, and `lock` represent guarded match state. `updateScores(...)`, `summaryUnderLock()`, `isInProgressUnderLocked()`, and `markFinishedUnderLock()` require the lock where documented/asserted. `toSummary()` creates a locked summary. `toSnapshotIfActive()` returns an immutable snapshot only while active. Nested `Snapshot(summary, sequence)` carries the internal ordering value with the consistent summary. |
| `MatchSummary` | Public immutable result record: `id`, `team1`, `team2`, `team1Score`, `team2Score`. Its constructor validates the ID, normalizes team names, and rejects negative scores. `normalizeTeamName(name, fieldName)` strips whitespace and rejects null or blank names. |
| `MatchId` | Public UUID-backed record. `value` is non-null. `random()` creates IDs for `startMatch`; `fromString(value)` parses an ID and reports malformed input; `toString()` renders the UUID. |
| `MatchStatus` | Internal lifecycle enum: `IN_PROGRESS` or `FINISHED`. |

### Internal state and ordering

- `Scoreboard.matches` contains active matches. `finishMatch` marks a context finished and removes that exact context while holding its lock.
- `MatchContext.lock` serializes updates, finishing, and consistent reads for one match. Do not replace it with a scoreboard-wide lock or access guarded mutable fields without it.
- `sequenceNumber` is assigned at match start and exists to break equal-total-score ties, with higher/newer values sorted first. It is internal and must not be added to the public `MatchSummary` API without an explicit request.
- Summary reads are consistent per match, but the whole returned list is not an atomic snapshot across all matches.

## Exceptions and when they are used

All domain exceptions extend unchecked `ScoreboardException` (`RuntimeException`). Preserve these distinctions and messages unless explicitly asked to change them:

| Exception | Use |
| --- | --- |
| `ScoreboardException` | Abstract base for library-specific runtime errors. |
| `InvalidTeamException` | A non-null team name is empty/blank after stripping, or both teams in a match are the same ignoring case. Null arguments currently fail with `NullPointerException` from `Objects.requireNonNull`; preserve this behavior. |
| `InvalidScoreException` | A score is negative, whether validated by `Scoreboard` or when constructing `MatchSummary`. |
| `InvalidMatchIdException` | `MatchId.fromString` receives a non-null string that is not a valid UUID. A null string currently fails with `NullPointerException`; preserve this behavior. |
| `MatchNotFoundException` | An ID is not present in the active-match map, or an existing context is already marked finished when an operation acquires its lock. These paths intentionally have different messages (`Match not found with ID: ...` and `Match not found or already finished with ID: ...`). |

## Scope and change discipline

- Update this file when project facts, toolchain versions, architecture, or established development rules change.
- When a user asks to update documentation after a change to behavior or code structure (such as adding or changing classes, methods, fields, or properties), update this file's domain inventory and descriptions to match the resulting code. This keeps the project guidance current and gives future changes a stable, accurate baseline.
- Keep documentation aligned with the code; do not invent members or behavior. If a request conflicts with these rules, follow the user's explicit instruction and update this file only if asked or if the rule itself has changed.
