package com.sportradar.worldcup.scoreboard;

import com.sportradar.worldcup.scoreboard.exception.MatchNotFoundException;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
class ScoreboardConcurrencyTest {

    private static final int THREADS = 32;
    private static final int READERS = 4;
    private static final long TIMEOUT_SECONDS = 10;

    private final Scoreboard scoreboard = new Scoreboard();

    // ---------- helpers ----------

    /**
     * Holds either a result or the failure of a single concurrent task.
     * {@code error} is a {@link Throwable} on purpose: assertion failures thrown in worker threads are
     * {@link AssertionError}s, not {@link Exception}s, and must be reported through {@code assertNoErrors}
     * instead of surfacing as an {@code ExecutionException}.
     */
    private record Outcome<T>(T value, Throwable error) {
        boolean succeeded() {
            return error == null;
        }
    }

    private <T> List<Outcome<T>> runAtOnce(List<Callable<T>> tasks)
            throws Exception {

        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<Outcome<T>>> futures = new ArrayList<>();
            for (Callable<T> task : tasks) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting for concurrent start");
                    }
                    try {
                        return new Outcome<>(task.call(), null);
                    } catch (Exception | AssertionError failure) {
                        // Deliberately not Throwable: JVM errors (e.g. OutOfMemoryError) should not be treated as task outcomes.
                        return new Outcome<>(null, failure);
                    }
                }));
            }
            assertTrue(ready.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "Not all tasks became ready");

            start.countDown();

            List<Outcome<T>> outcomes = new ArrayList<>();
            for (Future<Outcome<T>> future : futures) {
                outcomes.add(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            start.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS), "Executor did not terminate");
        }
    }

    private <T> void assertNoErrors(List<Outcome<T>> outcomes) {
        outcomes.stream()
                .filter(outcome -> !outcome.succeeded())
                .findFirst()
                .ifPresent(outcome -> fail("Unexpected concurrent operation error", outcome.error()));
    }

    private long total(MatchSummary match) {
        return (long) match.team1Score() + match.team2Score();
    }

    private void assertConsistentSnapshot(List<MatchSummary> snapshot, Map<MatchId, Integer> startIndex) {
        Set<MatchId> ids = new HashSet<>();

        for (MatchSummary match : snapshot) {
            assertTrue(ids.add(match.id()), "Duplicate match ID in summary");
            assertTrue(startIndex.containsKey(match.id()), "Unknown match ID in summary");
        }

        for (int i = 1; i < snapshot.size(); i++) {
            MatchSummary previous = snapshot.get(i - 1);
            MatchSummary current = snapshot.get(i);

            assertTrue(total(previous) >= total(current),
                    "Summary is not sorted by total score descending");

            if (total(previous) == total(current)) {
                assertTrue(startIndex.get(previous.id()) > startIndex.get(current.id()),
                        "Equal-score matches are not ordered by start order"
                );
            }
        }
    }

    // ---------- tests ----------

    @Test
    void simultaneousStartsOfTheSameTeamsGetUniqueIds() throws Exception {
        List<Callable<MatchSummary>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            tasks.add(() -> scoreboard.startMatch("Mexico", "Canada"));
        }

        List<Outcome<MatchSummary>> outcomes = runAtOnce(tasks);

        assertNoErrors(outcomes);
        Set<MatchId> ids = outcomes.stream().map(o -> o.value().id()).collect(Collectors.toSet());
        assertEquals(THREADS, ids.size());
        assertEquals(THREADS, scoreboard.getSummary().size());
    }

    @Test
    void simultaneousStartsOfDifferentTeamsAreAllRegistered() throws Exception {
        List<Callable<MatchSummary>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            int index = i;
            tasks.add(() -> scoreboard.startMatch("Home" + index, "Away" + index));
        }

        List<Outcome<MatchSummary>> outcomes = runAtOnce(tasks);

        assertNoErrors(outcomes);
        assertEquals(THREADS, scoreboard.getSummary().size());
        assertEquals(THREADS, outcomes.stream().map(outcome -> outcome.value().id()).collect(Collectors.toSet()).size());
        assertEquals(THREADS, scoreboard.getSummary().stream().map(MatchSummary::team1).collect(Collectors.toSet()).size());
    }

    @Test
    void concurrentUpdatesOfDifferentMatchesAreAllApplied() throws Exception {
        List<MatchId> ids = IntStream.range(0, THREADS)
                .mapToObj(i -> scoreboard.startMatch("Home" + i, "Away" + i).id())
                .toList();

        List<Callable<MatchSummary>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            MatchId id = ids.get(i);
            int score = i + 1;
            tasks.add(() -> scoreboard.updateScore(id, score, 0));
        }

        assertNoErrors(runAtOnce(tasks));

        for (int i = 0; i < THREADS; i++) {
            MatchId id = ids.get(i);
            MatchSummary updated = scoreboard.getSummary().stream()
                    .filter(match -> match.id().equals(id))
                    .findFirst()
                    .orElseThrow();

            assertEquals(i + 1, updated.team1Score());
            assertEquals(0, updated.team2Score());
        }
    }

    @Test
    void concurrentUpdatesOfOneMatchNeverMixTwoUpdates() throws Exception {
        MatchSummary match = scoreboard.startMatch("Mexico", "Canada");

        List<Callable<MatchSummary>> tasks = new ArrayList<>();
        for (int i = 1; i <= THREADS; i++) {
            int score = i;
            tasks.add(() -> scoreboard.updateScore(match.id(), score, score));
        }

        List<Outcome<MatchSummary>> outcomes = runAtOnce(tasks);

        assertNoErrors(outcomes);

        assertTrue(outcomes.stream().allMatch(outcome ->
                outcome.value().team1Score() == outcome.value().team2Score()
        ));

        MatchSummary finalResult = scoreboard.getSummary().getFirst();

        assertEquals(finalResult.team1Score(), finalResult.team2Score());
        assertTrue(finalResult.team1Score() >= 1);
        assertTrue(finalResult.team1Score() <= THREADS);
    }

    @Test
    void concurrentFinishOfTheSameMatchSucceedsExactlyOnce() throws Exception {
        MatchSummary match = scoreboard.startMatch("Mexico", "Canada");

        List<Callable<MatchSummary>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            tasks.add(() -> scoreboard.finishMatch(match.id()));
        }

        List<Outcome<MatchSummary>> outcomes = runAtOnce(tasks);

        long successfulFinishes = outcomes.stream().filter(Outcome::succeeded).count();
        long rejectedFinishes = outcomes.stream()
                .filter(outcome -> outcome.error() instanceof MatchNotFoundException)
                .count();

        assertEquals(1, successfulFinishes);
        assertEquals(THREADS - 1, rejectedFinishes);
        assertTrue(scoreboard.getSummary().isEmpty());
    }

    @Test
    void concurrentFinishOfDifferentMatchesAllSucceed() throws Exception {
        List<MatchId> ids = IntStream.range(0, THREADS)
                .mapToObj(i -> scoreboard.startMatch("Home" + i, "Away" + i).id())
                .toList();
        List<Callable<MatchSummary>> tasks = new ArrayList<>();
        for (MatchId id : ids) {
            tasks.add(() -> scoreboard.finishMatch(id));
        }

        List<Outcome<MatchSummary>> outcomes = runAtOnce(tasks);

        assertNoErrors(outcomes);
        assertEquals(THREADS, outcomes.size());
        assertTrue(scoreboard.getSummary().isEmpty());
    }
    @RepeatedTest(100)
    void updateRacingWithFinishHasOnlyTwoValidOutcomes() throws Exception {
        Scoreboard scoreboard = new Scoreboard();
        MatchSummary match = scoreboard.startMatch("Mexico", "Canada");

        List<Callable<MatchSummary>> tasks = List.of(
                () -> scoreboard.updateScore(match.id(), 3, 2),
                () -> scoreboard.finishMatch(match.id())
        );

        List<Outcome<MatchSummary>> outcomes = runAtOnce(tasks);

        Outcome<MatchSummary> update = outcomes.getFirst();
        Outcome<MatchSummary> finish = outcomes.getLast();

        assertTrue(finish.succeeded());

        if (update.succeeded()) {
            assertEquals(3, finish.value().team1Score());
            assertEquals(2, finish.value().team2Score());
        } else {
            assertInstanceOf(MatchNotFoundException.class, update.error());
            assertEquals(0, finish.value().team1Score());
            assertEquals(0, finish.value().team2Score());
        }
        assertTrue(scoreboard.getSummary().isEmpty());
    }

    @Test
    void summaryReadDuringConcurrentUpdatesIsAlwaysConsistent() throws Exception {
        List<MatchId> ids = IntStream.range(0, THREADS)
                .mapToObj(i -> scoreboard.startMatch("Home" + i, "Away" + i).id())
                .toList();

        Map<MatchId, Integer> startIndex = IntStream.range(0, THREADS)
                .boxed()
                .collect(Collectors.toMap(ids::get, i -> i));

        CountDownLatch writersDone = new CountDownLatch(THREADS);
        // Counted down by each reader after its first snapshot: writers start only once all readers are running.
        // This guarantees overlap without relying on wall-clock timing or Thread.sleep.
        CountDownLatch readersStarted = new CountDownLatch(READERS);
        // Counted down by the first applied update: marks the beginning of the write phase.
        CountDownLatch writingStarted = new CountDownLatch(1);
        AtomicInteger snapshotsTakenWhileWriting = new AtomicInteger();

        List<Callable<Void>> writeTasks = IntStream.range(0, THREADS)
                .<Callable<Void>>mapToObj(index -> () -> {
                    try {
                        // Wait until every reader has taken at least one snapshot,
                        // so that readers are guaranteed to be running before any write happens.
                        assertTrue(readersStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                                "Readers did not start");
                        MatchId id = ids.get(index);
                        for (int score = 1; score <= 500; score++) {
                            scoreboard.updateScore(id, score, score);
                            writingStarted.countDown();
                        }
                        return null;
                    } finally {
                        writersDone.countDown();
                    }
                })
                .toList();

        List<Callable<Void>> readTasks = IntStream.range(0, READERS)
                .<Callable<Void>>mapToObj(index -> () -> {
                    boolean firstSnapshot = true;
                    do {
                        // A snapshot overlaps the write phase if writing had started before it
                        // and was still not finished after it.
                        boolean writingHadStarted = writingStarted.getCount() == 0;
                        List<MatchSummary> snapshot = scoreboard.getSummary();
                        boolean writersStillRunning = writersDone.getCount() > 0;

                        assertEquals(THREADS, snapshot.size());
                        assertConsistentSnapshot(snapshot, startIndex);

                        if (writingHadStarted && writersStillRunning) {
                            snapshotsTakenWhileWriting.incrementAndGet();
                        }
                        if (firstSnapshot) {
                            readersStarted.countDown();
                            firstSnapshot = false;
                        }
                    } while (writersDone.getCount() > 0);
                    return null;
                })
                .toList();

        List<Callable<Void>> tasks = Stream.concat(writeTasks.stream(), readTasks.stream()).toList();

        assertNoErrors(runAtOnce(tasks));

        assertTrue(snapshotsTakenWhileWriting.get() > 0, "readers never overlapped with writers");

        List<MatchSummary> finalSnapshot = scoreboard.getSummary();
        assertConsistentSnapshot(finalSnapshot, startIndex);
        assertTrue(finalSnapshot.stream().allMatch(m -> m.team1Score() == 500 && m.team2Score() == 500));
        assertEquals(ids.reversed(), finalSnapshot.stream().map(MatchSummary::id).toList());
    }

    @Test
    void matchesByTeamReadDuringConcurrentUpdatesIsAlwaysConsistent() throws Exception {
        List<MatchId> ids = IntStream.range(0, THREADS)
                .mapToObj(i -> scoreboard.startMatch("Followed", "Opponent" + i).id())
                .toList();

        Map<MatchId, Integer> startIndex = IntStream.range(0, THREADS)
                .boxed()
                .collect(Collectors.toMap(ids::get, i -> i));

        CountDownLatch writersDone = new CountDownLatch(THREADS);
        CountDownLatch readersStarted = new CountDownLatch(READERS);
        CountDownLatch writingStarted = new CountDownLatch(1);
        AtomicInteger snapshotsTakenWhileWriting = new AtomicInteger();

        List<Callable<Void>> writeTasks = IntStream.range(0, THREADS)
                .<Callable<Void>>mapToObj(index -> () -> {
                    try {
                        assertTrue(readersStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                                "Readers did not start");
                        for (int score = 1; score <= 500; score++) {
                            scoreboard.updateScore(ids.get(index), score, score);
                            writingStarted.countDown();
                        }
                        return null;
                    } finally {
                        writersDone.countDown();
                    }
                })
                .toList();

        List<Callable<Void>> readTasks = IntStream.range(0, READERS)
                .<Callable<Void>>mapToObj(index -> () -> {
                    boolean firstSnapshot = true;
                    do {
                        boolean writingHadStarted = writingStarted.getCount() == 0;
                        List<MatchSummary> snapshot = scoreboard.getMatchesByTeam(" followed ");
                        boolean writersStillRunning = writersDone.getCount() > 0;

                        assertEquals(THREADS, snapshot.size());
                        assertConsistentSnapshot(snapshot, startIndex);
                        assertTrue(snapshot.stream().allMatch(m -> m.team1Score() == m.team2Score()),
                                "A match snapshot mixed scores from separate updates");

                        if (writingHadStarted && writersStillRunning) {
                            snapshotsTakenWhileWriting.incrementAndGet();
                        }
                        if (firstSnapshot) {
                            readersStarted.countDown();
                            firstSnapshot = false;
                        }
                    } while (writersDone.getCount() > 0);
                    return null;
                })
                .toList();

        List<Callable<Void>> tasks = Stream.concat(writeTasks.stream(), readTasks.stream()).toList();
        assertNoErrors(runAtOnce(tasks));

        assertTrue(snapshotsTakenWhileWriting.get() > 0, "readers never overlapped with writers");
        List<MatchSummary> finalSnapshot = scoreboard.getMatchesByTeam("Followed");
        assertConsistentSnapshot(finalSnapshot, startIndex);
        assertTrue(finalSnapshot.stream().allMatch(m -> m.team1Score() == 500 && m.team2Score() == 500));
    }
}
