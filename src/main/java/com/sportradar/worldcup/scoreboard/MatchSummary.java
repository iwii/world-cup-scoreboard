package com.sportradar.worldcup.scoreboard;

import java.util.Objects;

public record MatchSummary(
        MatchId id,
        String homeTeam,
        String awayTeam,
        int homeScore,
        int awayScore
) {

    public MatchSummary {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(homeTeam, "homeTeam");
        Objects.requireNonNull(awayTeam, "awayTeam");
    }
}