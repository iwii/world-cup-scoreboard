package com.sportradar.worldcup.scoreboard;

import com.sportradar.worldcup.scoreboard.exception.InvalidScoreException;
import com.sportradar.worldcup.scoreboard.exception.InvalidTeamException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchSummaryTest {

    private final MatchId id = MatchId.random();

    @Test
    @DisplayName("Constructor normalize team names and preserve valid scores")
    void constructorNormalizesTeamNamesAndKeepsValidScores() {
        MatchSummary summary = new MatchSummary(id, "  Poland  ", "Japan\u2003", 2, 1);

        assertThat(summary.team1()).isEqualTo("Poland");
        assertThat(summary.team2()).isEqualTo("Japan");
        assertThat(summary.team1Score()).isEqualTo(2);
        assertThat(summary.team2Score()).isEqualTo(1);
    }

    @Test
    @DisplayName("Constructor rejects a null match ID")
    void constructorRejectsNullId() {
        assertThatThrownBy(() -> new MatchSummary(null, "Poland", "Japan", 0, 0))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Constructor rejects null or blank team names")
    void constructorRejectsNullOrBlankTeamNames() {
        assertThatThrownBy(() -> new MatchSummary(id, null, "Japan", 0, 0))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new MatchSummary(id, "Poland", "\u2003", 0, 0))
                .isInstanceOf(InvalidTeamException.class);
    }

    @Test
    @DisplayName("Constructor rejects a negative score for either team")
    void constructorRejectsNegativeScoreForEitherTeam() {
        assertThatThrownBy(() -> new MatchSummary(id, "Poland", "Japan", -1, 0))
                .isInstanceOf(InvalidScoreException.class)
                .hasMessageContaining("team1Score");
        assertThatThrownBy(() -> new MatchSummary(id, "Poland", "Japan", 0, -1))
                .isInstanceOf(InvalidScoreException.class)
                .hasMessageContaining("team2Score");
    }
}
