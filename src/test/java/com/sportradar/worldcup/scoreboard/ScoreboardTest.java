package com.sportradar.worldcup.scoreboard;

import com.sportradar.worldcup.scoreboard.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoreboardTest {

    private Scoreboard scoreboard;

    @BeforeEach
    void setUp() {
        scoreboard = new Scoreboard();
    }

    @Test
    @DisplayName("Should successfully start a match with 0-0 score and return unique MatchId")
    void shouldStartMatchSuccessfully() {
        MatchId matchId = scoreboard.startMatch("Poland", "Argentina").id();

        assertThat(matchId).isNotNull();
        assertThat(matchId.value()).isNotNull();

        List<MatchSummary> summary = scoreboard.getSummary();
        assertThat(summary).hasSize(1);
        assertThat(summary.get(0))
                .satisfies(m -> {
                    assertThat(m.id()).isEqualTo(matchId);
                    assertThat(m.team1()).isEqualTo("Poland");
                    assertThat(m.team2()).isEqualTo("Argentina");
                    assertThat(m.team1Score()).isEqualTo(0);
                    assertThat(m.team2Score()).isEqualTo(0);
                });
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should throw InvalidTeamException when team name is null, empty or blank")
    void shouldRejectInvalidTeamNames(String invalidTeam) {
        assertThatThrownBy(() -> scoreboard.startMatch(invalidTeam, "Brazil"))
                .isInstanceOf(InvalidTeamException.class);

        assertThatThrownBy(() -> scoreboard.startMatch("Brazil", invalidTeam))
                .isInstanceOf(InvalidTeamException.class);
    }

    @Test
    @DisplayName("Should throw InvalidTeamException when a team plays against itself (case-insensitive)")
    void shouldRejectSameTeamPlayingAgainstItself() {
        assertThatThrownBy(() -> scoreboard.startMatch("Germany", "GERMANY"))
                .isInstanceOf(InvalidTeamException.class);

        assertThatThrownBy(() -> scoreboard.startMatch("  France ", "france"))
                .isInstanceOf(InvalidTeamException.class);
    }

    @Test
    @DisplayName("Should successfully update score with absolute values, including downward corrections")
    void shouldUpdateScoreSuccessfully() {
        MatchId matchId = scoreboard.startMatch("Spain", "Italy").id();

        // First update
        scoreboard.updateScore(matchId, 2, 1);

        List<MatchSummary> summary = scoreboard.getSummary();
        assertThat(summary.get(0).team1Score()).isEqualTo(2);
        assertThat(summary.get(0).team2Score()).isEqualTo(1);

        // VAR correction (downward update)
        scoreboard.updateScore(matchId, 1, 1);

        summary = scoreboard.getSummary();
        assertThat(summary.get(0).team1Score()).isEqualTo(1);
        assertThat(summary.get(0).team2Score()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should throw InvalidScoreException when negative scores are provided")
    void shouldRejectNegativeScores() {
        MatchId matchId = scoreboard.startMatch("Brazil", "Croatia").id();

        assertThatThrownBy(() -> scoreboard.updateScore(matchId, -1, 0))
                .isInstanceOf(InvalidScoreException.class);

        assertThatThrownBy(() -> scoreboard.updateScore(matchId, 0, -2))
                .isInstanceOf(InvalidScoreException.class);
    }

    @Test
    @DisplayName("Should successfully finish a match and remove it from the active summary")
    void shouldFinishMatchSuccessfully() {
        MatchId matchId1 = scoreboard.startMatch("Team A", "Team B").id();
        MatchId matchId2 = scoreboard.startMatch("Team C", "Team D").id();

        scoreboard.finishMatch(matchId1);

        List<MatchSummary> summary = scoreboard.getSummary();
        assertThat(summary).hasSize(1);
        assertThat(summary.get(0).id()).isEqualTo(matchId2);
    }

    @Test
    @DisplayName("Should throw MatchNotFoundException when updating or finishing a non-existent or finished match")
    void shouldThrowExceptionForUnknownOrFinishedMatch() {
        MatchId unknownId = MatchId.random();
        MatchId matchId = scoreboard.startMatch("Japan", "Senegal").id();

        // Unknown match operations
        assertThatThrownBy(() -> scoreboard.updateScore(unknownId, 1, 0))
                .isInstanceOf(MatchNotFoundException.class);

        assertThatThrownBy(() -> scoreboard.finishMatch(unknownId))
                .isInstanceOf(MatchNotFoundException.class);

        // Finish the match once
        scoreboard.finishMatch(matchId);

        // Operations after match is finished
        assertThatThrownBy(() -> scoreboard.updateScore(matchId, 1, 0))
                .isInstanceOf(MatchNotFoundException.class);

        assertThatThrownBy(() -> scoreboard.finishMatch(matchId))
                .isInstanceOf(MatchNotFoundException.class);
    }

    @Test
    @DisplayName("Edge Case 1: Thread race between finishMatch and updateScore should throw MatchNotFoundException")
    void shouldHandleRaceConditionBetweenFinishAndriUpdate() throws InterruptedException, ExecutionException {
        // Given
        MatchId matchId = scoreboard.startMatch("Germany", "France").id();

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);

        // When: Multiple threads try to update or finish the exact same match simultaneously
        Callable<Boolean> finisherTask = () -> {
            latch.await();
            try {
                scoreboard.finishMatch(matchId);
                return true;
            } catch (MatchNotFoundException e) {
                return false;
            }
        };

        Callable<Boolean> updaterTask = () -> {
            latch.await();
            try {
                scoreboard.updateScore(matchId, 1, 1);
                return true;
            } catch (MatchNotFoundException e) {
                return false;
            }
        };

        // Submit tasks
        Future<Boolean> finishFuture = executor.submit(finisherTask);
        Future<Boolean> updateFuture = executor.submit(updaterTask);

        // Release the hounds!
        latch.countDown();

        executor.shutdown();
        boolean finishedInTime = executor.awaitTermination(2, TimeUnit.SECONDS);
        assertThat(finishedInTime).isTrue();

        // Then: The system remains consistent; no corrupted states, summary is empty because match is finished.
        assertThat(scoreboard.getSummary()).isEmpty();
    }

    @Test
    @DisplayName("Edge Case 2: Whitespace trimming and case-insensitivity on team names")
    void shouldTrimWhitespaceAndHandleCaseInsensitivity() {
        // Given teams with spaces and mixed casing
        MatchId matchId = scoreboard.startMatch("  Brazil  ", "ARGENTINA ").id();

        List<MatchSummary> summary = scoreboard.getSummary();
        assertThat(summary).hasSize(1);
        assertThat(summary.get(0).team1()).isEqualTo("Brazil");
        assertThat(summary.get(0).team2()).isEqualTo("ARGENTINA");

        // Verify that self-play check ignores case and whitespace
        assertThatThrownBy(() -> scoreboard.startMatch("  brazil ", "BRAZIL"))
                .isInstanceOf(InvalidTeamException.class);
    }

    @Test
    @DisplayName("Edge Case 4: Downward score correction (VAR scenario) should apply absolute values correctly")
    void shouldHandleDownwardScoreCorrection() {
        // Given an active match
        MatchId matchId = scoreboard.startMatch("Portugal", "Uruguay").id();

        // Initial score update: 2:1
        scoreboard.updateScore(matchId, 2, 1);
        assertThat(scoreboard.getSummary().get(0))
                .satisfies(m -> {
                    assertThat(m.team1Score()).isEqualTo(2);
                    assertThat(m.team2Score()).isEqualTo(1);
                });

        // When VAR cancels a goal (Downward correction to 1:1)
        scoreboard.updateScore(matchId, 1, 1);

        // Then score is accurately updated downwards using absolute values
        assertThat(scoreboard.getSummary().get(0))
                .satisfies(m -> {
                    assertThat(m.team1Score()).isEqualTo(1);
                    assertThat(m.team2Score()).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("Should order summary by total score descending, and then by most recently started match")
    void shouldOrderSummaryCorrectly() {
        MatchId match1 = scoreboard.startMatch("Mexico", "Canada").id(); // 0:0 (started first)
        MatchId match2 = scoreboard.startMatch("Spain", "Brazil").id();   // 0:0
        MatchId match3 = scoreboard.startMatch("Germany", "France").id(); // 0:0 (started last)

        // When: Scores are updated
        scoreboard.updateScore(match1, 0, 2); // Total: 2
        scoreboard.updateScore(match2, 10, 2); // Total: 12 (Highest)
        scoreboard.updateScore(match3, 1, 1); // Total: 2 (Same as match1, but newer)

        // Then: Summary should be ordered by total score desc, then newest first
        List<MatchSummary> summary = scoreboard.getSummary();

        assertThat(summary).hasSize(3);

        // 1st: Spain vs Brazil (Total 12)
        assertThat(summary.get(0).id()).isEqualTo(match2);

        // 2nd: Germany vs France (Total 2, started *later* than Mexico-Canada)
        assertThat(summary.get(1).id()).isEqualTo(match3);

        // 3rd: Mexico vs Canada (Total 2, started *earlier*)
        assertThat(summary.get(2).id()).isEqualTo(match1);
    }

    @Test
    @DisplayName("updateScore on a finished match throws MatchNotFoundException")
    void shouldRejectUpdateOnFinishedMatch() {
        MatchId matchId = scoreboard.startMatch("Japan", "Senegal").id();
        scoreboard.finishMatch(matchId);

        assertThatThrownBy(() -> scoreboard.updateScore(matchId, 1, 0))
                .isInstanceOf(MatchNotFoundException.class)
                .hasMessageContaining(matchId.toString());
    }

    @Test
    @DisplayName("finishMatch on an already finished match throws MatchNotFoundException")
    void shouldRejectSecondFinish() {
        MatchId matchId = scoreboard.startMatch("Japan", "Senegal").id();
        scoreboard.finishMatch(matchId);

        assertThatThrownBy(() -> scoreboard.finishMatch(matchId))
                .isInstanceOf(MatchNotFoundException.class)
                .hasMessageContaining(matchId.toString());
    }
}