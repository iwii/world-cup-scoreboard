package com.sportradar.worldcup.scoreboard;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Encapsulates the mutable state, thread-safety lock, and metadata for an active match.
 * Acts as the internal domain model managed by the Scoreboard; it is never exposed to clients,
 * who only see immutable {@link MatchSummary} snapshots.
 *
 * <p>Thread-safety: the scores are guarded by {@link #lock}. The identity fields
 * ({@code id}, teams, {@code sequenceNumber}) are final and safe to read without locking.
 * The lock is package-private so that {@link Scoreboard} can hold it across compound operations
 * (registry identity check followed by an update, or conditional removal from the registry).
 */
final class MatchContext {
    final MatchId id;
    final String team1;
    final String team2;

    // Monotonically increasing start order, used only for chronological tie-breaking in summaries
    final long sequenceNumber;

    // Current score; guarded by lock (reads in toSummary()/toSnapshot(), writes via updateScores())
    int team1Score;
    int team2Score;

    // Dedicated ReentrantLock per match to serialize updates and finishes concurrently
    final ReentrantLock lock = new ReentrantLock();

    MatchContext(MatchId id, String team1, String team2, long sequenceNumber) {
        this.id = id;
        this.team1 = team1;
        this.team2 = team2;
        this.sequenceNumber = sequenceNumber;
    }

    /**
     * Updates the absolute scores for both teams (supports VAR downward corrections).
     * The caller must hold {@link #lock}; input validation is done by the Scoreboard.
     */
    void updateScores(int team1Score, int team2Score) {
        this.team1Score = team1Score;
        this.team2Score = team2Score;
    }

    private MatchSummary summaryUnderLock() {  // caller must hold lock
        return new MatchSummary(id, team1, team2, team1Score, team2Score);
    }

    /**
     * Creates an immutable MatchSummary snapshot acting as the source of truth.
     * Both scores are read under the lock, so a concurrent update is never seen half-applied.
     * The lock is reentrant, so this is also safe to call while the caller already holds it.
     */
    MatchSummary toSummary() {
        lock.lock();
        try {
            return summaryUnderLock();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Immutable summary of a match paired with its internal start sequence.
     * The sequence is used only for ordering inside the Scoreboard and is not part of the public API.
     */
    record Snapshot(MatchSummary summary, long sequence) {}

    /**
     * Creates a consistent {@link Snapshot} of this match (scores read under the lock)
     * for sorting by total score and start order.
     */
    Snapshot toSnapshot() {
        lock.lock();
        try {
            return new Snapshot(summaryUnderLock(), sequenceNumber);
        } finally {
            lock.unlock();
        }
    }
}
