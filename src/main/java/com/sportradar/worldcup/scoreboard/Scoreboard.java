package com.sportradar.worldcup.scoreboard;

import com.sportradar.worldcup.scoreboard.exception.InvalidScoreException;
import com.sportradar.worldcup.scoreboard.exception.InvalidTeamException;
import com.sportradar.worldcup.scoreboard.exception.MatchNotFoundException;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory scoreboard of live Football World Cup matches.
 *
 * <p>Supports starting a match, updating its absolute score, finishing it and listing the matches
 * in progress ordered by total score (descending) and, for equal totals, by most recently started first.
 * Finished matches are removed; the library has no persistence and starts no background threads.
 *
 * <p><b>Thread-safety:</b> all public operations are thread-safe. Matches are kept in a
 * {@link java.util.concurrent.ConcurrentHashMap} and operations on the same match are serialized by a
 * per-match lock, while operations on different matches run concurrently. {@link #getSummary()} and
 * {@link #getMatchesByTeam(String)} read each match consistently under its lock, but the returned list is
 * not an atomic snapshot of the whole scoreboard: matches may change while it is being built.
 * The class works with both platform and virtual threads.
 */
public final class Scoreboard {

    // Total score descending, then most recently started (highest sequence) first
    private static final Comparator<MatchContext.Snapshot> SNAPSHOT_ORDER =
            Comparator
                    .comparingLong((MatchContext.Snapshot s) ->
                            (long) s.summary().team1Score() + s.summary().team2Score()).reversed()
                    .thenComparing(Comparator.comparingLong(
                            MatchContext.Snapshot::sequence).reversed());

    // Thread-safe map storing active matches, offering lock-free concurrent structure access
    private final ConcurrentHashMap<MatchId, MatchContext> matches = new ConcurrentHashMap<>();

    // Monotonically increasing sequence generator for chronological tie-breaking
    private final AtomicLong sequenceGenerator = new AtomicLong(0);

    /**
     * Starts a new match with initial score 0-0 after validating and normalizing team names.
     */
    public MatchSummary startMatch(String team1, String team2) {
        String normalized1 = MatchSummary.normalizeTeamName(team1, "team1");
        String normalized2 = MatchSummary.normalizeTeamName(team2, "team2");

        if (normalized1.equalsIgnoreCase(normalized2)) {
            throw new InvalidTeamException("A team cannot play against itself: " + normalized1);
        }

        long sequence = sequenceGenerator.incrementAndGet();
        MatchId matchId;
        MatchContext context;
        do {
            matchId = MatchId.random();
            context = new MatchContext(
                    matchId, normalized1, normalized2, sequence, MatchStatus.IN_PROGRESS);
        } while (matches.putIfAbsent(matchId, context) != null);
        return context.toSummary();
    }

    /**
     * Updates absolute scores for a specific match under fine-grained per-match lock protection.
     */
    public MatchSummary updateScore(MatchId matchId, int team1Score, int team2Score) {
        Objects.requireNonNull(matchId, "matchId");
        validateScore(team1Score);
        validateScore(team2Score);

        MatchContext context = matches.get(matchId);
        if (context == null) {
            throw new MatchNotFoundException("Match not found with ID: " + matchId);
        }

        context.lock.lock();
        try {
            if (!context.isInProgressUnderLocked()) {
                throw new MatchNotFoundException(
                        "Match not found or already finished with ID: " + matchId);
            }
            context.updateScores(team1Score, team2Score);
            return context.summaryUnderLock();
        } finally {
            context.lock.unlock();
        }
    }

    /**
     * Finishes and removes a match from the scoreboard atomically under lock protection.
     */
    public MatchSummary finishMatch(MatchId matchId) {
        Objects.requireNonNull(matchId, "matchId");
        MatchContext context = matches.get(matchId);
        if (context == null) {
            throw new MatchNotFoundException("Match not found with ID: " + matchId);
        }

        context.lock.lock();
        try {
            if (!context.isInProgressUnderLocked()) {
                throw new MatchNotFoundException(
                        "Match not found or already finished with ID: " + matchId);
            }
            context.markFinishedUnderLock();
            matches.remove(matchId, context);
            return context.summaryUnderLock();
        } finally {
            context.lock.unlock();
        }
    }

    /**
     * Returns an on-demand snapshot summary of all active matches.
     * Sorted primarily by total score descending, and secondarily by sequence number descending (newest first).
     */
    public List<MatchSummary> getSummary() {
        return matches.values().stream()
                .map(MatchContext::toSnapshotIfActive)
                .flatMap(Optional::stream)
                .sorted(SNAPSHOT_ORDER)
                .map(MatchContext.Snapshot::summary)
                .toList();
    }

    /**
     * Returns the matches in progress in which the given team plays.
     */
    public List<MatchSummary> getMatchesByTeam(String team) {
        String normalized = MatchSummary.normalizeTeamName(team, "team");
        return matches.values().stream()
                .filter(context -> involves(context, normalized))
                .map(MatchContext::toSnapshotIfActive)
                .flatMap(Optional::stream)
                .sorted(SNAPSHOT_ORDER)
                .map(MatchContext.Snapshot::summary)
                .toList();
    }

    // Team names are final, so they can be read without the match lock; unrelated matches are never locked
    private static boolean involves(MatchContext context, String team) {
        return context.team1.equalsIgnoreCase(team) || context.team2.equalsIgnoreCase(team);
    }

    private static void validateScore(int score) {
        if (score < 0) {
            throw new InvalidScoreException("Score cannot be negative: " + score);
        }
    }
}