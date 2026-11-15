package com.sportradar.worldcup.scoreboard;

import com.sportradar.worldcup.exception.InvalidScoreException;
import com.sportradar.worldcup.exception.InvalidTeamException;
import com.sportradar.worldcup.exception.MatchNotFoundException;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class Scoreboard {

    private final Map<MatchId, Match> matches = new HashMap<>();
    private long nextStartSequence;

    public synchronized MatchSummary startMatch(String homeTeam, String awayTeam) {
        Team home = new Team(homeTeam);
        Team away = new Team(awayTeam);

        if (home.sameNameAs(away)) {
            throw new InvalidTeamException("Home and away teams must be different");
        }

        MatchId id;
        do {
            id = new MatchId(UUID.randomUUID());
        } while (matches.containsKey(id));

        Match match = new Match(id, home, away, 0, 0, nextStartSequence++);

        matches.put(id, match);

        return match.toSummary();
    }

    public synchronized MatchSummary updateScore(
            MatchId id,
            int homeScore,
            int awayScore
    ) {
        Objects.requireNonNull(id, "id");
        validateScore(homeScore);
        validateScore(awayScore);

        Match match = findMatch(id);
        Match updatedMatch = match.withScore(homeScore, awayScore);

        matches.put(id, updatedMatch);

        return updatedMatch.toSummary();
    }

    public synchronized MatchSummary finishMatch(MatchId id) {
        Objects.requireNonNull(id, "id");

        Match match = findMatch(id);
        matches.remove(id);

        return match.toSummary();
    }

    public synchronized List<MatchSummary> getSummary() {
        return matches.values().stream()
                .sorted(
                        Comparator.comparingLong(Match::totalScore)
                                .reversed()
                                .thenComparing(
                                        Comparator.comparingLong(
                                                Match::startSequence
                                        ).reversed()
                                )
                )
                .map(Match::toSummary)
                .toList();
    }

    private void validateScore(int score) {
        if (score < 0) {
            throw new InvalidScoreException(
                    "Score must not be negative: " + score
            );
        }
    }

    private Match findMatch(MatchId id) {
        Match match = matches.get(id);

        if (match == null) {
            throw new MatchNotFoundException("Match not found: " + id.value());
        }

        return match;
    }
}