package com.sportradar.worldcup.scoreboard;

import java.util.Objects;

record Match(
        MatchId id,
        Team homeTeam,
        Team awayTeam,
        int homeScore,
        int awayScore,
        long startSequence
) {

    Match {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(homeTeam, "homeTeam");
        Objects.requireNonNull(awayTeam, "awayTeam");
    }

    Match withScore(int homeScore, int awayScore) {
        return new Match(id, homeTeam, awayTeam, homeScore, awayScore, startSequence);
    }

    MatchSummary toSummary() {
        return new MatchSummary(id, homeTeam.name(), awayTeam.name(), homeScore, awayScore);
    }

    long totalScore() {
        return (long) homeScore + awayScore;
    }
}