package com.sportradar.worldcup.scoreboard;

import com.sportradar.worldcup.scoreboard.exception.MatchNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the defensive branches in {@link Scoreboard} that throw
 * "Match not found or already finished with ID" – reachable only when the
 * match entry is removed between {@code matches.get(matchId)} and the
 * subsequent lock acquisition. The tests force that window via reflection
 * instead of racing real threads, so they are deterministic.
 */
class ScoreboardRaceConditionTest {

    private Scoreboard scoreboard;

    @BeforeEach
    void setUp() {
        scoreboard = new Scoreboard();
    }

    @Test
    @DisplayName("updateScore rejects an update after the match has been removed")
    void updateScoreShouldRejectAfterRemoval() throws Exception {
        MatchId matchId = scoreboard.startMatch("Mexico", "Canada").id();

        // Simulate the race: remove the entry while updateScore is about to lock.
        // The implementation already fetched the context, so the identity check
        // (matches.get(matchId) != context) is what fires.
        removeFromMap(matchId);

        assertThatThrownBy(() -> scoreboard.updateScore(matchId, 1, 0))
                .isInstanceOf(MatchNotFoundException.class)
                .hasMessageContaining(matchId.toString());   // bez "already finished"
    }

    @Test
    @DisplayName("finishMatch rejects a finish after the match has been removed")
    void finishMatchShouldRejectAfterRemoval() throws Exception {
        MatchId matchId = scoreboard.startMatch("Mexico", "Canada").id();

        // Simulate the race: remove the entry so that remove(matchId, context) returns false.
        removeFromMap(matchId);

        assertThatThrownBy(() -> scoreboard.finishMatch(matchId))
                .isInstanceOf(MatchNotFoundException.class)
                .hasMessageContaining(matchId.toString());
    }

    /**
     * Removes the entry from the internal map without touching the match lifecycle,
     * emulating what a concurrent finishMatch would do between get() and lock().
     */
    @SuppressWarnings("unchecked")
    private void removeFromMap(MatchId matchId) throws Exception {
        Field field = Scoreboard.class.getDeclaredField("matches");
        field.setAccessible(true);
        Map<MatchId, MatchContext> matches = (Map<MatchId, MatchContext>) field.get(scoreboard);
        matches.remove(matchId);
    }
}