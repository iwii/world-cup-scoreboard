package com.sportradar.worldcup.scoreboard;

import com.sportradar.worldcup.scoreboard.exception.*;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Scoreboard {

    // Thread-safe map storing active matches, offering lock-free concurrent structure access
    private final ConcurrentHashMap<MatchId, MatchContext> matches = new ConcurrentHashMap<>();

    // Monotonically increasing sequence generator for chronological tie-breaking
    private final AtomicLong sequenceGenerator = new AtomicLong(0);

    private static final Comparator<MatchContext.Snapshot> SNAPSHOT_ORDER =
            Comparator
                    .comparingLong((MatchContext.Snapshot s) ->
                            (long) s.summary().team1Score() + s.summary().team2Score()).reversed()
                    .thenComparing(Comparator.comparingLong(
                            MatchContext.Snapshot::sequence).reversed());

    /**
     * Starts a new match with initial score 0-0 after validating and trimming team names.
     */
    public MatchSummary startMatch(String team1, String team2) {
        String trimmed1 = validateTeamNameAndTrimmed(team1);
        String trimmed2 = validateTeamNameAndTrimmed(team2);

        if (trimmed1.equalsIgnoreCase(trimmed2)) {
            throw new InvalidTeamException("A team cannot play against itself: " + trimmed1);
        }

        long sequence = sequenceGenerator.incrementAndGet();
        MatchId matchId;
        MatchContext context;
        do {
            matchId = MatchId.random();
            context = new MatchContext(matchId, trimmed1, trimmed2, sequence);
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
            // Strict identity check: ensure the map still references this exact MatchContext instance.
            // Protects against a race condition where the match was removed while waiting for the lock.
            if (matches.get(matchId) != context) {
                throw new MatchNotFoundException("Match not found or already finished with ID: " + matchId);
            }
            context.updateScores(team1Score, team2Score);
            return context.toSummary();
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
            // Atomic conditional removal: removes the entry only if it is still mapped to this specific context.
            if (!matches.remove(matchId, context)) {
                throw new MatchNotFoundException("Match not found or already finished with ID: " + matchId);
            }
            return context.toSummary();
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
                    .map(MatchContext::toSnapshot)
                    .sorted(SNAPSHOT_ORDER)
                    .map(MatchContext.Snapshot::summary)
                    .toList();
        }

    public List<MatchSummary> getMatchesByTeam(String team) {
        String normalized = validateTeamNameAndTrimmed(team).toLowerCase(Locale.ROOT);
        return matches.values().stream()
                .map(MatchContext::toSnapshot)              // ← snapshot pod lockiem
                .filter(s -> matchesTeam(s.summary(), normalized))
                .sorted(SNAPSHOT_ORDER)
                .map(MatchContext.Snapshot::summary)
                .toList();
    }

    private static boolean matchesTeam(MatchSummary summary, String normalizedTeam) {
        return summary.team1().toLowerCase(Locale.ROOT).equals(normalizedTeam)
                || summary.team2().toLowerCase(Locale.ROOT).equals(normalizedTeam);
    }

    private String validateTeamNameAndTrimmed(String teamName) {
        if (teamName == null || teamName.trim().isEmpty()) {
            throw new InvalidTeamException("Team name cannot be null, empty or blank");
        }
        return teamName.trim();
    }

    private void validateScore(int score) {
        if (score < 0) {
            throw new InvalidScoreException("Score cannot be negative: " + score);
        }
    }

}

