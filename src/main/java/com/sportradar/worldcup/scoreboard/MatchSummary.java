package com.sportradar.worldcup.scoreboard;

public record MatchSummary(MatchId id,
                           String team1,
                           String team2,
                           int team1Score,
                           int team2Score) {}
