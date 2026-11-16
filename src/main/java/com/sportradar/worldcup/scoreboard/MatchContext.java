package com.sportradar.worldcup.scoreboard;

import java.util.concurrent.locks.ReentrantLock;

final class MatchContext {
    final MatchId id;
    final String team1;
    final String team2;
    final long sequenceNumber;
    int team1Score;
    int team2Score;
    final ReentrantLock lock = new ReentrantLock();

    MatchContext(MatchId id, String team1, String team2, long sequenceNumber) {
        this.id = id;
        this.team1 = team1;
        this.team2 = team2;
        this.sequenceNumber = sequenceNumber;
    }

    void updateScores(int team1Score, int team2Score) {
        this.team1Score = team1Score;
        this.team2Score = team2Score;
    }

    MatchSummary toSummary() {
        lock.lock();
        try {
            return new MatchSummary(id, team1, team2, team1Score, team2Score);
        } finally {
            lock.unlock();
        }
    }

    record Snapshot(MatchSummary summary, long sequence) {}

    Snapshot toSnapshot() {
        lock.lock();
        try {
            return new Snapshot(
                    new MatchSummary(id, team1, team2, team1Score, team2Score),
                    sequenceNumber);
        } finally {
            lock.unlock();
        }
    }
}

