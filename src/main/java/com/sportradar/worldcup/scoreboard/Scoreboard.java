package com.sportradar.worldcup.scoreboard;

import java.util.List;

public final class Scoreboard {

    public MatchId startMatch(String team1, String team2) {
        throw new UnsupportedOperationException("not implemented");
    }

    public void updateScore(MatchId id, int team1Score, int team2Score) {
        throw new UnsupportedOperationException("not implemented");
    }

    public void finishMatch(MatchId id) {
        throw new UnsupportedOperationException("not implemented");
    }

    public List<MatchSummary> getSummary() {
        throw new UnsupportedOperationException("not implemented");
    }
}
