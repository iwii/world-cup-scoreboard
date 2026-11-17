package com.sportradar.worldcup.scoreboard;

import com.sportradar.worldcup.scoreboard.exception.MatchNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Deterministically covers the defensive branches in {@link Scoreboard} that throw
 * "Match not found or already finished with ID": the match entry is removed from the registry
 * after the operation fetched its {@link MatchContext} but before it acquired the match lock.
 *
 * <p>The window is forced without sleeping or racing real operations:
 * <ol>
 *   <li>the test thread holds the match lock;</li>
 *   <li>a worker thread starts the operation and blocks on that lock;</li>
 *   <li>once the worker is queued on the lock, the test removes the entry from the registry
 *       and releases the lock;</li>
 *   <li>the worker resumes and must hit the identity check / failed conditional removal.</li>
 * </ol>
 * The assertions check for the "already finished" message, which only the defensive branches
 * produce (the plain "Match not found" branch uses a different message).
 */
@Timeout(value = 10, unit = TimeUnit.SECONDS)
class ScoreboardRaceConditionTest {

    private static final long WAIT_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(5);

    private Scoreboard scoreboard;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        scoreboard = new Scoreboard();
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    @DisplayName("updateScore rejects an update when the match is removed while waiting for the lock")
    void updateScoreShouldRejectWhenRemovedWhileWaitingForLock() throws Exception {
        MatchId matchId = scoreboard.startMatch("Mexico", "Canada").id();

        Future<MatchSummary> update = runAndRemoveWhileBlockedOnLock(
                matchId, () -> scoreboard.updateScore(matchId, 1, 0));

        assertRejectedAsAlreadyFinished(update, matchId);
    }

    @Test
    @DisplayName("finishMatch rejects a finish when the match is removed while waiting for the lock")
    void finishMatchShouldRejectWhenRemovedWhileWaitingForLock() throws Exception {
        MatchId matchId = scoreboard.startMatch("Mexico", "Canada").id();

        Future<MatchSummary> finish = runAndRemoveWhileBlockedOnLock(
                matchId, () -> scoreboard.finishMatch(matchId));

        assertRejectedAsAlreadyFinished(finish, matchId);
    }

    /**
     * Holds the match lock, starts the operation on a worker thread, waits until the worker is
     * blocked on that lock, removes the entry from the registry and only then releases the lock.
     */
    private <T> Future<T> runAndRemoveWhileBlockedOnLock(MatchId matchId, Callable<T> operation) {
        MatchContext context = registry().get(matchId);
        ReentrantLock lock = context.lock;

        lock.lock();
        try {
            Future<T> future = executor.submit(operation);
            awaitQueuedThread(lock);
            registry().remove(matchId);
            return future;
        } finally {
            lock.unlock();
        }
    }

    private void awaitQueuedThread(ReentrantLock lock) {
        long deadline = System.nanoTime() + WAIT_TIMEOUT_NANOS;
        while (!lock.hasQueuedThreads()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Worker thread never blocked on the match lock");
            }
            Thread.yield();
        }
    }

    private void assertRejectedAsAlreadyFinished(Future<?> future, MatchId matchId) {
        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(MatchNotFoundException.class)
                .cause()
                .hasMessageContaining("already finished")
                .hasMessageContaining(matchId.toString());
        assertThat(scoreboard.getSummary()).isEmpty();
    }

    /**
     * Gives the test access to the internal registry of ongoing matches.
     */
    @SuppressWarnings("unchecked")
    private Map<MatchId, MatchContext> registry() {
        try {
            Field field = Scoreboard.class.getDeclaredField("matches");
            field.setAccessible(true);
            return (Map<MatchId, MatchContext>) field.get(scoreboard);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot access Scoreboard.matches", e);
        }
    }
}