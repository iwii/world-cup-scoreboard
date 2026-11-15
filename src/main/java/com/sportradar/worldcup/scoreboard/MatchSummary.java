package com.sportradar.worldcup.scoreboard;

public record MatchSummary(MatchId id, String homeTeam, String awayTeam, int homeScore, int awayScore) {
}
