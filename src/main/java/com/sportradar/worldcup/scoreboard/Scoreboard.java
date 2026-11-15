package com.sportradar.worldcup.scoreboard;

import java.util.List;

public final class Scoreboard {

    public MatchSummary startMatch(String homeTeam, String awayTeam) {
        throw new UnsupportedOperationException("not implemented");
    }

    public MatchSummary updateScore(MatchId id, int homeScore, int awayScore) {
        throw new UnsupportedOperationException("not implemented");
    }

    public MatchSummary finishMatch(MatchId id) {
        throw new UnsupportedOperationException("not implemented");
    }

    public List<MatchSummary> getSummary() {
        throw new UnsupportedOperationException("not implemented");
    }
}
